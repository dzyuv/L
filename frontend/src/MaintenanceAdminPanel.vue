<script setup>
import { computed, ref } from "vue";
import axios from "axios";
import { Check, CircleDollarSign, RefreshCw, UserRound, Wrench, X } from "lucide-vue-next";

const props = defineProps({
  tickets: { type: Array, default: () => [] },
  assets: { type: Array, default: () => [] },
  resources: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
});
const emit = defineEmits(["refresh"]);
const statusFilter = ref("");
const severityFilter = ref("");
const selectedTicket = ref(null);
const actionStatus = ref("");
const form = ref(emptyForm());
const saving = ref(false);
const notice = ref("");
const failed = ref(false);

const assetMap = computed(() => Object.fromEntries(props.assets.map((item) => [item.id, item])));
const resourceMap = computed(() => Object.fromEntries(props.resources.map((item) => [item.id, item])));
const displayTickets = computed(() => props.tickets.filter((item) => (!statusFilter.value || item.status === statusFilter.value) && (!severityFilter.value || item.severity === severityFilter.value)));
const openCount = computed(() => props.tickets.filter((item) => !["CLOSED", "REJECTED"].includes(item.status)).length);
const nextActions = computed(() => ({
  REPORTED: [{ value: "TRIAGED", label: "受理工单" }, { value: "REPAIRING", label: "直接维修" }, { value: "REJECTED", label: "驳回上报" }],
  TRIAGED: [{ value: "REPAIRING", label: "开始维修" }, { value: "REJECTED", label: "驳回上报" }],
  REPAIRING: [{ value: "WAITING_ACCEPTANCE", label: "提交验收" }, { value: "REJECTED", label: "终止工单" }],
  WAITING_ACCEPTANCE: [{ value: "CLOSED", label: "验收并关闭" }, { value: "REPAIRING", label: "退回维修" }],
})[selectedTicket.value?.status] || []);

