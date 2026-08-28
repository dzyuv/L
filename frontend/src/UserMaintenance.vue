<script setup>
import { computed, onMounted, ref } from "vue";
import axios from "axios";
import { AlertTriangle, Plus, RefreshCw, Wrench, X } from "lucide-vue-next";

const props = defineProps({
  internal: { type: Boolean, default: false },
  resources: { type: Array, default: () => [] },
  bookings: { type: Array, default: () => [] },
});
const assets = ref([]);
const tickets = ref([]);
const loading = ref(false);
const dialogOpen = ref(false);
const notice = ref("");
const failed = ref(false);
const form = ref(emptyForm());
const assetQuery = ref("");

const selectedAsset = computed(() => assets.value.find((item) => item.id === Number(form.value.assetId)));
const assetMap = computed(() => Object.fromEntries(assets.value.map((item) => [item.id, item])));
const resourceMap = computed(() => Object.fromEntries(props.resources.map((item) => [item.id, item])));
const reportableResources = computed(() => {
  const recentIds = new Set(props.bookings.map((item) => Number(item.resourceId)));
  return [...props.resources].sort((a, b) => Number(recentIds.has(Number(b.id))) - Number(recentIds.has(Number(a.id))));
});
const filteredAssets = computed(() => {
  const term = assetQuery.value.trim().toLowerCase();
  const resourceId = Number(form.value.resourceId) || null;
  return assets.value.filter((item) => (!resourceId || Number(item.resourceId) === resourceId) && (!term || [item.assetNo, item.name, item.serialNo, item.location, item.brand, item.model].some(value => String(value || "").toLowerCase().includes(term))));
});

function emptyForm() {
  return { assetId: "", resourceId: "", location: "", assetClue: "", reportType: "MALFUNCTION", severity: "MEDIUM", description: "" };
}
function statusText(value) {
  return ({ REPORTED: "待受理", TRIAGED: "已受理", REPAIRING: "维修中", WAITING_ACCEPTANCE: "待验收", CLOSED: "已关闭", REJECTED: "已驳回" })[value] || value;
}
function reportTypeText(value) {
  return ({ DAMAGE: "损坏", MALFUNCTION: "故障", LOSS: "丢失", OTHER: "其他" })[value] || value;
}
function severityText(value) {
  return ({ LOW: "低", MEDIUM: "中", HIGH: "高", CRITICAL: "紧急" })[value] || value;
}
function formatTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}
async function loadMaintenance() {
  loading.value = true;
  try {
    const [assetResponse, ticketResponse] = await Promise.all([
      props.internal ? axios.get("/api/v1/assets/catalog") : Promise.resolve({ data: { data: [] } }),
      axios.get("/api/v1/maintenance/tickets/mine"),
    ]);
    assets.value = assetResponse.data?.data || [];
    tickets.value = ticketResponse.data?.data || [];
  } catch (e) {
    failed.value = true;
    notice.value = e.response?.data?.message || "报修数据加载失败";
  } finally {
    loading.value = false;
  }
}
function openDialog() {
  form.value = emptyForm();
  assetQuery.value = "";
  notice.value = "";
  dialogOpen.value = true;
}
async function submitReport() {
  if (props.internal && !form.value.assetId) return show("请选择需要上报的具体设备", true);
  if (!props.internal && !form.value.resourceId && !form.value.location.trim()) return show("请选择问题发生的实验室或填写位置", true);
  if (!form.value.description.trim()) return show("请填写具体的问题描述", true);
  loading.value = true;
  try {
    await axios.post("/api/v1/maintenance/tickets", {
      ...form.value,
      assetId: form.value.assetId ? Number(form.value.assetId) : null,
      resourceId: form.value.resourceId ? Number(form.value.resourceId) : null,
      location: form.value.location.trim() || null,
      assetClue: form.value.assetClue.trim() || null,
      description: form.value.description.trim(),
    });
    dialogOpen.value = false;
    show("报修已提交，管理员受理后可在此查看进度");
    await loadMaintenance();
  } catch (e) {
    show(e.response?.data?.message || "报修提交失败", true);
  } finally {
    loading.value = false;
  }
}
function show(message, isFailed = false) {
  notice.value = message;
  failed.value = isFailed;
}

onMounted(loadMaintenance);
</script>

