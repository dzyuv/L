<script setup>
import { computed, ref } from "vue";
import axios from "axios";
import { PackagePlus, Save, Truck, Upload, X } from "lucide-vue-next";

const props = defineProps({
  assets: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] },
  resources: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(["refresh"]);
const notice = ref("");
const failed = ref(false);
const saving = ref(false);
const selectedIds = ref([]);
const moveResourceId = ref("");
const moveLocation = ref("");
const importText = ref("");
const purchase = ref(emptyPurchase());

const categoryMap = computed(() => Object.fromEntries(props.categories.map((item) => [item.id, item])));
const resourceMap = computed(() => Object.fromEntries(props.resources.map((item) => [item.id, item])));
const assetTypes = computed(() => {
  const groups = new Map();
  for (const item of props.assets) {
    const key = [item.categoryId, item.name, item.brand || "", item.model || ""].join("\u0001");
    if (!groups.has(key)) groups.set(key, { key, name: item.name, categoryId: item.categoryId, brand: item.brand, model: item.model, specification: item.specification });
  }
  return [...groups.values()];
});
const selectedType = computed(() => assetTypes.value.find((item) => item.key === purchase.value.typeKey) || null);
const selectedAssets = computed(() => props.assets.filter((item) => selectedIds.value.includes(item.id)));
const allSelected = computed(() => props.assets.length > 0 && selectedIds.value.length === props.assets.length);