function emptyForm() {
  return { assetId: "", assignedTo: "", estimatedCost: "", actualCost: "", resolution: "" };
}
function statusText(value) {
  return ({ REPORTED: "待受理", TRIAGED: "已受理", REPAIRING: "维修中", WAITING_ACCEPTANCE: "待验收", CLOSED: "已关闭", REJECTED: "已驳回" })[value] || value;
}
function reportTypeText(value) {
  return ({ DAMAGE: "设备损坏", MALFUNCTION: "功能故障", LOSS: "设备丢失", OTHER: "其他问题" })[value] || value;
}
function severityText(value) {
  return ({ LOW: "低", MEDIUM: "中", HIGH: "高", CRITICAL: "紧急" })[value] || value;
}
function formatTime(value) {
  return value ? String(value).replace("T", " ").slice(0, 16) : "-";
}
function openTicket(item) {
  selectedTicket.value = item;
  actionStatus.value = "";
  form.value = {
    assetId: item.assetId ?? "",
    assignedTo: item.assignedTo ?? "",
    estimatedCost: item.estimatedCost ?? "",
    actualCost: item.actualCost ?? "",
    resolution: item.resolution || "",
  };
}
function chooseAction(status) {
  actionStatus.value = status;
}
function show(message, isFailed = false) {
  notice.value = message;
  failed.value = isFailed;
}
async function submitUpdate() {
  if (!actionStatus.value) return show("请选择本次处理动作", true);
  if (["REPAIRING", "WAITING_ACCEPTANCE", "CLOSED"].includes(actionStatus.value) && !form.value.assetId) return show("开始维修前需先绑定具体资产", true);
  if (["WAITING_ACCEPTANCE", "CLOSED", "REJECTED"].includes(actionStatus.value) && !form.value.resolution.trim()) return show("提交验收、关闭或驳回时需填写处理结果", true);
  saving.value = true;
  try {
    await axios.put(`/api/v1/admin/maintenance/tickets/${selectedTicket.value.id}`, {
      status: actionStatus.value,
      assetId: form.value.assetId === "" ? null : Number(form.value.assetId),
      assignedTo: form.value.assignedTo === "" ? null : Number(form.value.assignedTo),
      resolution: form.value.resolution.trim() || null,
      estimatedCost: form.value.estimatedCost === "" ? null : Number(form.value.estimatedCost),
      actualCost: form.value.actualCost === "" ? null : Number(form.value.actualCost),
    });
    selectedTicket.value = null;
    show("工单状态和资产状态已同步更新");
    emit("refresh");
  } catch (e) {
    show(e.response?.data?.message || "工单处理失败", true);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div v-if="notice" class="ticket-notice" :class="{ failed }">{{ notice }}<button title="关闭提示" @click="notice = ''"><X :size="14" /></button></div>
  <div class="ticket-summary">
    <div><span>全部工单</span><strong>{{ tickets.length }}</strong></div><div><span>进行中</span><strong>{{ openCount }}</strong></div><div><span>待受理</span><strong>{{ tickets.filter(item => item.status === 'REPORTED').length }}</strong></div><div><span>维修中</span><strong>{{ tickets.filter(item => item.status === 'REPAIRING').length }}</strong></div>
  </div>
  <div class="ticket-toolbar"><select v-model="statusFilter"><option value="">全部状态</option><option value="REPORTED">待受理</option><option value="TRIAGED">已受理</option><option value="REPAIRING">维修中</option><option value="WAITING_ACCEPTANCE">待验收</option><option value="CLOSED">已关闭</option><option value="REJECTED">已驳回</option></select><select v-model="severityFilter"><option value="">全部严重程度</option><option value="CRITICAL">紧急</option><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select><span>{{ displayTickets.length }} 条工单</span><button :disabled="loading" title="刷新工单" @click="emit('refresh')"><RefreshCw :size="16" /></button></div>
  <section class="ticket-table-wrap"><div class="ticket-row ticket-head"><span>工单 / 定位</span><span>上报人</span><span>类型</span><span>问题描述</span><span>上报时间</span><span>状态</span><span>处理</span></div><div v-for="item in displayTickets" :key="item.id" class="ticket-row"><span><b class="mono">{{ item.ticketNo }}</b><small>{{ assetMap[item.assetId]?.assetNo || resourceMap[item.resourceId]?.name || item.locationSnapshot || '待定位资产' }} · {{ assetMap[item.assetId]?.name || item.assetClue || '未绑定设备' }}</small></span><span>用户 {{ item.reportedBy }}</span><span><b>{{ reportTypeText(item.reportType) }}</b><small :class="`severity-${item.severity.toLowerCase()}`">{{ severityText(item.severity) }}</small></span><span class="ticket-description" :title="item.description">{{ item.description }}</span><span>{{ formatTime(item.reportedAt) }}</span><span class="ticket-status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span><span><button class="process-button" @click="openTicket(item)">{{ ['CLOSED', 'REJECTED'].includes(item.status) ? '查看' : '处理' }}</button></span></div><div v-if="!displayTickets.length" class="ticket-empty"><Wrench :size="24" />暂无符合条件的工单</div></section>

  <div v-if="selectedTicket" class="ticket-modal-bg" @click.self="selectedTicket = null"><section class="ticket-modal"><div class="ticket-modal-title"><div><h2>工单处理</h2><p>{{ selectedTicket.ticketNo }}</p></div><button title="关闭" @click="selectedTicket = null"><X :size="18" /></button></div><div class="ticket-detail">
    <div class="ticket-asset"><Wrench :size="20" /><span><b>{{ assetMap[selectedTicket.assetId]?.name || resourceMap[selectedTicket.resourceId]?.name || '待确认具体设备' }}</b><small>{{ assetMap[selectedTicket.assetId]?.assetNo || selectedTicket.locationSnapshot || '位置未登记' }}</small></span><em class="ticket-status" :class="selectedTicket.status.toLowerCase()">{{ statusText(selectedTicket.status) }}</em></div>
    <dl><div><dt>上报人</dt><dd>用户 {{ selectedTicket.reportedBy }}</dd></div><div><dt>问题类型</dt><dd>{{ reportTypeText(selectedTicket.reportType) }} · {{ severityText(selectedTicket.severity) }}</dd></div><div><dt>问题位置</dt><dd>{{ resourceMap[selectedTicket.resourceId]?.name || '-' }} · {{ selectedTicket.locationSnapshot || '未填写' }}</dd></div><div><dt>设备线索</dt><dd>{{ selectedTicket.assetClue || '未填写' }}</dd></div><div class="wide"><dt>问题描述</dt><dd>{{ selectedTicket.description }}</dd></div></dl>
    <template v-if="nextActions.length"><div class="action-title">下一步处理</div><div class="action-buttons"><button v-for="action in nextActions" :key="action.value" :class="{ active: actionStatus === action.value, danger: action.value === 'REJECTED' }" @click="chooseAction(action.value)"><Check v-if="actionStatus === action.value" :size="14" />{{ action.label }}</button></div><div class="ticket-form"><label class="wide"><Wrench :size="14" />绑定具体资产<select v-model="form.assetId"><option value="">尚未确认</option><option v-for="asset in assets" :key="asset.id" :value="asset.id">{{ asset.assetNo }} · {{ asset.name }} · {{ asset.location || '位置未登记' }}</option></select></label><label><UserRound :size="14" />维修负责人用户 ID<input v-model="form.assignedTo" type="number" min="1" placeholder="选填" /></label><label><CircleDollarSign :size="14" />预计费用<input v-model="form.estimatedCost" type="number" min="0" step="0.01" /></label><label><CircleDollarSign :size="14" />实际费用<input v-model="form.actualCost" type="number" min="0" step="0.01" /></label><label class="wide">处理记录<textarea v-model="form.resolution" maxlength="2000" placeholder="记录诊断结果、维修内容或驳回原因"></textarea></label><button class="submit-ticket wide" :disabled="saving || !actionStatus" @click="submitUpdate"><Check :size="16" />确认处理</button></div></template>
    <div v-else class="closed-result"><b>处理结果</b><p>{{ selectedTicket.resolution || '未填写处理结果' }}</p><span>预计费用：{{ selectedTicket.estimatedCost ?? '-' }} · 实际费用：{{ selectedTicket.actualCost ?? '-' }}</span></div>
  </div></section></div>
</template>

<style scoped>
.ticket-notice{min-height:38px;padding:0 12px;margin-bottom:12px;background:#e8f5ed;color:#347458;display:flex;align-items:center;border-left:3px solid #4d9a70;font-size:12px}.ticket-notice.failed{background:#faece9;color:#a24d42;border-color:#bd655a}.ticket-notice button{margin-left:auto;border:0;background:transparent;color:inherit}.ticket-summary{display:grid;grid-template-columns:repeat(4,1fr);background:#fff;border:1px solid #dfe7e3;margin-bottom:12px}.ticket-summary>div{padding:15px 18px;border-right:1px solid #e8eeeb}.ticket-summary>div:last-child{border:0}.ticket-summary span,.ticket-summary strong{display:block}.ticket-summary span{color:#7d8c85;font-size:10px}.ticket-summary strong{font-size:22px;margin-top:5px}.ticket-toolbar{display:flex;align-items:center;gap:8px;margin-bottom:12px}.ticket-toolbar select{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}.ticket-toolbar span{margin-left:auto;color:#7b8b84;font-size:11px}.ticket-toolbar button{width:36px;height:36px;border:1px solid #d7e1dc;background:#fff;color:#597068;border-radius:5px;display:grid;place-items:center}.ticket-table-wrap{background:#fff;border:1px solid #dfe7e3;border-radius:6px;overflow:auto}.ticket-row{min-width:1080px;display:grid;grid-template-columns:1.45fr .75fr .85fr 1.7fr 1fr .75fr .55fr;gap:14px;align-items:center;min-height:61px;padding:0 18px;border-bottom:1px solid #edf1ef;font-size:11px}.ticket-head{min-height:38px;background:#fafbfa;color:#839089;font-size:10px}.ticket-row b,.ticket-row small{display:block}.ticket-row small{margin-top:4px;color:#7d8c85}.ticket-description{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.severity-critical{color:#a54d46!important}.severity-high{color:#9a6c31!important}.ticket-status{width:max-content;padding:5px 7px;border-radius:3px;background:#fff0d8;color:#8f6a2f;font-size:10px;font-style:normal}.ticket-status.repairing{background:#e5eef6;color:#426d8b}.ticket-status.waiting_acceptance{background:#eee9f7;color:#68568d}.ticket-status.closed{background:#e4f3e9;color:#347658}.ticket-status.rejected{background:#f3e9e7;color:#9a5a51}.process-button{height:30px;border:1px solid #d4e1da;background:#fff;color:#3d6c5a;border-radius:4px;padding:0 9px;font-size:10px}.ticket-empty{padding:36px;display:flex;justify-content:center;align-items:center;gap:8px;color:#8d9a94;font-size:12px}.ticket-modal-bg{position:fixed;z-index:60;inset:0;background:rgba(18,31,26,.48);display:grid;place-items:center;padding:20px}.ticket-modal{width:min(680px,100%);max-height:92vh;overflow:auto;background:#fff;border-radius:7px}.ticket-modal-title{padding:19px 21px;border-bottom:1px solid #e6ece9;display:flex;justify-content:space-between}.ticket-modal-title h2{font-size:17px;margin:0 0 4px}.ticket-modal-title p{font-size:11px;color:#82918a;margin:0}.ticket-modal-title button{border:0;background:transparent}.ticket-detail{padding:20px}.ticket-asset{display:grid;grid-template-columns:30px 1fr auto;align-items:center;padding:13px;background:#f2f7f4}.ticket-asset b,.ticket-asset small{display:block}.ticket-asset small{margin-top:4px;color:#799087;font-size:10px}.ticket-detail dl{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin:17px 0}.ticket-detail dl div{padding-bottom:10px;border-bottom:1px solid #edf1ef}.ticket-detail dt{font-size:10px;color:#87948e}.ticket-detail dd{margin:5px 0 0;font-size:12px;line-height:1.5}.wide{grid-column:1/-1}.action-title{font-size:11px;color:#778880;margin-bottom:8px}.action-buttons{display:flex;flex-wrap:wrap;gap:7px}.action-buttons button{height:34px;padding:0 11px;border:1px solid #d5e1da;background:#fff;color:#426b5b;border-radius:4px;display:flex;align-items:center;gap:5px}.action-buttons button.active{background:#225c4d;border-color:#225c4d;color:#fff}.action-buttons button.danger{color:#9b5149;border-color:#e3cfcb}.action-buttons button.danger.active{background:#9b5149;color:#fff}.ticket-form{display:grid;grid-template-columns:1fr 1fr 1fr;gap:11px;margin-top:16px}.ticket-form label{display:flex;flex-wrap:wrap;align-items:center;gap:5px;color:#607169;font-size:10px}.ticket-form input,.ticket-form textarea{width:100%;height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;color:#243b31;background:#fff}.ticket-form textarea{height:78px;padding:8px;resize:vertical}.submit-ticket{height:38px;border:0;border-radius:5px;background:#225c4d;color:#fff;display:flex;align-items:center;justify-content:center;gap:6px}.submit-ticket:disabled{opacity:.5}.closed-result{padding:14px;background:#f6f8f7}.closed-result b{font-size:12px}.closed-result p{font-size:12px;line-height:1.6}.closed-result span{font-size:10px;color:#7c8c85}.mono{font-family:ui-monospace,monospace}@media(max-width:650px){.ticket-summary{grid-template-columns:1fr 1fr}.ticket-summary>div:nth-child(2){border-right:0}.ticket-summary>div{border-bottom:1px solid #e8eeeb}.ticket-toolbar{flex-wrap:wrap}.ticket-toolbar select{flex:1;min-width:120px}.ticket-toolbar span{display:none}.ticket-detail dl,.ticket-form{grid-template-columns:1fr}.wide{grid-column:auto}.ticket-asset{grid-template-columns:25px 1fr}.ticket-asset em{grid-column:2;margin-top:7px}}
.ticket-form select{width:100%;height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;color:#243b31;background:#fff}
</style>