<template>
  <section class="maintenance-panel panel">
    <div class="panel-head maintenance-head">
      <div><h2>{{ internal ? '内部设备报修' : '设施问题反馈' }}</h2><p>{{ internal ? '检索内部资产并上报故障、损坏或丢失' : '只需说明问题发生的位置和现象，无需掌握设备资产编号' }}</p></div>
      <div class="maintenance-actions">
        <button class="maintenance-icon" :disabled="loading" title="刷新报修记录" @click="loadMaintenance"><RefreshCw :size="16" /></button>
        <button class="maintenance-command" :disabled="loading || (internal && !assets.length)" :title="internal && !assets.length ? '管理员尚未登记可报修资产' : '发起问题上报'" @click="openDialog"><Plus :size="16" />发起上报</button>
      </div>
    </div>
    <div v-if="notice" class="maintenance-notice" :class="{ failed }"><AlertTriangle v-if="failed" :size="15" /><Wrench v-else :size="15" />{{ notice }}<button title="关闭提示" @click="notice = ''"><X :size="14" /></button></div>
    <div class="maintenance-table">
      <div class="maintenance-row maintenance-table-head"><span>工单编号</span><span>问题位置 / 设备</span><span>问题</span><span>上报时间</span><span>状态</span></div>
      <div v-for="ticket in tickets" :key="ticket.id" class="maintenance-row">
        <span class="mono">{{ ticket.ticketNo }}</span>
        <span><b>{{ assetMap[ticket.assetId]?.name || resourceMap[ticket.resourceId]?.name || ticket.locationSnapshot || '待管理员定位' }}</b><small>{{ assetMap[ticket.assetId]?.assetNo || ticket.assetClue || ticket.locationSnapshot || '未指定具体设备' }}</small></span>
        <span><b>{{ reportTypeText(ticket.reportType) }} · {{ severityText(ticket.severity) }}</b><small :title="ticket.description">{{ ticket.description }}</small></span>
        <span>{{ formatTime(ticket.reportedAt) }}</span>
        <span class="ticket-status" :class="ticket.status.toLowerCase()">{{ statusText(ticket.status) }}</span>
      </div>
      <div v-if="!tickets.length" class="maintenance-empty">暂无报修记录</div>
    </div>
  </section>

  <div v-if="dialogOpen" class="maintenance-modal-bg" @click.self="dialogOpen = false">
    <section class="maintenance-modal" role="dialog" aria-modal="true">
      <div class="maintenance-modal-title"><div><h2>{{ internal ? '内部设备报修' : '设施问题反馈' }}</h2><p>{{ internal ? '按位置检索并确认具体资产，贵重设备需核对唯一编号' : '管理员会根据位置和设备线索确认具体资产' }}</p></div><button title="关闭" @click="dialogOpen = false"><X :size="18" /></button></div>
      <div class="maintenance-form">
        <label class="wide">问题发生区域<select v-model="form.resourceId"><option value="">无法确认或不在列表中</option><option v-for="resource in reportableResources" :key="resource.id" :value="resource.id">{{ resource.name }} · {{ resource.location }}</option></select></label>
        <template v-if="internal"><label class="wide">检索设备<input v-model="assetQuery" placeholder="输入资产编号、设备名称、序列号或位置" /></label><label class="wide">具体资产<select v-model="form.assetId"><option value="" disabled>请选择检索到的设备</option><option v-for="asset in filteredAssets" :key="asset.id" :value="asset.id">{{ asset.assetNo }} · {{ asset.name }} · {{ asset.location || '位置未登记' }}{{ asset.serialNo ? ` · SN ${asset.serialNo}` : '' }}</option></select></label></template>
        <template v-else><label class="wide">具体位置<input v-model="form.location" maxlength="200" placeholder="例如：A201 实验台右侧、靠窗第三排" /></label><label class="wide">设备线索（选填）<input v-model="form.assetClue" maxlength="500" placeholder="例如：白色显微镜、机身标签末四位 0231" /></label></template>
        <div v-if="selectedAsset" class="selected-asset wide"><Wrench :size="18" /><span><b>{{ selectedAsset.name }}</b><small>{{ selectedAsset.location || '位置未登记' }} · {{ selectedAsset.brand }} {{ selectedAsset.model }}</small></span><em>{{ selectedAsset.assetNo }}</em></div>
        <label>上报类型<select v-model="form.reportType"><option value="MALFUNCTION">功能故障</option><option value="DAMAGE">设备损坏</option><option value="LOSS">设备丢失</option><option value="OTHER">其他问题</option></select></label>
        <label>严重程度<select v-model="form.severity"><option value="LOW">低</option><option value="MEDIUM">中</option><option value="HIGH">高</option><option value="CRITICAL">紧急</option></select></label>
        <label class="wide">问题描述<textarea v-model="form.description" maxlength="2000" :placeholder="internal ? '说明故障现象、发生时间和当前设备状态' : '描述看到的问题、发生时间以及是否存在安全风险'"></textarea></label>
        <button class="maintenance-submit wide" :disabled="loading" @click="submitReport"><Wrench :size="16" />确认上报</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.maintenance-panel{margin-top:14px;overflow:hidden}.maintenance-head{gap:16px}.maintenance-actions{display:flex;gap:8px}.maintenance-icon,.maintenance-command{height:36px;border-radius:5px;display:inline-flex;align-items:center;justify-content:center;gap:6px}.maintenance-icon{width:36px;border:1px solid #d8e3dd;background:#fff;color:#537267}.maintenance-command,.maintenance-submit{border:0;background:#225c4d;color:#fff;padding:0 13px}.maintenance-command:disabled,.maintenance-icon:disabled{opacity:.5;cursor:not-allowed}.maintenance-notice{min-height:38px;padding:0 18px;background:#eaf5ee;color:#347458;display:flex;align-items:center;gap:7px;font-size:12px;border-bottom:1px solid #dcebe2}.maintenance-notice.failed{background:#faece9;color:#a24d42}.maintenance-notice button{margin-left:auto;border:0;background:transparent;color:inherit}.maintenance-table{overflow:auto}.maintenance-row{min-width:760px;display:grid;grid-template-columns:1.35fr 1.2fr 1.8fr 1fr .7fr;gap:14px;align-items:center;min-height:58px;padding:0 22px;border-bottom:1px solid #edf1ee;font-size:12px}.maintenance-table-head{min-height:38px;background:#fafbfa;color:#84928b;font-size:10px}.maintenance-row b,.maintenance-row small{display:block}.maintenance-row small{margin-top:4px;color:#829089;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;max-width:230px}.ticket-status{width:max-content;padding:5px 8px;border-radius:3px;background:#fff0d8;color:#8f6a2f;font-size:10px}.ticket-status.repairing{background:#e5eef6;color:#426d8b}.ticket-status.waiting_acceptance{background:#eee9f7;color:#68568d}.ticket-status.closed{background:#e4f3e9;color:#347658}.ticket-status.rejected{background:#f3e9e7;color:#9a5a51}.maintenance-empty{padding:30px;text-align:center;color:#94a098;font-size:12px}.maintenance-modal-bg{position:fixed;z-index:60;inset:0;background:rgba(18,31,26,.48);display:grid;place-items:center;padding:20px}.maintenance-modal{width:min(620px,100%);max-height:92vh;overflow:auto;background:#fff;border-radius:7px}.maintenance-modal-title{padding:19px 21px;border-bottom:1px solid #e6ece9;display:flex;justify-content:space-between}.maintenance-modal-title h2{font-size:17px;margin:0 0 4px}.maintenance-modal-title p{font-size:11px;color:#82918a;margin:0}.maintenance-modal-title button{border:0;background:transparent}.maintenance-form{padding:20px;display:grid;grid-template-columns:1fr 1fr;gap:13px}.maintenance-form label{display:flex;flex-direction:column;gap:6px;color:#607169;font-size:11px}.maintenance-form select,.maintenance-form textarea{border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}.maintenance-form select{height:38px}.maintenance-form textarea{height:100px;padding:9px;resize:vertical}.wide{grid-column:1/-1}.selected-asset{display:grid;grid-template-columns:28px 1fr auto;align-items:center;padding:12px;background:#f2f7f4;color:#3c6958}.selected-asset b,.selected-asset small{display:block}.selected-asset small{margin-top:3px;color:#799087;font-size:10px}.selected-asset em{font-style:normal;font-family:monospace;font-size:11px}.maintenance-submit{height:39px;border-radius:5px;display:flex;align-items:center;justify-content:center;gap:7px}.maintenance-submit:disabled{opacity:.55}.mono{font-family:ui-monospace,monospace;color:#60756b}@media(max-width:650px){.maintenance-head{align-items:flex-start}.maintenance-actions{flex-shrink:0}.maintenance-command{width:36px;padding:0;font-size:0}.maintenance-form{grid-template-columns:1fr}.wide{grid-column:auto}.selected-asset{grid-template-columns:24px 1fr}.selected-asset em{grid-column:2}}
@media(max-width:650px){.maintenance-table{padding:10px;background:#f8faf9}.maintenance-table-head{display:none}.maintenance-row{min-width:0;grid-template-columns:minmax(0,1fr) auto;gap:9px 12px;align-items:start;min-height:0;margin-bottom:9px;padding:13px;border:1px solid #e0e8e4;border-radius:6px;background:#fff}.maintenance-row>span:nth-child(1){grid-column:1/-1;padding-bottom:8px;border-bottom:1px solid #edf1ef;font-size:10px}.maintenance-row>span:nth-child(2){grid-column:1;grid-row:2}.maintenance-row>span:nth-child(3){grid-column:1/-1;grid-row:3}.maintenance-row>span:nth-child(4){grid-column:1/-1;grid-row:4;color:#88958f;font-size:10px}.maintenance-row>span:nth-child(5){grid-column:2;grid-row:2;justify-self:end}.maintenance-row small{max-width:none;white-space:normal;line-height:1.45}.maintenance-empty{background:#fff}}
.maintenance-form input{height:38px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}
</style>
