<script setup>
import { computed, ref, watch } from "vue";
import axios from "axios";
import { Boxes, ChevronRight, History, PackagePlus, Pencil, Plus, Save, Settings2, Truck, UserRoundCheck, X } from "lucide-vue-next";

const props = defineProps({
  assets: { type: Array, default: () => [] },
  categories: { type: Array, default: () => [] },
  resources: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(["refresh"]);
const query = ref("");
const statusFilter = ref("");
const categoryFilter = ref("");
const dialogOpen = ref(false);
const formMode = ref("new");
const selectedTypeKey = ref("");
const categoryDialogOpen = ref(false);
const assignDialogOpen = ref(false);
const historyDialogOpen = ref(false);
const selectedAsset = ref(null);
const selectedCategory = ref(null);
const form = ref(emptyAsset());
const categoryForm = ref(emptyCategory());
const assignForm = ref({ custodianUserId: "", location: "", reason: "资产领用或位置调拨" });
const historyRows = ref([]);
const saving = ref(false);
const notice = ref("");
const failed = ref(false);
const selectedIds = ref([]);
const moveResourceId = ref("");
const moveLocation = ref("");

const categoryMap = computed(() => Object.fromEntries(props.categories.map((item) => [item.id, item])));
const resourceMap = computed(() => Object.fromEntries(props.resources.map((item) => [item.id, item])));
const selectedFormCategory = computed(() => categoryMap.value[form.value.categoryId]);
const displayAssets = computed(() => {
  const term = query.value.trim().toLowerCase();
  return props.assets.filter((item) => {
    const matchesTerm = !term || [item.assetNo, item.name, item.serialNo, item.brand, item.model, item.location].some((value) => String(value || "").toLowerCase().includes(term));
    return matchesTerm && (!statusFilter.value || item.status === statusFilter.value) && (!categoryFilter.value || item.categoryId === Number(categoryFilter.value));
  });
});
const groupedAssets = computed(() => {
  const groups = new Map();
  for (const item of displayAssets.value) {
    const key = [item.categoryId, item.name, item.brand || "", item.model || ""].join("\u0001");
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        name: item.name,
        categoryId: item.categoryId,
        brand: item.brand,
        model: item.model,
        specification: item.specification,
        items: [],
      });
    }
    groups.get(key).items.push(item);
  }
  return [...groups.values()].map((group) => {
    const statusCounts = group.items.reduce((map, item) => {
      map[item.status] = (map[item.status] || 0) + 1;
      return map;
    }, {});
    const resourceNames = [...new Set(group.items.map((item) => resourceMap.value[item.resourceId]?.name).filter(Boolean))];
    return {
      ...group,
      count: group.items.length,
      resourceNames,
      statusSummary: Object.entries(statusCounts)
        .sort((left, right) => right[1] - left[1])
        .map(([status, count]) => `${count} ${statusText(status)}`),
    };
  });
});
const expandedKeys = ref({});
watch(query, (value) => {
  if (!String(value || "").trim()) return;
  expandedKeys.value = Object.fromEntries(groupedAssets.value.map((group) => [group.key, true]));
});
function toggleGroup(key) {
  expandedKeys.value = { ...expandedKeys.value, [key]: !expandedKeys.value[key] };
}
function isSelected(id) {
  return selectedIds.value.includes(id);
}
function groupSelectedCount(group) {
  return group.items.filter((item) => isSelected(item.id)).length;
}
function groupAllSelected(group) {
  return group.items.length > 0 && group.items.every((item) => isSelected(item.id));
}
function toggleOne(id) {
  selectedIds.value = isSelected(id) ? selectedIds.value.filter((item) => item !== id) : [...selectedIds.value, id];
}
function toggleGroupSelect(group) {
  const ids = group.items.map((item) => item.id);
  if (groupAllSelected(group)) selectedIds.value = selectedIds.value.filter((id) => !ids.includes(id));
  else {
    selectedIds.value = [...new Set([...selectedIds.value, ...ids])];
    expandedKeys.value = { ...expandedKeys.value, [group.key]: true };
  }
}
function clearSelection() {
  selectedIds.value = [];
  moveResourceId.value = "";
  moveLocation.value = "";
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
    clearSelection();
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "批量迁移失败", true);
  } finally {
    saving.value = false;
  }
}
const assetTypes = computed(() => {
  const groups = new Map();
  for (const item of props.assets) {
    const key = [item.categoryId, item.name, item.brand || "", item.model || ""].join("\u0001");
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        name: item.name,
        categoryId: item.categoryId,
        brand: item.brand,
        model: item.model,
        specification: item.specification,
      });
    }
  }
  return [...groups.values()];
});
const selectedAssetType = computed(() => assetTypes.value.find((item) => item.key === selectedTypeKey.value) || null);