function emptyPurchase() {
  return {
    mode: "existing",
    typeKey: "",
    categoryId: "",
    name: "",
    brand: "",
    model: "",
    specification: "",
    resourceId: "",
    autoNumber: true,
    numberPrefix: "",
    quantity: 1,
    serials: "",
    location: "",
  };
}
function show(message, isFailed = false) {
  notice.value = message;
  failed.value = isFailed;
}
function applyType() {
  const type = selectedType.value;
  if (!type) return;
  purchase.value.name = type.name;
  purchase.value.categoryId = type.categoryId;
  purchase.value.brand = type.brand || "";
  purchase.value.model = type.model || "";
  purchase.value.specification = type.specification || "";
  if (!purchase.value.numberPrefix) purchase.value.numberPrefix = suggestPrefix(type.name);
}
function suggestPrefix(name) {
  const letters = String(name || "").replace(/[^A-Za-z0-9]/g, "").toUpperCase();
  return letters.slice(0, 3) || "AST";
}
function toggleAll() {
  selectedIds.value = allSelected.value ? [] : props.assets.map((item) => item.id);
}
function toggleOne(id) {
  selectedIds.value = selectedIds.value.includes(id) ? selectedIds.value.filter((item) => item !== id) : [...selectedIds.value, id];
}
function parseSerials() {
  return purchase.value.serials.split(/\r?\n/).map((item) => item.trim()).filter(Boolean);
}
async function previewNumber() {
  const prefix = purchase.value.numberPrefix.trim() || suggestPrefix(purchase.value.name);
  try {
    const response = await axios.get("/api/v1/admin/assets/next-no", { params: { prefix } });
    show(`下一个自动编号：${response.data?.data?.assetNo || "-"}`);
  } catch (e) {
    show(e.response?.data?.message || "编号预览失败", true);
  }
}
async function submitPurchase() {
  if (purchase.value.mode === "existing") applyType();
  if (!purchase.value.name.trim() || !purchase.value.categoryId) return show("请选择或填写资产种类", true);
  const quantity = Number(purchase.value.quantity) || 0;
  if (quantity < 1) return show("入库数量至少为 1", true);
  const serials = parseSerials();
  if (serials.length && serials.length !== quantity) return show("序列号行数必须与入库数量一致", true);
  const items = Array.from({ length: quantity }, (_, index) => ({
    serialNo: serials[index] || "",
    location: purchase.value.location.trim() || null,
    resourceId: purchase.value.resourceId === "" ? null : Number(purchase.value.resourceId),
  }));
  saving.value = true;
  try {
    const response = await axios.post("/api/v1/admin/assets/batch", {
      categoryId: Number(purchase.value.categoryId),
      name: purchase.value.name.trim(),
      brand: purchase.value.brand.trim() || null,
      model: purchase.value.model.trim() || null,
      specification: purchase.value.specification.trim() || null,
      resourceId: purchase.value.resourceId === "" ? null : Number(purchase.value.resourceId),
      status: "IN_STOCK",
      autoNumber: purchase.value.autoNumber,
      numberPrefix: purchase.value.numberPrefix.trim() || suggestPrefix(purchase.value.name),
      items,
    });
    const total = response.data?.data?.total || quantity;
    show(`采购入库 ${total} 台，编号已${purchase.value.autoNumber ? "自动生成" : "按填写入账"}`);
    purchase.value.serials = "";
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "采购入库失败", true);
  } finally {
    saving.value = false;
  }
}
function parseImportRows() {
  return importText.value.split(/\r?\n/).map((line) => line.trim()).filter((line) => line && !line.startsWith("#")).map((line) => {
    const [assetNo, name, categoryId, serialNo, brand, model, resourceId, location, originalCost] = line.split(",").map((item) => item.trim());
    return { assetNo, name, categoryId, serialNo, brand, model, resourceId, location, originalCost };
  });
}
async function submitImport() {
  const rows = parseImportRows();
  if (!rows.length) return show("请粘贴至少一行导入数据", true);
  saving.value = true;
  let success = 0;
  try {
    const grouped = new Map();
    for (const row of rows) {
      if (!row.name || !row.categoryId) throw new Error("每一行都需要名称和分类ID");
      const key = [row.categoryId, row.name, row.brand || "", row.model || ""].join("\u0001");
      if (!grouped.has(key)) grouped.set(key, { categoryId: Number(row.categoryId), name: row.name, brand: row.brand, model: row.model, items: [] });
      grouped.get(key).items.push({
        assetNo: row.assetNo || "",
        serialNo: row.serialNo || "",
        location: row.location || "",
        resourceId: row.resourceId ? Number(row.resourceId) : null,
        originalCost: row.originalCost === "" || row.originalCost == null ? null : Number(row.originalCost),
      });
    }
    for (const group of grouped.values()) {
      const response = await axios.post("/api/v1/admin/assets/batch", {
        ...group,
        specification: null,
        resourceId: group.items[0]?.resourceId || null,
        status: "IN_STOCK",
        autoNumber: group.items.every((item) => !item.assetNo),
        numberPrefix: suggestPrefix(group.name),
        items: group.items,
      });
      success += response.data?.data?.total || group.items.length;
    }
    importText.value = "";
    show(`已导入 ${success} 台设备`);
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || e.message || "批量导入失败", true);
  } finally {
    saving.value = false;
  }
}
async function submitMove() {
  if (!selectedIds.value.length) return show("请先勾选要迁移的设备", true);
  if (moveResourceId.value === "") return show("请选择目标资源", true);
  saving.value = true;
  try {
    const response = await axios.post("/api/v1/admin/assets/move", {
      assetIds: selectedIds.value,
      resourceId: Number(moveResourceId.value),
      location: moveLocation.value.trim() || null,
    });
    show(`已将 ${response.data?.data?.total || selectedIds.value.length} 台设备迁移到目标资源`);
    selectedIds.value = [];
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "批量迁移失败", true);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div v-if="notice" class="manage-notice" :class="{ failed }">{{ notice }}<button type="button" title="关闭提示" @click="notice = ''"><X :size="14" /></button></div>
  <div class="manage-grid">
    <section class="manage-card">
      <div class="manage-head"><div><h2>采购入库</h2><p>支持按已有种类追加，或登记新种类后批量生成编号</p></div></div>
      <div class="manage-form">
        <label class="wide">入库方式<select v-model="purchase.mode"><option value="existing">追加已有资产种类</option><option value="new">新资产种类采购</option></select></label>
        <label v-if="purchase.mode === 'existing'" class="wide">已有种类<select v-model="purchase.typeKey" @change="applyType"><option value="">请选择</option><option v-for="type in assetTypes" :key="type.key" :value="type.key">{{ type.name }}{{ [type.brand, type.model].filter(Boolean).length ? ` · ${[type.brand, type.model].filter(Boolean).join(' ')}` : '' }}</option></select></label>
        <template v-if="purchase.mode === 'new'">
          <label>资产名称<input v-model.trim="purchase.name" /></label>
          <label>资产分类<select v-model="purchase.categoryId"><option value="" disabled>请选择</option><option v-for="category in categories.filter(item => item.enabled)" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
          <label>品牌<input v-model="purchase.brand" /></label>
          <label>型号<input v-model="purchase.model" /></label>
        </template>
        <label>入库数量<input v-model.number="purchase.quantity" type="number" min="1" max="200" /></label>
        <label>目标资源<select v-model="purchase.resourceId"><option value="">暂不关联</option><option v-for="resource in resources" :key="resource.id" :value="resource.id">{{ resource.name }}</option></select></label>
        <label>存放位置<input v-model="purchase.location" /></label>
        <label>编号前缀<input v-model.trim="purchase.numberPrefix" placeholder="例如 LAB-PC" /></label>
        <label class="check-row"><input v-model="purchase.autoNumber" type="checkbox" />自动生成资产编号</label>
        <button class="ghost" type="button" @click="previewNumber">预览下一个编号</button>
        <label class="wide">序列号（选填，一行一个，数量须一致）<textarea v-model="purchase.serials" placeholder="SN-001&#10;SN-002"></textarea></label>
        <button class="command wide" :disabled="saving" @click="submitPurchase"><PackagePlus :size="16" />确认入库</button>
      </div>
    </section>
    <section class="manage-card">
      <div class="manage-head"><div><h2>批量导入</h2><p>CSV：资产编号,名称,分类ID,序列号,品牌,型号,资源ID,位置,原值</p></div></div>
      <div class="manage-form">
        <label class="wide">导入内容<textarea v-model="importText" class="import-box" placeholder="# 空编号时自动生成&#10;LAB-PC-010,教学台式计算机,1,SN-010,Lenovo,M90t,2,B305-03,8960"></textarea></label>
        <button class="command wide" :disabled="saving" @click="submitImport"><Upload :size="16" />导入设备</button>
      </div>
    </section>
  </div>
  <section class="manage-card migrate-card">
    <div class="manage-head">
      <div><h2>批量迁移</h2><p>勾选设备后迁移到其他预约资源，可同时更新存放位置</p></div>
      <div class="migrate-actions">
        <select v-model="moveResourceId"><option value="">选择目标资源</option><option v-for="resource in resources" :key="resource.id" :value="resource.id">{{ resource.name }}</option></select>
        <input v-model="moveLocation" placeholder="新位置（选填）" />
        <button class="command" :disabled="saving || !selectedIds.length" @click="submitMove"><Truck :size="16" />迁移 {{ selectedIds.length }} 台</button>
      </div>
    </div>
    <div class="migrate-table">
      <div class="migrate-row head"><label><input type="checkbox" :checked="allSelected" @change="toggleAll" />全选</label><span>设备</span><span>编号</span><span>当前资源</span><span>位置</span></div>
      <div v-for="item in assets" :key="item.id" class="migrate-row">
        <label><input type="checkbox" :checked="selectedIds.includes(item.id)" @change="toggleOne(item.id)" /></label>
        <span><b>{{ item.name }}</b><small>{{ [item.brand, item.model].filter(Boolean).join(' ') || '未填型号' }}</small></span>
        <span class="mono">{{ item.assetNo }}</span>
        <span>{{ resourceMap[item.resourceId]?.name || '未关联' }}</span>
        <span>{{ item.location || '-' }}</span>
      </div>
      <div v-if="!assets.length" class="empty">暂无资产</div>
    </div>
  </section>
</template>

<style scoped>
.manage-notice{min-height:38px;padding:0 12px;margin-bottom:12px;background:#e8f5ed;color:#347458;display:flex;align-items:center;border-left:3px solid #4d9a70;font-size:12px}.manage-notice.failed{background:#faece9;color:#a24d42;border-color:#bd655a}.manage-notice button{margin-left:auto;border:0;background:transparent;color:inherit}
.manage-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:14px}
.manage-card{background:#fff;border:1px solid #dfe7e3;border-radius:6px;overflow:hidden}
.manage-head{padding:16px 18px;border-bottom:1px solid #e8eeeb;display:flex;justify-content:space-between;gap:12px;align-items:flex-start}
.manage-head h2{margin:0 0 4px;font-size:15px}.manage-head p{margin:0;color:#839089;font-size:11px}
.manage-form{padding:16px 18px 18px;display:grid;grid-template-columns:1fr 1fr;gap:11px}
.manage-form label{display:flex;flex-direction:column;gap:6px;font-size:11px;color:#607169}
.manage-form input,.manage-form select,.manage-form textarea{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}
.manage-form textarea{height:84px;padding:8px;resize:vertical}
.import-box{min-height:180px;font-family:ui-monospace,monospace;font-size:11px}
.wide{grid-column:1/-1}.check-row{flex-direction:row;align-items:center}
.command,.ghost{height:36px;border-radius:5px;padding:0 12px;display:inline-flex;align-items:center;justify-content:center;gap:6px}
.command{border:0;background:#225c4d;color:#fff}.ghost{border:1px solid #d6e0db;background:#fff;color:#42695a}
.command:disabled{opacity:.5}
.migrate-actions{display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.migrate-actions select,.migrate-actions input{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px}
.migrate-table{overflow:auto}
.migrate-row{min-width:760px;display:grid;grid-template-columns:70px 1.4fr 1fr 1.1fr 1fr;gap:12px;align-items:center;min-height:48px;padding:0 18px;border-bottom:1px solid #edf1ef;font-size:12px}
.migrate-row.head{min-height:36px;background:#fafbfa;color:#839089;font-size:10px}
.migrate-row b,.migrate-row small{display:block}.migrate-row small{margin-top:3px;color:#82918a;font-size:10px}
.mono{font-family:ui-monospace,monospace}.empty{padding:28px;text-align:center;color:#8d9a94;font-size:12px}
@media(max-width:950px){.manage-grid{grid-template-columns:1fr}.manage-form{grid-template-columns:1fr}.wide{grid-column:auto}.migrate-actions{width:100%}}
</style>