function emptyAsset() {
  return { assetNo: "", name: "", categoryId: "", resourceId: "", serialNo: "", brand: "", model: "", specification: "", status: "IN_STOCK", location: "", custodianUserId: "", purchaseDate: "", warrantyUntil: "", originalCost: "", remark: "" };
}
function emptyCategory() {
  return { name: "", serialized: true, highValue: false, enabled: true, description: "" };
}
function show(message, isFailed = false) {
  notice.value = message;
  failed.value = isFailed;
}
function statusText(value) {
  return ({ IN_STOCK: "在库", IN_USE: "使用中", REPORTED: "已上报", MAINTENANCE: "维修中", LOST: "丢失", SCRAPPED: "已报废" })[value] || value;
}
function formatTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}
function openCreateNew() {
  selectedAsset.value = null;
  formMode.value = "new";
  selectedTypeKey.value = "";
  form.value = emptyAsset();
  dialogOpen.value = true;
}
function applySelectedType() {
  const type = selectedAssetType.value;
  if (!type) return;
  form.value.name = type.name;
  form.value.categoryId = type.categoryId;
  form.value.brand = type.brand || "";
  form.value.model = type.model || "";
  form.value.specification = type.specification || "";
}
function openAddExisting(group) {
  selectedAsset.value = null;
  formMode.value = "existing";
  form.value = emptyAsset();
  selectedTypeKey.value = group?.key || "";
  if (group) applySelectedType();
  dialogOpen.value = true;
}
function openEdit(item) {
  selectedAsset.value = item;
  formMode.value = "edit";
  selectedTypeKey.value = "";
  form.value = Object.fromEntries(Object.keys(emptyAsset()).map((key) => [key, item[key] ?? ""]));
  dialogOpen.value = true;
}
function assetPayload() {
  return {
    ...form.value,
    categoryId: Number(form.value.categoryId),
    resourceId: form.value.resourceId === "" ? null : Number(form.value.resourceId),
    custodianUserId: form.value.custodianUserId === "" ? null : Number(form.value.custodianUserId),
    originalCost: form.value.originalCost === "" ? null : Number(form.value.originalCost),
    serialNo: form.value.serialNo.trim() || null,
    purchaseDate: form.value.purchaseDate || null,
    warrantyUntil: form.value.warrantyUntil || null,
  };
}
async function saveAsset() {
  if (formMode.value === "existing" && !selectedTypeKey.value) return show("请选择已有资产类型", true);
  if (!form.value.assetNo.trim() || !form.value.name.trim() || !form.value.categoryId) {
    return show(formMode.value === "existing" ? "请填写这一台设备的资产编号" : "请填写资产编号、名称和分类", true);
  }
  if (formMode.value === "new" && !form.value.serialNo.trim()) return show("新资产类型的第一台设备必须填写资产编号和序列号", true);
  if ((selectedFormCategory.value?.serialized || selectedFormCategory.value?.highValue) && !form.value.serialNo.trim()) return show("序列化或贵重资产必须填写唯一序列号", true);
  saving.value = true;
  try {
    if (selectedAsset.value) await axios.put(`/api/v1/admin/assets/${selectedAsset.value.id}`, assetPayload());
    else await axios.post("/api/v1/admin/assets", assetPayload());
    dialogOpen.value = false;
    show(selectedAsset.value ? "设备资料已更新" : formMode.value === "existing" ? "已添加一台同类设备" : "新资产类型已登记，第一台设备已入账");
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "资产保存失败", true);
  } finally {
    saving.value = false;
  }
}
function openCategories() {
  selectedCategory.value = null;
  categoryForm.value = emptyCategory();
  categoryDialogOpen.value = true;
}
function editCategory(item) {
  selectedCategory.value = item;
  categoryForm.value = { name: item.name, serialized: item.serialized, highValue: item.highValue, enabled: item.enabled, description: item.description || "" };
}
async function saveCategory() {
  if (!categoryForm.value.name.trim()) return show("请填写分类名称", true);
  saving.value = true;
  try {
    if (selectedCategory.value) await axios.put(`/api/v1/admin/assets/categories/${selectedCategory.value.id}`, categoryForm.value);
    else await axios.post("/api/v1/admin/assets/categories", categoryForm.value);
    selectedCategory.value = null;
    categoryForm.value = emptyCategory();
    show("资产分类已保存");
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "分类保存失败", true);
  } finally {
    saving.value = false;
  }
}
function openAssign(item) {
  selectedAsset.value = item;
  assignForm.value = { custodianUserId: item.custodianUserId ?? "", location: item.location || "", reason: "资产领用或位置调拨" };
  assignDialogOpen.value = true;
}
async function saveAssignment() {
  saving.value = true;
  try {
    await axios.post(`/api/v1/admin/assets/${selectedAsset.value.id}/assign`, {
      custodianUserId: assignForm.value.custodianUserId === "" ? null : Number(assignForm.value.custodianUserId),
      location: assignForm.value.location || null,
      reason: assignForm.value.reason || null,
    });
    assignDialogOpen.value = false;
    show("资产领用与位置已更新");
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "资产调拨失败", true);
  } finally {
    saving.value = false;
  }
}
async function openHistory(item) {
  selectedAsset.value = item;
  historyRows.value = [];
  historyDialogOpen.value = true;
  try {
    const response = await axios.get(`/api/v1/admin/assets/${item.id}/history`);
    historyRows.value = response.data?.data || [];
  } catch (e) {
    show(e.response?.data?.message || "资产履历加载失败", true);
  }
}
</script>

<template>
  <div v-if="notice && !dialogOpen" class="asset-notice" :class="{ failed }">{{ notice }}<button title="关闭提示" @click="notice = ''"><X :size="14" /></button></div>
  <div class="asset-toolbar">
    <label class="asset-search"><input v-model="query" placeholder="搜索资产编号、名称或序列号" /></label>
    <select v-model="categoryFilter"><option value="">全部分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select>
    <select v-model="statusFilter"><option value="">全部状态</option><option value="IN_STOCK">在库</option><option value="IN_USE">使用中</option><option value="REPORTED">已上报</option><option value="MAINTENANCE">维修中</option><option value="LOST">丢失</option><option value="SCRAPPED">已报废</option></select>
    <span>{{ groupedAssets.length }} 类 / {{ displayAssets.length }} 台</span>
    <button class="asset-quiet" @click="openCategories"><Settings2 :size="16" />分类设置</button>
    <button class="asset-quiet" :disabled="!assetTypes.length" title="给已有资产类型再登记一台设备" @click="openAddExisting()"><Plus :size="16" />添加已有设备</button>
    <button class="asset-command" @click="openCreateNew"><PackagePlus :size="16" />新资产类型</button>
  </div>
  <div v-if="selectedIds.length" class="asset-migrate-bar">
    <span>已选 <b>{{ selectedIds.length }}</b> 台</span>
    <select v-model="moveResourceId"><option value="">迁移到资源</option><option v-for="resource in resources" :key="resource.id" :value="resource.id">{{ resource.name }}</option></select>
    <input v-model="moveLocation" placeholder="新位置（选填）" />
    <button class="asset-command" type="button" :disabled="saving || !moveResourceId" @click="submitMove"><Truck :size="16" />迁移</button>
    <button class="asset-quiet" type="button" @click="clearSelection">取消选择</button>
  </div>
  <section class="asset-table-wrap">
    <div class="asset-row asset-head asset-group-row"><span></span><span>同类资产</span><span>分类 / 规格</span><span>数量</span><span>关联资源</span><span>状态概览</span></div>
    <div v-for="group in groupedAssets" :key="group.key" class="asset-group" :class="{ open: expandedKeys[group.key] }">
      <div class="asset-row asset-group-row asset-group-select">
        <label class="asset-check" @click.stop><input type="checkbox" :checked="groupAllSelected(group)" @change="toggleGroupSelect(group)" :title="groupSelectedCount(group) ? `已选 ${groupSelectedCount(group)} 台` : '全选此类'" /></label>
        <button class="asset-group-toggle" type="button" @click="toggleGroup(group.key)">
          <span class="asset-group-name"><ChevronRight :size="16" /><span><b>{{ group.name }}</b><small>{{ [group.brand, group.model].filter(Boolean).join(' ') || '未填写品牌型号' }}</small></span></span>
          <span><b>{{ categoryMap[group.categoryId]?.name || `分类 ${group.categoryId}` }}</b><small>{{ group.specification || '规格未登记' }}</small></span>
          <span><b>{{ group.count }}</b><small>台设备</small></span>
          <span>{{ group.resourceNames.length ? group.resourceNames.join('、') : '未关联资源' }}</span>
          <span>{{ group.statusSummary.join(' · ') }}</span>
        </button>
      </div>
      <div v-if="expandedKeys[group.key]" class="asset-group-body">
        <div class="asset-row asset-unit-row asset-unit-head"><span></span><span>资产编号</span><span>序列号</span><span>关联资源</span><span>位置 / 保管人</span><span>原值</span><span>状态</span><span>操作</span></div>
        <div v-for="item in group.items" :key="item.id" class="asset-row asset-unit-row">
          <label class="asset-check"><input type="checkbox" :checked="isSelected(item.id)" @change="toggleOne(item.id)" /></label>
          <span><b class="mono">{{ item.assetNo }}</b></span>
          <span>{{ item.serialNo || '非序列化' }}</span>
          <span>{{ resourceMap[item.resourceId]?.name || '-' }}</span>
          <span><b>{{ item.location || '-' }}</b><small>{{ item.custodianUserId ? `用户 ${item.custodianUserId}` : '未指定保管人' }}</small></span>
          <span>{{ item.originalCost == null ? '-' : `¥${Number(item.originalCost).toLocaleString()}` }}</span>
          <span class="asset-status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span>
          <span class="asset-row-actions"><button title="编辑资料" @click.stop="openEdit(item)"><Pencil :size="14" /></button><button title="领用或调拨" @click.stop="openAssign(item)"><UserRoundCheck :size="14" /></button><button title="查看履历" @click.stop="openHistory(item)"><History :size="14" /></button></span>
        </div>
        <div class="asset-group-add"><button type="button" class="asset-quiet" @click="openAddExisting(group)"><Plus :size="14" />添加一台同类设备</button></div>
      </div>
    </div>
    <div v-if="!groupedAssets.length" class="asset-empty"><Boxes :size="24" /><span>暂无符合条件的资产</span></div>
  </section>

  <div v-if="dialogOpen" class="asset-modal-bg" @click.self="dialogOpen = false"><section class="asset-modal" :class="formMode === 'existing' ? 'existing-form-modal' : 'asset-form-modal'"><div class="asset-modal-title"><div><h2>{{ formMode === 'edit' ? '编辑设备' : formMode === 'existing' ? '添加已有设备' : '登记新资产类型' }}</h2><p>{{ formMode === 'existing' ? '种类信息已固定，只需填写这一台设备的编号、位置和状态' : formMode === 'edit' ? '修改这一台设备的资料' : '先登记资产种类，再填写第一台设备的个体信息' }}</p></div><button class="modal-close" title="关闭" aria-label="关闭资产窗口" @click="dialogOpen = false"><X :size="22" /></button></div>
    <div v-if="notice" class="asset-form-notice" :class="{ failed }" role="alert">{{ notice }}<button type="button" title="关闭提示" @click="notice = ''"><X :size="14" /></button></div>
    <div v-if="formMode === 'existing'" class="asset-form existing-form">
      <label class="asset-wide">选择已有资产类型<select v-model="selectedTypeKey" @change="applySelectedType"><option value="" disabled>请选择要追加的资产类型</option><option v-for="type in assetTypes" :key="type.key" :value="type.key">{{ type.name }}{{ [type.brand, type.model].filter(Boolean).length ? ` · ${[type.brand, type.model].filter(Boolean).join(' ')}` : '' }} · {{ categoryMap[type.categoryId]?.name || '未分类' }}</option></select></label>
      <div v-if="selectedAssetType" class="type-summary asset-wide"><b>{{ selectedAssetType.name }}</b><span>{{ categoryMap[selectedAssetType.categoryId]?.name || '未分类' }}</span><small>{{ [selectedAssetType.brand, selectedAssetType.model, selectedAssetType.specification].filter(Boolean).join(' · ') || '品牌型号未登记' }}</small></div>
      <label>资产编号<input v-model.trim="form.assetNo" maxlength="50" placeholder="这一台的唯一编号" /></label>
      <label>唯一序列号<input v-model.trim="form.serialNo" :placeholder="selectedFormCategory?.serialized || selectedFormCategory?.highValue ? '必填' : '选填'" /></label>
      <label>关联预约资源<select v-model="form.resourceId"><option value="">不关联</option><option v-for="resource in resources" :key="resource.id" :value="resource.id">{{ resource.name }}</option></select></label>
      <label>存放位置<input v-model="form.location" /></label>
      <label>资产状态<select v-model="form.status"><option value="IN_STOCK">在库</option><option value="IN_USE">使用中</option><option value="REPORTED">已上报</option><option value="MAINTENANCE">维修中</option><option value="LOST">丢失</option><option value="SCRAPPED">已报废</option></select></label>
      <label>保管人用户 ID<input v-model="form.custodianUserId" type="number" min="1" /></label>
      <label>采购原值<input v-model="form.originalCost" type="number" min="0" step="0.01" /></label>
      <label>采购日期<input v-model="form.purchaseDate" type="date" /></label>
      <label>保修截止<input v-model="form.warrantyUntil" type="date" /></label>
      <label class="asset-wide">备注<textarea v-model="form.remark" maxlength="1000" placeholder="仅这一台设备的备注"></textarea></label>
      <button class="asset-command asset-wide" :disabled="saving" @click="saveAsset"><Save :size="16" />添加这一台</button>
    </div>
    <div v-else class="asset-form">
      <div class="form-section-title asset-wide">资产种类</div>
      <label>资产名称<input v-model.trim="form.name" maxlength="100" placeholder="例如：数字示波器" /></label>
      <label>资产分类<select v-model.number="form.categoryId"><option value="" disabled>请选择</option><option v-for="category in categories.filter(item => item.enabled)" :key="category.id" :value="category.id">{{ category.name }}{{ category.highValue ? ' · 贵重' : '' }}</option></select></label>
      <label>品牌<input v-model="form.brand" /></label>
      <label>型号<input v-model="form.model" /></label>
      <label class="asset-wide">规格<input v-model="form.specification" /></label>
      <div class="form-section-title asset-wide">{{ formMode === 'edit' ? '这一台设备' : '第一台设备' }}</div>
      <label>资产编号<input v-model.trim="form.assetNo" maxlength="50" :placeholder="formMode === 'new' ? '必填' : ''" /></label>
      <label>唯一序列号<input v-model.trim="form.serialNo" :placeholder="formMode === 'new' || selectedFormCategory?.serialized || selectedFormCategory?.highValue ? '必填' : '选填'" /></label>
      <label>关联预约资源<select v-model="form.resourceId"><option value="">不关联</option><option v-for="resource in resources" :key="resource.id" :value="resource.id">{{ resource.name }}</option></select></label>
      <label>存放位置<input v-model="form.location" /></label>
      <label>资产状态<select v-model="form.status"><option value="IN_STOCK">在库</option><option value="IN_USE">使用中</option><option value="REPORTED">已上报</option><option value="MAINTENANCE">维修中</option><option value="LOST">丢失</option><option value="SCRAPPED">已报废</option></select></label>
      <label>保管人用户 ID<input v-model="form.custodianUserId" type="number" min="1" /></label>
      <label>采购原值<input v-model="form.originalCost" type="number" min="0" step="0.01" /></label>
      <label>采购日期<input v-model="form.purchaseDate" type="date" /></label>
      <label>保修截止<input v-model="form.warrantyUntil" type="date" /></label>
      <label class="asset-wide">备注<textarea v-model="form.remark" maxlength="1000"></textarea></label>
      <button class="asset-command asset-wide" :disabled="saving" @click="saveAsset"><Save :size="16" />{{ formMode === 'edit' ? '保存修改' : '登记种类并入账第一台' }}</button>
    </div>
  </section></div>

  <div v-if="categoryDialogOpen" class="asset-modal-bg" @click.self="categoryDialogOpen = false"><section class="asset-modal category-modal"><div class="asset-modal-title"><div><h2>资产分类</h2><p>序列化和贵重分类下的资产强制一物一码</p></div><button class="modal-close" title="关闭" aria-label="关闭资产分类窗口" @click="categoryDialogOpen = false"><X :size="22" /></button></div><div class="category-layout"><div class="category-list"><button v-for="item in categories" :key="item.id" :class="{ active: selectedCategory?.id === item.id }" @click="editCategory(item)"><span><b>{{ item.name }}</b><small>{{ item.enabled ? '启用' : '停用' }} · {{ item.highValue ? '贵重' : item.serialized ? '序列化' : '普通' }}</small></span><Pencil :size="14" /></button></div><div class="category-form"><label>分类名称<input v-model="categoryForm.name" /></label><label class="category-check"><input v-model="categoryForm.serialized" type="checkbox" />每件资产独立编号</label><label class="category-check"><input v-model="categoryForm.highValue" type="checkbox" />贵重资产</label><label class="category-check"><input v-model="categoryForm.enabled" type="checkbox" />启用分类</label><label>说明<textarea v-model="categoryForm.description"></textarea></label><button class="asset-command" @click="saveCategory"><Save :size="15" />{{ selectedCategory ? '保存修改' : '新增分类' }}</button><button v-if="selectedCategory" class="asset-quiet" @click="selectedCategory = null; categoryForm = emptyCategory()">取消编辑</button></div></div></section></div>

  <div v-if="assignDialogOpen" class="asset-modal-bg" @click.self="assignDialogOpen = false"><section class="asset-modal small-modal"><div class="asset-modal-title"><div><h2>领用与调拨</h2><p>{{ selectedAsset?.assetNo }} · {{ selectedAsset?.name }}</p></div><button class="modal-close" title="关闭" aria-label="关闭调拨窗口" @click="assignDialogOpen = false"><X :size="22" /></button></div><div class="assign-form"><label>保管人用户 ID<input v-model="assignForm.custodianUserId" type="number" min="1" placeholder="留空表示取消保管人" /></label><label>存放位置<input v-model="assignForm.location" /></label><label>变更原因<textarea v-model="assignForm.reason"></textarea></label><button class="asset-command" :disabled="saving" @click="saveAssignment"><UserRoundCheck :size="16" />确认变更</button></div></section></div>

  <div v-if="historyDialogOpen" class="asset-modal-bg" @click.self="historyDialogOpen = false"><section class="asset-modal"><div class="asset-modal-title"><div><h2>资产状态履历</h2><p>{{ selectedAsset?.assetNo }} · {{ selectedAsset?.name }}</p></div><button class="modal-close" title="关闭" aria-label="关闭资产履历窗口" @click="historyDialogOpen = false"><X :size="22" /></button></div><div class="history-list"><div v-for="row in historyRows" :key="row.id"><span class="history-marker"></span><span><b>{{ row.fromStatus ? statusText(row.fromStatus) : '建立资产' }} → {{ statusText(row.toStatus) }}</b><small>{{ row.reason || '状态变更' }} · 操作用户 {{ row.operatorId || '-' }}</small><em>{{ formatTime(row.createdAt) }}</em></span></div><div v-if="!historyRows.length" class="asset-empty">暂无状态履历</div></div></section></div>
</template>

<style scoped>
.asset-notice{min-height:38px;padding:0 12px;margin-bottom:12px;background:#e8f5ed;color:#347458;display:flex;align-items:center;border-left:3px solid #4d9a70;font-size:12px}.asset-notice.failed{background:#faece9;color:#a24d42;border-color:#bd655a}.asset-notice button{margin-left:auto;border:0;background:transparent;color:inherit}.asset-form-notice{min-height:38px;margin:16px 20px 0;padding:0 12px;background:#eaf5ee;color:#347458;display:flex;align-items:center;gap:7px;font-size:12px;border:1px solid #dcebe2;border-radius:5px}.asset-form-notice.failed{background:#faece9;color:#a24d42;border-color:#ead1cc}.asset-form-notice button{margin-left:auto;border:0;background:transparent;color:inherit}.form-section-title{margin:2px 0 0;padding-top:10px;border-top:1px solid #edf1ef;color:#356c58;font-size:12px;font-weight:700}.asset-form .form-section-title:first-child{border-top:0;padding-top:0}.type-summary{display:grid;gap:4px;padding:12px 14px;background:#f2f7f4;border-radius:5px}.type-summary b{font-size:14px}.type-summary span{font-size:11px;color:#3c6958}.type-summary small{color:#799087;font-size:11px}.existing-form-modal{width:min(640px,100%)}.asset-group-add{padding:8px 18px 12px;background:#f7faf8}.asset-quiet:disabled{opacity:.5;cursor:not-allowed}
.asset-migrate-bar{display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin:-4px 0 12px;padding:10px 12px;background:#eef6f1;border:1px solid #d5e6dc;border-radius:6px;font-size:12px;color:#2f493e}.asset-migrate-bar b{font-size:14px}.asset-migrate-bar select,.asset-migrate-bar input{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff}
.asset-toolbar{display:grid;grid-template-columns:minmax(160px,1.4fr) 140px 120px auto auto auto auto;gap:8px;align-items:center;margin-bottom:12px;color:#74847d;font-size:11px}.asset-toolbar input,.asset-toolbar select{width:100%;height:38px;border:1px solid #dce5e0;border-radius:5px;background:#fff;padding:0 10px;color:#263b32}.asset-command,.asset-quiet{height:38px;border-radius:5px;padding:0 12px;display:inline-flex;align-items:center;justify-content:center;gap:6px;white-space:nowrap}.asset-command{border:0;background:#225c4d;color:#fff}.asset-quiet{border:1px solid #d6e0db;background:#fff;color:#42695a}.asset-table-wrap{background:#fff;border:1px solid #dfe7e3;border-radius:6px;overflow:auto}.asset-row{min-width:1080px;display:grid;gap:14px;align-items:center;min-height:59px;padding:0 18px;border-bottom:1px solid #edf1ef;font-size:11px}.asset-group-row{grid-template-columns:28px 1.6fr 1.2fr .55fr 1.4fr 1.3fr}.asset-group-select{grid-template-columns:28px 1fr;padding:0 18px 0 12px;background:#fff}.asset-group-toggle{display:grid;grid-template-columns:1.6fr 1.2fr .55fr 1.4fr 1.3fr;gap:14px;align-items:center;width:100%;min-height:59px;border:0;background:transparent;text-align:left;color:inherit;cursor:pointer;padding:0}.asset-group-toggle:hover{background:#f3f8f5}.asset-check{display:grid;place-items:center}.asset-check input{width:15px;height:15px}.asset-unit-row{grid-template-columns:28px 1.1fr 1fr 1.1fr 1.2fr .7fr .7fr .85fr;background:#f7faf8}.asset-unit-head{min-height:34px;background:#eef4f0;color:#7d8c85;font-size:10px}.asset-head{min-height:38px;background:#fafbfa;color:#839089;font-size:10px}.asset-group-row.asset-head{position:sticky;top:0;z-index:1}.asset-group-name{display:flex;align-items:center;gap:8px;min-width:0}.asset-group-name svg{flex:0 0 auto;color:#5f7d70;transition:transform .15s ease}.asset-group.open .asset-group-name svg{transform:rotate(90deg)}.asset-group-body{border-bottom:1px solid #edf1ef}.asset-group-body .asset-row:last-child{border-bottom:0}.asset-row b,.asset-row small{display:block}.asset-row b{font-size:12px}.asset-row small{color:#7e8e86;margin-top:4px}.asset-status{font-size:10px;padding:5px 7px;border-radius:3px;background:#edf1ef;color:#607168;width:max-content}.asset-status.in_stock{background:#e4f3e9;color:#347658}.asset-status.in_use{background:#e7eef5;color:#426d8b}.asset-status.reported{background:#fff0d8;color:#8f6a2f}.asset-status.maintenance{background:#eee9f7;color:#68568d}.asset-status.lost,.asset-status.scrapped{background:#f3e9e7;color:#9a5a51}.asset-row-actions{display:flex;gap:5px}.asset-row-actions button{width:30px;height:30px;border:1px solid #d6e1db;background:#fff;color:#416b5b;border-radius:4px;display:grid;place-items:center}.asset-empty{padding:36px;display:flex;align-items:center;justify-content:center;gap:8px;color:#8d9a94;font-size:12px}.asset-modal-bg{position:fixed;z-index:60;inset:0;background:rgba(18,31,26,.48);display:grid;place-items:center;padding:20px}.asset-modal{width:min(650px,100%);max-height:92vh;overflow:auto;background:#fff;border-radius:7px}.asset-form-modal{width:min(760px,100%)}.small-modal{width:min(480px,100%)}.asset-modal-title{padding:19px 21px;border-bottom:1px solid #e6ece9;display:flex;justify-content:space-between}.asset-modal-title h2{font-size:17px;margin:0 0 4px}.asset-modal-title p{font-size:11px;color:#82918a;margin:0}.asset-modal-title button{border:0;background:transparent}.asset-form{padding:20px;display:grid;grid-template-columns:1fr 1fr;gap:12px}.asset-form label,.assign-form label,.category-form label{display:flex;flex-direction:column;gap:6px;font-size:11px;color:#607169}.asset-form input,.asset-form select,.asset-form textarea,.assign-form input,.assign-form textarea,.category-form input,.category-form textarea{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;color:#243b31;background:#fff}.asset-form textarea,.assign-form textarea,.category-form textarea{height:70px;padding:8px;resize:vertical}.asset-wide{grid-column:1/-1}.category-layout{display:grid;grid-template-columns:1fr 1fr;min-height:390px}.category-list{padding:10px;border-right:1px solid #e8eeeb}.category-list button{width:100%;min-height:54px;padding:8px 10px;border:0;border-bottom:1px solid #edf1ef;background:#fff;display:flex;align-items:center;justify-content:space-between;text-align:left;color:#2f493e}.category-list button.active{background:#edf6f1}.category-list b,.category-list small{display:block}.category-list small{margin-top:4px;color:#839089;font-size:10px}.category-form{padding:18px;display:flex;flex-direction:column;gap:11px}.category-form .category-check{flex-direction:row;align-items:center}.category-check input{height:auto}.assign-form{padding:20px;display:grid;gap:12px}.history-list{padding:18px 22px}.history-list>div{display:grid;grid-template-columns:16px 1fr;gap:8px;min-height:72px}.history-marker{width:9px;height:9px;margin-top:4px;border-radius:50%;background:#4d9273;box-shadow:0 0 0 4px #e8f2ed}.history-list b,.history-list small,.history-list em{display:block}.history-list b{font-size:12px}.history-list small{margin-top:5px;color:#788a82;font-size:11px}.history-list em{margin-top:4px;color:#a0aaa5;font-size:10px;font-style:normal}.mono{font-family:ui-monospace,monospace}@media(max-width:900px){.asset-toolbar{grid-template-columns:1fr 1fr 1fr}.asset-toolbar span{display:none}}@media(max-width:650px){.asset-toolbar{grid-template-columns:1fr 1fr}.asset-search{grid-column:1/-1}.asset-toolbar .asset-command,.asset-toolbar .asset-quiet{font-size:0;padding:0}.asset-toolbar .asset-command svg,.asset-toolbar .asset-quiet svg{margin:0}.asset-form,.category-layout{grid-template-columns:1fr}.asset-wide{grid-column:auto}.category-list{border-right:0;border-bottom:1px solid #e8eeeb;max-height:180px;overflow:auto}}
</style>
