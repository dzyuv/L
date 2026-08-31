<script setup>
import { computed, onMounted, ref } from "vue";
import axios from "axios";
import AssetAdminPanel from "./AssetAdminPanel.vue";
import MaintenanceAdminPanel from "./MaintenanceAdminPanel.vue";
import {
  AlertTriangle, BarChart3, CalendarClock, Check, ChevronRight, ClipboardCheck,
  FlaskConical, PackageSearch, Plus, RefreshCw, Save, Search, Settings, Shield, ScrollText, Users, Wrench, X,
} from "lucide-vue-next";
const PIE_COLORS = ["#2f7a5f", "#d4a24c", "#5b7fa6", "#c36b5a", "#8b6dad", "#4d8f7a", "#b08968", "#6aa88a"];
const STATUS_PIE_COLORS = {
  APPROVED: "#65a184",
  PENDING_APPROVAL: "#d4a24c",
  CHECKED_IN: "#2f7a5f",
  COMPLETED: "#5b7fa6",
  REJECTED: "#c36b5a",
  CANCELED: "#8a9690",
  NO_SHOW: "#9a5a51",
  EXPIRED: "#b08968",
};

const props = defineProps({ user: { type: Object, required: true }, initialResources: { type: Array, default: () => [] } });
const isSystemAdmin = computed(() => (props.user?.roles || []).includes("SYSTEM_ADMIN"));
const tabs = computed(() => isSystemAdmin.value
  ? [{ id: "overview", label: "总览", icon: BarChart3 }, { id: "users", label: "用户与角色", icon: Users }, { id: "configs", label: "系统配置", icon: Settings }, { id: "logs", label: "管理员日志", icon: ScrollText }]
  : [{ id: "overview", label: "运营总览", icon: BarChart3 }, { id: "resources", label: "资源管理", icon: FlaskConical }, { id: "resource-types", label: "资源类别", icon: FlaskConical }, { id: "assets", label: "资产台账", icon: PackageSearch }, { id: "maintenance", label: "报修处理", icon: Wrench }, { id: "bookings", label: "预约", icon: ClipboardCheck }, { id: "violations", label: "违约处理", icon: AlertTriangle }]);
const activeTab = ref("overview");
const loading = ref(false);
const notice = ref("");
const error = ref(false);
const resources = ref([...props.initialResources]);
const resourceTypes = ref([]);
const bookings = ref([]);
const approvals = ref([]);
const rejectionTarget = ref(null);
const rejectionReason = ref("");
const rejectionSaving = ref(false);
const violations = ref([]);
const dismissTarget = ref(null);
const dismissReason = ref("");
const dismissSaving = ref(false);
const users = ref([]);
const teachers = ref([]);
const roles = ref([]);
const configs = ref([]);
const operationLogs = ref([]);
const logQuery = ref("");
const logTypeFilter = ref("");
const logResultFilter = ref("");
const assets = ref([]);
const assetCategories = ref([]);
const maintenanceTickets = ref([]);
const stats = ref(null);
const query = ref("");
const selectedUser = ref(null);
const selectedRoles = ref([]);
const resetPassword = ref("");
const resourceDialog = ref(false);
const selectedResource = ref(null);
const resourceForm = ref(emptyResource());
const schedules = ref([]);
const closures = ref([]);
const managers = ref([]);
const closureForm = ref({ startTime: "", endTime: "", reason: "" });
const managerForm = ref({ userId: "" });
const resourceTypeEditForm = ref({ name: "", defaultApprovalLevel: 1, enabled: true });
const resourceTypeDialog = ref(false);
const editingResourceType = ref(null);
const resourceTypeActionId = ref(null);
let noticeTimer;

const availableTeachers = computed(() => {
  const assigned = new Set(managers.value.map((item) => Number(item.userId)));
  return teachers.value.filter((item) => !assigned.has(Number(item.id)));
});
const pendingBookings = computed(() => bookings.value.filter((item) => item.status === "PENDING_APPROVAL").length);
const activeUsers = computed(() => users.value.filter((item) => item.status === "ACTIVE").length);
const openMaintenance = computed(() => maintenanceTickets.value.filter((item) => !["CLOSED", "REJECTED"].includes(item.status)).length);
const repairingAssets = computed(() => assets.value.filter((item) => item.status === "MAINTENANCE").length);
const highValueAssets = computed(() => {
  const highValueCategoryIds = new Set(assetCategories.value.filter((item) => item.highValue).map((item) => item.id));
  return assets.value.filter((item) => highValueCategoryIds.has(item.categoryId)).length;
});
const displayUsers = computed(() => {
  const term = query.value.trim().toLowerCase();
  if (!term) return users.value;
  return users.value.filter((item) => [item.username, item.realName, item.email].some((value) => String(value || "").toLowerCase().includes(term)));
});
const operationTypes = computed(() => [...new Set(operationLogs.value.map((item) => item.operationType).filter(Boolean))]);
const displayOperationLogs = computed(() => {
  const term = logQuery.value.trim().toLowerCase();
  return operationLogs.value.filter((item) => {
    const operator = users.value.find((user) => Number(user.id) === Number(item.operatorId));
    const matchesTerm = !term || [operator?.realName, operator?.username, item.operationType,
      item.targetType, item.targetId, item.requestId, item.ip, JSON.stringify(item.detail || {})]
      .some((value) => String(value || "").toLowerCase().includes(term));
    return matchesTerm
      && (!logTypeFilter.value || item.operationType === logTypeFilter.value)
      && (!logResultFilter.value || item.result === logResultFilter.value);
  });
});
const labReport = computed(() => {
  const counts = bookings.value.reduce((map, item) => { map[item.status] = (map[item.status] || 0) + 1; return map; }, {});
  const resourceCounts = bookings.value.reduce((map, item) => {
    const key = item.resourceNameSnapshot || `资源 ${item.resourceId}`;
    map[key] = (map[key] || 0) + 1;
    return map;
  }, {});
  const usageRanking = (stats.value?.ranking || []).map((item) => ({
    name: item.resourceName,
    count: Number(item.bookingCount || 0),
    ratePercent: Number(item.ratePercent || 0),
  })).slice(0, 6);
  const resourceRanking = usageRanking.length
    ? usageRanking
    : Object.entries(resourceCounts).map(([name, count]) => ({ name, count, ratePercent: null })).sort((a, b) => b.count - a.count).slice(0, 6);
  const maxResourceCount = resourceRanking[0]?.count || 1;
  const statusItems = [
    { key: "APPROVED", label: "已通过", count: counts.APPROVED || 0 },
    { key: "PENDING_APPROVAL", label: "待审批", count: counts.PENDING_APPROVAL || 0 },
    { key: "CHECKED_IN", label: "已签到", count: counts.CHECKED_IN || 0 },
    { key: "COMPLETED", label: "已完成", count: counts.COMPLETED || 0 },
    { key: "REJECTED", label: "已驳回", count: counts.REJECTED || 0 },
    { key: "CANCELED", label: "已取消", count: counts.CANCELED || 0 },
    { key: "NO_SHOW", label: "未签到", count: counts.NO_SHOW || 0 },
    { key: "EXPIRED", label: "审批超时", count: counts.EXPIRED || 0 },
  ].map((item) => ({ ...item, color: STATUS_PIE_COLORS[item.key] || PIE_COLORS[0] }));
  const upcoming = bookings.value.filter(item => item.startTime && new Date(item.startTime) >= new Date()).sort((a, b) => new Date(a.startTime) - new Date(b.startTime)).slice(0, 6);
  const violationSource = violations.value.length
    ? violations.value
    : bookings.value.filter((item) => item.status === "NO_SHOW");
  const userViolations = aggregateUserCounts(violationSource, (item) => item.applicantName || item.applicantNameSnapshot);
  const statusPieItems = statusItems.filter((item) => item.count > 0);
  return {
    total: bookings.value.length,
    active: (counts.APPROVED || 0) + (counts.CHECKED_IN || 0),
    resourceRanking,
    maxResourceCount,
    statusItems,
    statusPie: buildPie(statusPieItems),
    upcoming,
    userViolations,
    maxViolationCount: userViolations[0]?.count || 1,
  };
});
const compactBookings = computed(() => {
  const now = Date.now();
  return [...bookings.value].sort((left, right) => {
    const leftEnd = new Date(left.endTime || left.startTime).getTime();
    const rightEnd = new Date(right.endTime || right.startTime).getTime();
    const leftCurrent = Number.isFinite(leftEnd) && leftEnd >= now;
    const rightCurrent = Number.isFinite(rightEnd) && rightEnd >= now;
    if (leftCurrent !== rightCurrent) return leftCurrent ? -1 : 1;
    return new Date(left.startTime) - new Date(right.startTime);
  }).slice(0, 8);
});

function emptyResource() {
  return { typeId: "", name: "", location: "", capacity: 1, description: "", imageUrl: "", maxDurationMinutes: 120, needCheckin: true, approvalLevelOverride: "" };
}
function show(message, failed = false) {
  notice.value = message;
  error.value = failed;
  clearTimeout(noticeTimer);
  noticeTimer = setTimeout(() => { notice.value = ""; }, 5000);
}
function apiData(response, fallback = []) { return response.data?.data ?? fallback; }
function statusText(value) {
  return ({ ACTIVE: "正常", DISABLED: "已禁用", LOCKED: "已锁定", PENDING_APPROVAL: "待审批", APPROVED: "已通过", REJECTED: "已驳回", CANCELED: "已取消", CHECKED_IN: "已签到", COMPLETED: "已完成", NO_SHOW: "未签到", EXPIRED: "审批超时", OPEN: "待处理", RESOLVED: "已解决", CONFIRMED: "已确认", DISMISSED: "已撤销", LIFTED: "已解除" })[value] || value;
}
function violationTypeText(value) {
  return ({ NO_SHOW: "未签到" })[value] || value;
}
function formatTime(value) { return value ? String(value).replace("T", " ").slice(0, 16) : "-"; }
function teacherLabel(item) {
  const name = item?.realName || item?.username || `用户 ${item?.id}`;
  const account = item?.username || item?.employeeNo;
  return account && account !== name ? `${name}（${account}）` : name;
}
function managerName(item) {
  const teacher = teachers.value.find((user) => Number(user.id) === Number(item.userId));
  return teacher ? teacherLabel(teacher) : `用户 ${item.userId}`;
}
function roleName(code) { return ({ STUDENT: "学生", TEACHER: "教师", LAB_ADMIN: "实验室管理员", SYSTEM_ADMIN: "系统管理员" })[code] || code; }
function operationName(code) { return ({ USER_STATUS_UPDATED: "修改用户状态", USER_ROLES_UPDATED: "调整用户角色", USER_PASSWORD_RESET: "重置用户密码", USER_DELETED: "删除用户", APPROVAL_FLOW_PUBLISHED: "发布审批流", SYSTEM_CONFIG_UPDATED: "修改系统配置" })[code] || code; }
function operationTarget(item) {
  const label = ({ USER: "用户", RESOURCE_TYPE: "资源类别", SYSTEM_CONFIG: "系统配置" })[item.targetType] || item.targetType || "系统";
  return item.targetId ? `${label} #${item.targetId}` : label;
}
function operationOperator(item) {
  const operator = users.value.find((user) => Number(user.id) === Number(item.operatorId));
  return operator ? `${operator.realName}（${operator.username}）` : `管理员 #${item.operatorId}`;
}
function operationDetail(item) {
  const detail = item.detail || {};
  if (item.operationType === "USER_STATUS_UPDATED") return `${detail.username || "用户"} → ${statusText(detail.status)}`;
  if (item.operationType === "USER_ROLES_UPDATED") return `${detail.username || "用户"} → ${(detail.roles || []).map(roleName).join(" / ")}`;
  if (item.operationType === "USER_PASSWORD_RESET") return `已重置 ${detail.username || "用户"} 的密码`;
  if (item.operationType === "USER_DELETED") return `已删除 ${detail.realName || detail.username || "用户"}`;
  if (item.operationType === "APPROVAL_FLOW_PUBLISHED") return `版本 v${detail.version || "-"}，${detail.nodeCount || 0} 个审批节点`;
  if (item.operationType === "SYSTEM_CONFIG_UPDATED") return `配置项 ${detail.key || "-"}`;
  return item.reason || "-";
}

async function loadAll() {
  loading.value = true;
  try {
    if (isSystemAdmin.value) {
      const [userResponse, roleResponse, configResponse, logResponse] = await Promise.all([
        axios.get("/api/v1/admin/users"), axios.get("/api/v1/admin/roles"),
        axios.get("/api/v1/admin/configs"), axios.get("/api/v1/admin/operation-logs"),
      ]);
      users.value = apiData(userResponse, {}).items || [];
      roles.value = apiData(roleResponse, {}).items || [];
      configs.value = apiData(configResponse, []);
      operationLogs.value = apiData(logResponse, {}).items || [];
    } else {
      const [resourceResponse, typeResponse, bookingResponse, approvalResponse, assetResponse, categoryResponse, ticketResponse, teacherResponse, violationResponse] = await Promise.all([
        axios.get("/api/v1/resources"), axios.get("/api/v1/admin/resource-types"),
        axios.get("/api/v1/admin/bookings"), axios.get("/api/v1/approvals/mine"),
        axios.get("/api/v1/admin/assets"), axios.get("/api/v1/admin/assets/categories"),
        axios.get("/api/v1/admin/maintenance/tickets"), axios.get("/api/v1/admin/users/teachers"),
        axios.get("/api/v1/admin/violations"),
      ]);
      resources.value = apiData(resourceResponse, []);
      resourceTypes.value = apiData(typeResponse, []);
      bookings.value = apiData(bookingResponse, {}).items || [];
      approvals.value = apiData(approvalResponse, []);
      assets.value = apiData(assetResponse, {}).items || [];
      assetCategories.value = apiData(categoryResponse, []);
      maintenanceTickets.value = apiData(ticketResponse, {}).items || [];
      teachers.value = apiData(teacherResponse, {}).items || [];
      violations.value = apiData(violationResponse, {}).items || [];
      await loadStatistics();
    }
  } catch (e) { show(e.response?.data?.message || "管理数据加载失败", true); }
  finally { loading.value = false; }
}

async function loadStatistics() {
  try {
    const response = await axios.get("/api/v1/statistics/usage");
    stats.value = apiData(response, null);
  } catch {
    stats.value = null;
  }
}
function formatMinutes(value) {
  const minutes = Number(value || 0);
  if (minutes < 60) return `${minutes} 分钟`;
  return `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟`;
}
function formatPercent(value) {
  const amount = Number(value || 0);
  return `${amount.toFixed(1)}%`;
}
async function updateUserStatus(item) {
  const status = item.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
  try { await axios.put(`/api/v1/admin/users/${item.id}/status`, { status }); show(status === "ACTIVE" ? "账号已启用" : "账号已禁用"); await loadAll(); }
  catch (e) { show(e.response?.data?.message || "账号状态更新失败", true); }
}
function editUser(item) { selectedUser.value = item; selectedRoles.value = [...item.roles]; resetPassword.value = ""; }
async function saveUserRoles() {
  try { await axios.put(`/api/v1/admin/users/${selectedUser.value.id}/roles`, { roles: selectedRoles.value }); show("用户角色已更新"); selectedUser.value = null; await loadAll(); }
  catch (e) { show(e.response?.data?.message || "角色更新失败", true); }
}
async function saveResetPassword() {
  if (resetPassword.value.length < 8) return show("新密码至少 8 位", true);
  try { await axios.post(`/api/v1/admin/users/${selectedUser.value.id}/reset-password`, { password: resetPassword.value }); show("密码已重置，用户可立即登录"); resetPassword.value = ""; }
  catch (e) { show(e.response?.data?.message || "密码重置失败", true); }
}
function aggregateUserCounts(rows, nameOf) {
  const grouped = new Map();
  for (const item of rows) {
    const userId = Number(item.userId);
    if (!userId) continue;
    const current = grouped.get(userId) || { userId, name: "", count: 0 };
    current.count += 1;
    current.name = current.name || nameOf(item) || statsUserLabel(userId);
    grouped.set(userId, current);
  }
  return [...grouped.values()].sort((left, right) => right.count - left.count || left.name.localeCompare(right.name, "zh"));
}
function polar(cx, cy, radius, deg) {
  const rad = (deg * Math.PI) / 180;
  return { x: +(cx + radius * Math.cos(rad)).toFixed(3), y: +(cy + radius * Math.sin(rad)).toFixed(3) };
}
function donutPath(startDeg, sweepDeg, cx = 80, cy = 80, outer = 72, inner = 44) {
  if (sweepDeg <= 0) return "";
  if (sweepDeg >= 359.99) {
    return `M ${cx} ${cy - outer} A ${outer} ${outer} 0 1 1 ${cx} ${cy + outer} A ${outer} ${outer} 0 1 1 ${cx} ${cy - outer} M ${cx} ${cy - inner} A ${inner} ${inner} 0 1 0 ${cx} ${cy + inner} A ${inner} ${inner} 0 1 0 ${cx} ${cy - inner}`;
  }
  const outerStart = polar(cx, cy, outer, startDeg);
  const outerEnd = polar(cx, cy, outer, startDeg + sweepDeg);
  const innerStart = polar(cx, cy, inner, startDeg);
  const innerEnd = polar(cx, cy, inner, startDeg + sweepDeg);
  const large = sweepDeg > 180 ? 1 : 0;
  return `M ${outerStart.x} ${outerStart.y} A ${outer} ${outer} 0 ${large} 1 ${outerEnd.x} ${outerEnd.y} L ${innerEnd.x} ${innerEnd.y} A ${inner} ${inner} 0 ${large} 0 ${innerStart.x} ${innerStart.y} Z`;
}
function buildPie(items) {
  const total = items.reduce((sum, item) => sum + Number(item.count || 0), 0);
  let cursor = -90;
  return items.map((item, index) => {
    const count = Number(item.count || 0);
    const sweep = total ? (count / total) * 360 : 0;
    const slice = {
      ...item,
      count,
      color: item.color || PIE_COLORS[index % PIE_COLORS.length],
      percent: total ? Math.round((count / total) * 100) : 0,
      path: donutPath(cursor, sweep),
    };
    cursor += sweep;
    return slice;
  });
}
function statsUserLabel(userId) {
  const booking = bookings.value.find((item) => Number(item.userId) === Number(userId));
  if (booking?.applicantNameSnapshot) return booking.applicantNameSnapshot;
  const teacher = teachers.value.find((item) => Number(item.id) === Number(userId));
  if (teacher) return teacherLabel(teacher);
  const violation = violations.value.find((item) => Number(item.userId) === Number(userId));
  if (violation?.applicantName) return violation.applicantName;
  return `用户 ${userId}`;
}
async function deleteUser(item) {
  if (Number(item.id) === Number(props.user?.id)) return show("不能删除当前登录账号", true);
  if (!window.confirm(`确定删除用户“${item.realName || item.username}”吗？删除后该账号无法登录。`)) return;
  try {
    await axios.delete(`/api/v1/admin/users/${item.id}`);
    if (selectedUser.value?.id === item.id) selectedUser.value = null;
    show("用户已删除");
    await loadAll();
  } catch (e) { show(e.response?.data?.message || "用户删除失败", true); }
}
async function saveConfig(item) {
  try { await axios.put(`/api/v1/admin/configs/${encodeURIComponent(item.key)}`, { value: item.value }); show(`配置 ${item.key} 已保存`); await loadAll(); }
  catch (e) { show(e.response?.data?.message || "配置保存失败", true); }
}
function editResourceType(item) {
  editingResourceType.value = item;
  resourceTypeEditForm.value = { name: item.name, defaultApprovalLevel: item.defaultApprovalLevel ?? 1, enabled: item.enabled !== false };
  resourceTypeDialog.value = true;
}
function beginCreateResourceType() {
  editingResourceType.value = null;
  resourceTypeEditForm.value = { name: "", defaultApprovalLevel: 1, enabled: true };
  resourceTypeDialog.value = true;
}
async function saveResourceType() {
  const name = resourceTypeEditForm.value.name.trim();
  if (!name) return show("请填写资源类别名称", true);
  const isEditing = Boolean(editingResourceType.value);
  try {
    if (isEditing) {
      await axios.put(`/api/v1/admin/resource-types/${editingResourceType.value.id}`, { name, defaultApprovalLevel: Number(resourceTypeEditForm.value.defaultApprovalLevel), enabled: resourceTypeEditForm.value.enabled });
    } else {
      await axios.post("/api/v1/admin/resource-types", { name, defaultApprovalLevel: Number(resourceTypeEditForm.value.defaultApprovalLevel) });
    }
    resourceTypeDialog.value = false;
    editingResourceType.value = null;
    show(isEditing ? "资源类别已更新" : "资源类别已创建");
    await loadAll();
  } catch (e) { show(e.response?.data?.message || "资源类别保存失败", true); }
}
async function toggleResourceType(item) {
  if (resourceTypeActionId.value) return;
  resourceTypeActionId.value = item.id;
  try {
    await axios.put(`/api/v1/admin/resource-types/${item.id}`, { name: item.name, defaultApprovalLevel: item.defaultApprovalLevel, enabled: item.enabled === false });
    show(item.enabled === false ? "资源类别已启用" : "资源类别已停用");
    await loadAll();
  } catch (e) { show(e.response?.data?.message || "资源类别状态更新失败", true); }
  finally { resourceTypeActionId.value = null; }
}
async function deleteResourceType(item) {
  const linkedCount = resources.value.filter(resource => Number(resource.typeId) === Number(item.id)).length;
  if (linkedCount > 0) return show(`该类别仍关联 ${linkedCount} 个资源，请先迁移或删除这些资源`, true);
  if (!window.confirm(`确定删除资源类别“${item.name}”吗？删除后将无法恢复。`)) return;
  try {
    await axios.delete(`/api/v1/admin/resource-types/${item.id}`);
    if (editingResourceType.value?.id === item.id) { editingResourceType.value = null; resourceTypeDialog.value = false; }
    show("资源类别已删除");
    await loadAll();
  } catch (e) { show(e.response?.data?.message || "资源类别删除失败，可能仍被资源使用", true); }
}

function createResource() { resourceForm.value = emptyResource(); resourceDialog.value = true; }
function editResource(item) { resourceForm.value = { typeId: item.typeId, name: item.name, location: item.location, capacity: item.capacity, description: item.description || "", imageUrl: item.imageUrl || "", maxDurationMinutes: item.maxDurationMinutes, needCheckin: item.needCheckin, approvalLevelOverride: item.approvalLevelOverride ?? "" }; selectedResource.value = item; resourceDialog.value = true; }
async function saveResource() {
  const payload = {
    ...resourceForm.value,
    approvalLevelOverride: resourceForm.value.approvalLevelOverride === "" || resourceForm.value.approvalLevelOverride === null
      ? null
      : Number(resourceForm.value.approvalLevelOverride),
  };
  try {
    if (selectedResource.value) await axios.put(`/api/v1/admin/resources/${selectedResource.value.id}`, payload);
    else await axios.post("/api/v1/admin/resources", payload);
    show(selectedResource.value ? "资源信息已更新" : "资源已创建"); resourceDialog.value = false; selectedResource.value = null; await loadAll();
  } catch (e) { show(e.response?.data?.message || "资源保存失败", true); }
}
async function manageResource(item) {
  selectedResource.value = item; activeTab.value = "resources";
  try {
    const [scheduleResponse, closureResponse, managerResponse] = await Promise.all([
      axios.get(`/api/v1/admin/resources/${item.id}/schedules`), axios.get(`/api/v1/admin/resources/${item.id}/closures`), axios.get(`/api/v1/admin/resources/${item.id}/managers`),
    ]);
    const existing = apiData(scheduleResponse, []);
    schedules.value = existing.length ? existing.map(row => ({ weekday: row.weekday, openTime: row.openTime?.slice(0, 5), closeTime: row.closeTime?.slice(0, 5), slotMinutes: row.slotMinutes, maxDurationMinutes: row.maxDurationMinutes }))
      : [1, 2, 3, 4, 5].map(weekday => ({ weekday, openTime: "09:00", closeTime: "17:00", slotMinutes: 30, maxDurationMinutes: 120 }));
    closures.value = apiData(closureResponse, []);
    managers.value = apiData(managerResponse, []);
  } catch (e) { show(e.response?.data?.message || "资源规则加载失败", true); }
}
async function addManager() {
  const userId = Number(managerForm.value.userId);
  if (!Number.isInteger(userId) || userId < 1) return show("请选择要添加的教师", true);
  try {
    await axios.post(`/api/v1/admin/resources/${selectedResource.value.id}/managers`, { userId });
    managerForm.value = { userId: "" };
    show("资源负责人已保存");
    await manageResource(selectedResource.value);
  } catch (e) { show(e.response?.data?.message || "资源负责人保存失败", true); }
}
async function removeManager(item) {
  try { await axios.delete(`/api/v1/admin/resources/${selectedResource.value.id}/managers/${item.id}`); show("资源负责人已撤销"); await manageResource(selectedResource.value); }
  catch (e) { show(e.response?.data?.message || "资源负责人撤销失败", true); }
}
function addSchedule() { schedules.value.push({ weekday: 1, openTime: "09:00", closeTime: "17:00", slotMinutes: 30, maxDurationMinutes: 120 }); }
async function saveSchedules() {
  try { await axios.put(`/api/v1/admin/resources/${selectedResource.value.id}/schedules`, schedules.value.map(row => ({ ...row, openTime: `${row.openTime}:00`, closeTime: `${row.closeTime}:00` }))); show("开放时间已保存"); }
  catch (e) { show(e.response?.data?.message || "开放时间保存失败", true); }
}
async function createClosure() {
  if (!closureForm.value.startTime || !closureForm.value.endTime) return show("请选择维护开始和结束时间", true);
  if (new Date(closureForm.value.startTime) >= new Date(closureForm.value.endTime)) return show("维护结束时间必须晚于开始时间", true);
  if (!closureForm.value.reason.trim()) return show("请填写维护或临时关闭原因", true);
  try {
    const response = await axios.post(`/api/v1/admin/resources/${selectedResource.value.id}/closures`, closureForm.value);
    const cancelled = Number(response.data?.data?.cancelledCount || 0);
    closureForm.value = { startTime: "", endTime: "", reason: "" };
    show(cancelled > 0 ? `维护时段已创建，已取消 ${cancelled} 条占用该时段的预约` : "维护时段已创建");
    await manageResource(selectedResource.value);
  }
  catch (e) { show(e.response?.data?.message || "维护时段创建失败", true); }
}
async function cancelClosure(item) {
  try { await axios.post(`/api/v1/admin/resources/${selectedResource.value.id}/closures/${item.id}/cancel`); show("维护时段已取消"); await manageResource(selectedResource.value); }
  catch (e) { show(e.response?.data?.message || "维护时段取消失败", true); }
}
async function approveTask(item) {
  try { await axios.post(`/api/v1/approvals/${item.id}/approve`, {}); show("审批已通过"); await loadAll(); }
  catch (e) { show(e.response?.data?.message || "审批处理失败", true); await loadAll(); }
}
function openRejection(item) {
  rejectionTarget.value = item;
  rejectionReason.value = "";
}
function closeRejection() {
  if (rejectionSaving.value) return;
  rejectionTarget.value = null;
  rejectionReason.value = "";
}
async function submitRejection() {
  const reason = rejectionReason.value.trim();
  if (!reason || !rejectionTarget.value) {
    show("请填写驳回原因", true);
    return;
  }
  rejectionSaving.value = true;
  try {
    await axios.post(`/api/v1/approvals/${rejectionTarget.value.id}/reject`, { comment: reason });
    rejectionTarget.value = null;
    rejectionReason.value = "";
    show("审批已驳回");
    await loadAll();
  } catch (e) {
    show(e.response?.data?.message || "驳回操作失败", true);
    rejectionTarget.value = null;
    rejectionReason.value = "";
    await loadAll();
  } finally {
    rejectionSaving.value = false;
  }
}
async function confirmViolation(item) {
  try {
    await axios.put(`/api/v1/admin/violations/${item.id}`, { status: "CONFIRMED" });
    show("已确认该次违约");
    await loadAll();
  } catch (e) {
    show(e.response?.data?.message || "确认违约失败", true);
    await loadAll();
  }
}
function openDismiss(item) {
  dismissTarget.value = item;
  dismissReason.value = "";
}
function closeDismiss() {
  if (dismissSaving.value) return;
  dismissTarget.value = null;
  dismissReason.value = "";
}
async function submitDismiss() {
  const reason = dismissReason.value.trim();
  if (!reason || !dismissTarget.value) {
    show("请填写撤销原因", true);
    return;
  }
  dismissSaving.value = true;
  try {
    await axios.put(`/api/v1/admin/violations/${dismissTarget.value.id}`, { status: "DISMISSED", comment: reason });
    dismissTarget.value = null;
    dismissReason.value = "";
    show("已撤销该次违约，相关预约限制已重新计算");
    await loadAll();
  } catch (e) {
    show(e.response?.data?.message || "撤销违约失败", true);
    await loadAll();
  } finally {
    dismissSaving.value = false;
  }
}
onMounted(loadAll);
</script>

<template>
  <main class="admin-shell">
    <aside class="admin-sidebar">
      <div class="workspace-mark"><Shield :size="20" /><div><b>{{ isSystemAdmin ? '系统管理中心' : '实验室管理中心' }}</b><small>{{ user.realName }}</small></div></div>
      <nav>
        <button v-for="tab in tabs" :key="tab.id" :class="{ active: activeTab === tab.id }" @click="activeTab = tab.id"><component :is="tab.icon" :size="17" /><span>{{ tab.label }}</span><ChevronRight :size="14" /></button>
      </nav>
      <div class="scope-note"><span>当前权限范围</span><b>{{ isSystemAdmin ? '全局系统' : '授权实验室资源' }}</b></div>
    </aside>

    <section class="admin-content">
      <div class="admin-topline"><div><span class="eyebrow">ADMINISTRATION</span><h1>{{ tabs.find(item => item.id === activeTab)?.label }}</h1></div><button class="icon-action" :disabled="loading" title="刷新管理数据" @click="notice = ''; loadAll()"><RefreshCw :size="18" /></button></div>
      <div v-if="notice" class="admin-notice" :class="{ error }" role="status" aria-live="polite"><AlertTriangle v-if="error" :size="18" /><Check v-else :size="18" /><span><b>{{ error ? '操作未完成' : '操作成功' }}</b><small>{{ notice }}</small></span><button title="关闭提示" @click="notice = ''"><X :size="15" /></button></div>

      <template v-if="activeTab === 'overview'">
        <div class="metric-strip" v-if="isSystemAdmin"><div><span>用户总数</span><strong>{{ users.length }}</strong><small>{{ activeUsers }} 个正常账号</small></div><div><span>角色数量</span><strong>{{ roles.length }}</strong><small>基于角色的权限控制</small></div><div><span>系统配置</span><strong>{{ configs.length }}</strong><small>当前参数项</small></div><div><span>管理员日志</span><strong>{{ operationLogs.length }}</strong><small>最近操作记录</small></div></div>
        <div class="metric-strip metric-strip-links" v-else>
          <button type="button" @click="activeTab = 'assets'"><span>资产总数</span><strong>{{ assets.length }}</strong><small>{{ highValueAssets }} 件贵重资产</small></button>
          <button type="button" @click="activeTab = 'maintenance'"><span>待处理报修</span><strong>{{ openMaintenance }}</strong><small>尚未关闭的维修工单</small></button>
          <button type="button" @click="activeTab = 'violations'"><span>待处理违约</span><strong>{{ violations.filter((item) => item.status === 'OPEN').length }}</strong><small>未签到待确认或撤销</small></button>
          <button type="button" @click="activeTab = 'bookings'"><span>待审批预约</span><strong>{{ approvals.length || pendingBookings }}</strong><small>需要管理员关注</small></button>
        </div>
        <div class="overview-body">
        <section v-if="isSystemAdmin" class="admin-section"><div class="section-title"><div><h2>账号状态概览</h2><p>快速识别禁用和锁定账号</p></div></div>
          <div class="compact-list"><div v-for="item in users.slice(0, 6)" :key="item.id"><span class="avatar">{{ item.realName?.slice(0, 1) }}</span><div><b>{{ item.realName }}</b><small>{{ item.username }} · {{ item.roles.map(roleName).join(' / ') }}</small></div><span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span></div></div>
        </section>
        <template v-if="!isSystemAdmin">
          <section class="report-grid report-board">
            <article class="admin-section report-card"><div class="report-card-head"><div><h2>资源使用排行</h2><small>{{ stats?.utilization?.length ? '按有效使用率统计，已排除维护关闭与非开放时间' : '按预约次数统计当前授权资源' }}</small></div><BarChart3 :size="18" /></div><div class="report-bars"><div v-for="item in labReport.resourceRanking" :key="item.name" class="report-bar-row"><span :title="item.name">{{ item.name }}</span><div class="report-bar-track"><i :style="{ width: `${Math.max(8, item.ratePercent != null ? item.ratePercent : item.count / labReport.maxResourceCount * 100)}%` }"></i></div><b>{{ item.ratePercent != null ? formatPercent(item.ratePercent) : item.count }}</b></div><div v-if="!labReport.resourceRanking.length" class="admin-empty">暂无预约数据</div></div></article>
            <article class="admin-section report-card report-pie-card"><div class="report-card-head"><div><h2>预约状态分布</h2><small>当前管理范围内的全部预约</small></div><CalendarClock :size="18" /></div>
              <div v-if="labReport.statusPie.length" class="report-pie">
                <svg viewBox="0 0 160 160" role="img" :aria-label="`预约共 ${labReport.total} 条`">
                  <path v-for="slice in labReport.statusPie" :key="slice.key" :d="slice.path" :fill="slice.color"></path>
                  <text x="80" y="76" text-anchor="middle" fill="#24483b" font-size="22" font-weight="700">{{ labReport.total }}</text>
                  <text x="80" y="94" text-anchor="middle" fill="#82918a" font-size="10">预约</text>
                </svg>
                <div class="report-pie-legend">
                  <div v-for="slice in labReport.statusPie" :key="slice.key"><i :style="{ background: slice.color }"></i><span>{{ slice.label }}</span><strong>{{ slice.count }}</strong><em>{{ slice.percent }}%</em></div>
                </div>
              </div>
              <div v-else class="admin-empty">暂无预约数据</div>
            </article>
            <article class="admin-section report-card"><div class="report-card-head"><div><h2>用户违约次数</h2><small>未签到产生的违约记录</small></div><AlertTriangle :size="18" /></div>
              <div class="report-bars">
                <div v-for="item in labReport.userViolations" :key="item.userId" class="report-bar-row"><span :title="item.name">{{ item.name }}</span><div class="report-bar-track"><i :style="{ width: `${Math.max(8, item.count / labReport.maxViolationCount * 100)}%` }"></i></div><b>{{ item.count }}</b></div>
                <div v-if="!labReport.userViolations.length" class="admin-empty">暂无违约统计</div>
              </div>
            </article>
          </section>
          <section class="admin-section report-card report-upcoming"><div class="report-card-head"><div><h2>近期预约</h2><small>按开始时间排列，便于安排实验室值守</small></div><button class="quiet" @click="activeTab = 'bookings'">查看全部 <ChevronRight :size="14" /></button></div><div class="data-table"><div class="data-row report-booking-row table-head"><span>预约编号</span><span>申请人</span><span>资源</span><span>开始时间</span><span>状态</span></div><div v-for="item in labReport.upcoming" :key="item.id" class="data-row report-booking-row"><span class="mono">{{ item.bookingNo }}</span><span>{{ item.applicantNameSnapshot || `用户 ${item.userId}` }}</span><span>{{ item.resourceNameSnapshot }}</span><span>{{ formatTime(item.startTime) }}</span><span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span></div><div v-if="!labReport.upcoming.length" class="admin-empty">暂无即将开始的预约</div></div></section>
        </template>
        </div>
      </template>

      <template v-else-if="activeTab === 'users'">
        <div class="toolbar"><label class="search"><Search :size="16" /><input v-model="query" placeholder="搜索姓名、账号或邮箱" /></label><span>{{ displayUsers.length }} 位用户</span></div>
        <section class="admin-section data-table"><div class="data-row user-grid table-head"><span>用户</span><span>联系方式</span><span>角色</span><span>状态</span><span>操作</span></div><div v-for="item in displayUsers" :key="item.id" class="data-row user-grid"><span class="user-cell"><i class="avatar">{{ item.realName?.slice(0, 1) }}</i><span><b>{{ item.realName }}</b><small>{{ item.username }}</small></span></span><span><b>{{ item.email || '-' }}</b><small>{{ item.phone || '-' }}</small></span><span class="role-tags"><i v-for="role in item.roles" :key="role">{{ roleName(role) }}</i></span><span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span><span class="row-actions"><button @click="editUser(item)">角色与密码</button><button :class="{ danger: item.status === 'ACTIVE' }" @click="updateUserStatus(item)">{{ item.status === 'ACTIVE' ? '禁用' : '启用' }}</button><button class="danger" :disabled="Number(item.id) === Number(user.id)" @click="deleteUser(item)">删除</button></span></div></section>
      </template>

      <template v-else-if="activeTab === 'configs'">
        <section class="admin-section"><div class="section-title"><div><h2>业务参数</h2><p>保存后由各服务按配置键读取，敏感修改仅系统管理员可执行</p></div></div><div class="config-list"><label v-for="item in configs" :key="item.key"><span><b>{{ item.key }}</b><small>{{ item.type }}</small></span><input v-model="item.value" /><button title="保存配置" @click="saveConfig(item)"><Save :size="17" /></button></label><div v-if="!configs.length" class="admin-empty">暂无系统配置</div></div></section>
      </template>

      <template v-else-if="activeTab === 'logs'">
        <div class="toolbar log-toolbar">
          <label class="search"><Search :size="16" /><input v-model="logQuery" placeholder="搜索管理员、对象、请求号或 IP" /></label>
          <div class="log-filters">
            <select v-model="logTypeFilter"><option value="">全部操作</option><option v-for="type in operationTypes" :key="type" :value="type">{{ operationName(type) }}</option></select>
            <select v-model="logResultFilter"><option value="">全部结果</option><option value="SUCCESS">成功</option><option value="FAILED">失败</option></select>
            <span>{{ displayOperationLogs.length }} 条记录</span>
          </div>
        </div>
        <section class="admin-section data-table operation-log-table">
          <div class="data-row log-grid table-head"><span>操作时间</span><span>管理员</span><span>操作</span><span>目标与摘要</span><span>结果</span><span>请求信息</span></div>
          <div v-for="item in displayOperationLogs" :key="item.id" class="data-row log-grid">
            <span>{{ formatTime(item.createdAt) }}</span>
            <span><b>{{ operationOperator(item) }}</b><small>ID {{ item.operatorId }}</small></span>
            <span><b>{{ operationName(item.operationType) }}</b><small>{{ item.operationType }}</small></span>
            <span><b>{{ operationTarget(item) }}</b><small>{{ operationDetail(item) }}</small></span>
            <span class="status" :class="item.result === 'SUCCESS' ? 'active' : 'rejected'">{{ item.result === 'SUCCESS' ? '成功' : '失败' }}</span>
            <span><span class="mono">{{ item.requestId || '-' }}</span><small>{{ item.ip || '-' }}</small></span>
          </div>
          <div v-if="!displayOperationLogs.length" class="admin-empty">暂无符合条件的管理员操作日志</div>
        </section>
      </template>

      <template v-else-if="activeTab === 'resource-types'">
        <section class="admin-section resource-type-list">
            <div class="section-title"><div><h2>资源类别</h2><p>{{ resourceTypes.length }} 个类别会出现在资源管理表单中。默认审批：一级由教师审，二级再由实验室管理员终审；单个资源可覆盖</p></div><button class="command" type="button" @click="beginCreateResourceType"><Plus :size="16" />新建资源类别</button></div>
            <div class="resource-type-row resource-type-head"><span>类别名称</span><span>默认审批</span><span>状态</span><span>创建时间</span><span>操作</span></div>
            <div v-for="item in resourceTypes" :key="item.id" class="resource-type-row"><span><b>{{ item.name }}</b><small>ID {{ item.id }}</small></span><span>{{ item.defaultApprovalLevel === 0 ? '无需审批' : `L${item.defaultApprovalLevel} · ${item.defaultApprovalLevel === 2 ? '二级审批' : '一级审批'}` }}</span><span class="status" :class="item.enabled === false || item.deleted ? 'disabled' : 'active'">{{ item.enabled === false || item.deleted ? '已停用' : '启用中' }}</span><span>{{ formatTime(item.createdAt) }}</span><span class="resource-type-actions"><button class="quiet" type="button" @click.stop="editResourceType(item)">编辑</button><button class="quiet" type="button" :disabled="resourceTypeActionId === item.id" :class="{ danger: item.enabled !== false }" @click.stop="toggleResourceType(item)">{{ resourceTypeActionId === item.id ? '处理中...' : item.enabled === false ? '启用' : '停用' }}</button><button class="quiet danger" type="button" @click.stop="deleteResourceType(item)">删除</button></span></div>
            <div v-if="!resourceTypes.length" class="admin-empty">暂无资源类别，请先创建一个类别</div>
        </section>
      </template>

      <template v-else-if="activeTab === 'resources'">
        <div class="toolbar"><span>{{ resources.length }} 项可用资源</span><button class="command" @click="selectedResource = null; createResource()"><Plus :size="16" />新增资源</button></div>
        <section class="resource-admin-layout"><div class="admin-section resource-catalog"><button v-for="item in resources" :key="item.id" :class="{ selected: selectedResource?.id === item.id }" @click="manageResource(item)"><span class="resource-symbol"><FlaskConical :size="19" /></span><span><b>{{ item.name }}</b><small>{{ item.location }} · 容量 {{ item.capacity }} 人</small></span><i>{{ statusText(item.status) }}</i></button><div v-if="!resources.length" class="admin-empty">暂无资源</div></div>
          <div class="resource-detail"><section v-if="selectedResource" class="admin-section"><div class="section-title"><div><h2>{{ selectedResource.name }}</h2><p>开放时间与维护排期</p></div><button class="quiet" @click="editResource(selectedResource)">编辑资料</button></div><div class="rules-block"><div class="subhead"><b>每周开放时间</b><button @click="addSchedule"><Plus :size="15" />增加</button></div><div v-for="(row, index) in schedules" :key="index" class="schedule-row"><select v-model.number="row.weekday"><option v-for="n in 7" :key="n" :value="n">周{{ '一二三四五六日'[n-1] }}</option></select><input v-model="row.openTime" type="time" /><span>至</span><input v-model="row.closeTime" type="time" /><input v-model.number="row.slotMinutes" type="number" min="5" step="5" title="时间粒度" /><button class="remove-icon" title="删除时段" @click="schedules.splice(index, 1)"><X :size="15" /></button></div><button class="command save-rules" @click="saveSchedules"><Save :size="16" />保存开放时间</button></div>
            <div class="rules-block"><div class="subhead"><b>资源负责人</b><small>从现有教师中选择。有负责人时由其审批；未配置负责人，或负责人本人申请时，由实验室管理员审批</small></div><div class="manager-form"><select v-model="managerForm.userId"><option value="">请选择教师</option><option v-for="teacher in availableTeachers" :key="teacher.id" :value="teacher.id">{{ teacherLabel(teacher) }}</option></select><button class="command" :disabled="!managerForm.userId || !availableTeachers.length" @click="addManager"><Plus :size="16" />添加</button></div><div v-if="!teachers.length" class="admin-empty">暂无可用教师账号</div><div class="closure-list"><div v-for="item in managers" :key="item.id"><span><b>{{ managerName(item) }}</b><small>资源负责人</small></span><button @click="removeManager(item)">撤销</button></div><div v-if="!managers.length" class="admin-empty">尚未配置资源负责人</div></div></div>
            <div class="rules-block"><div class="subhead"><b>维护与临时关闭</b><small>关闭时间内不能创建预约，已占用该时段的待审/已通过预约会被取消</small></div><div class="closure-form"><input v-model="closureForm.startTime" type="datetime-local" /><input v-model="closureForm.endTime" type="datetime-local" /><input v-model="closureForm.reason" placeholder="维护原因" /><button class="command" @click="createClosure"><Plus :size="16" />添加</button></div><div class="closure-list"><div v-for="item in closures" :key="item.id"><span><b>{{ item.reason }}</b><small>{{ formatTime(item.startTime) }} 至 {{ formatTime(item.endTime) }}</small></span><button @click="cancelClosure(item)">取消</button></div><div v-if="!closures.length" class="admin-empty">当前没有维护安排</div></div></div></section><div v-else class="detail-placeholder"><FlaskConical :size="28" /><b>选择一项资源</b><span>配置开放时间、预约粒度和维护关闭区间</span></div></div></section>
      </template>

      <template v-else-if="activeTab === 'assets'">
        <AssetAdminPanel :assets="assets" :categories="assetCategories" :resources="resources" :loading="loading" @refresh="loadAll" />
      </template>

      <template v-else-if="activeTab === 'maintenance'">
        <MaintenanceAdminPanel :tickets="maintenanceTickets" :assets="assets" :resources="resources" :loading="loading" @refresh="loadAll" />
      </template>

      <template v-else-if="activeTab === 'bookings'">
        <div class="booking-split">
          <section class="admin-section">
            <div class="section-title"><div><h2>预约审批</h2><p>一级教师审，二级实验室管理员终审</p></div><span>{{ approvals.length }} 项待处理</span></div>
            <div class="approval-admin-list">
              <article v-for="item in approvals" :key="item.id">
                <span class="approval-level">L{{ item.level }}</span>
                <div class="approval-applicant"><b>{{ item.applicantName || `用户 ${item.applicantUserId}` }}</b><span>申请人</span><small>预约 #{{ item.bookingId }}</small></div>
                <div class="approval-schedule"><b>{{ item.resourceName || `资源 #${item.resourceId}` }}</b><span>{{ formatTime(item.startTime) }} 至 {{ formatTime(item.endTime) }}</span><small>审批截止 {{ formatTime(item.deadline) }}</small></div>
                <div class="approval-buttons"><button class="reject" @click="openRejection(item)"><X :size="15" />驳回</button><button class="accept" @click="approveTask(item)"><Check :size="15" />通过</button></div>
              </article>
              <div v-if="!approvals.length" class="admin-empty">暂无待审批任务</div>
            </div>
          </section>
          <section class="admin-section compact-booking-panel">
            <div class="section-title"><div><h2>近期预约</h2><p>只看未结束和最近安排</p></div><span>{{ compactBookings.length }} 条</span></div>
            <div class="compact-booking-list">
              <article v-for="item in compactBookings" :key="item.id">
                <div><b>{{ item.applicantNameSnapshot || `用户 ${item.userId}` }}</b><small>{{ item.resourceNameSnapshot }} · {{ formatTime(item.startTime) }}</small></div>
                <span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span>
              </article>
              <div v-if="!compactBookings.length" class="admin-empty">暂无预约记录</div>
            </div>
          </section>
        </div>
      </template>

      <template v-else-if="activeTab === 'violations'">
        <section class="admin-section data-table">
          <div class="data-row violation-grid table-head"><span>申请人</span><span>预约</span><span>类型</span><span>发生时间</span><span>状态</span><span>处理</span></div>
          <div v-for="item in violations" :key="item.id" class="data-row violation-grid">
            <span><b>{{ item.applicantName || `用户 ${item.userId}` }}</b><small>用户 {{ item.userId }}</small></span>
            <span><b>{{ item.resourceName || '-' }}</b><small>{{ item.bookingNo || `预约 #${item.bookingId}` }} · {{ formatTime(item.startTime) }}</small></span>
            <span>{{ violationTypeText(item.violationType) }}</span>
            <span>{{ formatTime(item.createdAt) }}</span>
            <span><span class="status" :class="item.status.toLowerCase()">{{ statusText(item.status) }}</span><small v-if="item.comment">{{ item.comment }}</small></span>
            <span class="row-actions" v-if="item.status === 'OPEN'">
              <button class="danger" type="button" @click="confirmViolation(item)">确认违约</button>
              <button type="button" @click="openDismiss(item)">撤销误判</button>
            </span>
            <span v-else>{{ formatTime(item.processedAt) }}</span>
          </div>
          <div v-if="!violations.length" class="admin-empty">暂无违约记录</div>
        </section>
      </template>
    </section>

    <div v-if="selectedUser" class="admin-modal-bg" @click.self="selectedUser = null"><section class="admin-modal"><div class="modal-title"><div><h2>{{ selectedUser.realName }}</h2><p>{{ selectedUser.username }} 的角色与访问凭据</p></div><button class="modal-close" title="关闭" aria-label="关闭用户窗口" @click="selectedUser = null"><X :size="22" /></button></div><div class="modal-body"><fieldset><legend>分配角色</legend><label v-for="role in roles" :key="role.code"><input v-model="selectedRoles" type="checkbox" :value="role.code" /><span>{{ roleName(role.code) }}</span></label></fieldset><button class="command full-command" @click="saveUserRoles"><Save :size="16" />保存角色</button><div class="password-reset"><label>重置密码<input v-model="resetPassword" type="password" placeholder="输入至少 8 位新密码" /></label><button @click="saveResetPassword">确认重置</button></div></div></section></div>
    <div v-if="resourceTypeDialog" class="admin-modal-bg" @click.self="resourceTypeDialog = false"><section class="admin-modal resource-type-modal"><div class="modal-title"><div><h2>{{ editingResourceType ? '编辑资源类别' : '新增资源类别' }}</h2><p>{{ editingResourceType ? '修改类别名称、默认审批级别和启用状态' : '填写类别名称并设置默认审批级别' }}</p></div><button class="modal-close" title="关闭" aria-label="关闭资源类别窗口" @click="resourceTypeDialog = false"><X :size="22" /></button></div><form class="modal-body resource-type-modal-form" @submit.prevent="saveResourceType"><label>类别名称<input v-model="resourceTypeEditForm.name" maxlength="100" /></label><label>默认审批级别<select v-model.number="resourceTypeEditForm.defaultApprovalLevel"><option :value="0">无需审批</option><option :value="1">一级审批</option><option :value="2">二级审批</option></select></label><label>类别状态<select v-model="resourceTypeEditForm.enabled"><option :value="true">启用中</option><option :value="false">已停用</option></select></label><div class="modal-actions"><button class="quiet" type="button" @click="resourceTypeDialog = false">取消</button><button class="command" type="submit"><Save :size="16" />{{ editingResourceType ? '保存修改' : '创建类别' }}</button></div></form></section></div>
    <div v-if="resourceDialog" class="admin-modal-bg" @click.self="resourceDialog = false"><section class="admin-modal resource-form-modal"><div class="modal-title"><div><h2>{{ selectedResource ? '编辑资源' : '新增资源' }}</h2><p>维护资源基础资料与预约规则</p></div><button class="modal-close" title="关闭" aria-label="关闭资源窗口" @click="resourceDialog = false"><X :size="22" /></button></div><div class="modal-body form-columns"><label>资源类型<select v-model.number="resourceForm.typeId"><option value="" disabled>请选择</option><option v-for="type in resourceTypes" :key="type.id" :value="type.id">{{ type.name }}</option></select></label><label>资源名称<input v-model="resourceForm.name" /></label><label>位置<input v-model="resourceForm.location" /></label><label>容量<input v-model.number="resourceForm.capacity" type="number" min="1" /></label><label>最大预约时长（分钟）<input v-model.number="resourceForm.maxDurationMinutes" type="number" min="1" /></label><label class="check-field"><input v-model="resourceForm.needCheckin" type="checkbox" />需要签到</label><label>审批级别<select v-model="resourceForm.approvalLevelOverride"><option value="">跟随类别</option><option :value="0">无需审批</option><option :value="1">一级审批</option><option :value="2">二级审批</option></select></label><label class="wide-field">图片地址<input v-model.trim="resourceForm.imageUrl" maxlength="500" placeholder="请输入可公开访问的图片 URL" /></label><div v-if="resourceForm.imageUrl" class="resource-image-preview wide-field"><img :src="resourceForm.imageUrl" alt="资源图片预览" /></div><label class="wide-field">描述<textarea v-model="resourceForm.description"></textarea></label><button class="command full-command wide-field" @click="saveResource"><Save :size="16" />保存资源</button></div></section></div>
    <div v-if="rejectionTarget" class="approval-reject-backdrop" @click.self="closeRejection"><section class="approval-reject-modal" role="dialog" aria-modal="true" aria-labelledby="admin-reject-title"><header class="approval-reject-head"><div><h2 id="admin-reject-title">驳回预约</h2><p>填写原因后，申请人可在预约记录中查看</p></div><button class="modal-close" type="button" title="关闭" aria-label="关闭驳回窗口" :disabled="rejectionSaving" @click="closeRejection"><X :size="22" /></button></header><div class="approval-reject-body"><div class="approval-reject-context"><b>{{ rejectionTarget.resourceName || `预约 #${rejectionTarget.bookingId}` }}</b><span>申请人：{{ rejectionTarget.applicantName || `用户 ${rejectionTarget.applicantUserId}` }}</span><small>{{ formatTime(rejectionTarget.startTime) }} 至 {{ formatTime(rejectionTarget.endTime) }}</small></div><label class="approval-reject-field"><span>驳回原因 <em>必填</em></span><textarea v-model="rejectionReason" maxlength="500" autofocus placeholder="请说明驳回原因，便于申请人修改后重新提交"></textarea><small>{{ rejectionReason.length }} / 500</small></label></div><footer class="approval-reject-actions"><button class="approval-reject-cancel" type="button" :disabled="rejectionSaving" @click="closeRejection">取消</button><button class="approval-reject-confirm" type="button" :disabled="!rejectionReason.trim() || rejectionSaving" @click="submitRejection">{{ rejectionSaving ? '提交中...' : '确认驳回' }}</button></footer></section></div>
    <div v-if="dismissTarget" class="approval-reject-backdrop" @click.self="closeDismiss"><section class="approval-reject-modal" role="dialog" aria-modal="true" aria-labelledby="dismiss-title"><header class="approval-reject-head"><div><h2 id="dismiss-title">撤销违约</h2><p>误判未签到时撤销后，不再计入限制次数</p></div><button class="modal-close" type="button" title="关闭" aria-label="关闭撤销窗口" :disabled="dismissSaving" @click="closeDismiss"><X :size="22" /></button></header><div class="approval-reject-body"><div class="approval-reject-context"><b>{{ dismissTarget.resourceName || dismissTarget.bookingNo || `预约 #${dismissTarget.bookingId}` }}</b><span>申请人：{{ dismissTarget.applicantName || `用户 ${dismissTarget.userId}` }}</span><small>{{ formatTime(dismissTarget.startTime) }}</small></div><label class="approval-reject-field"><span>撤销原因 <em>必填</em></span><textarea v-model="dismissReason" maxlength="500" autofocus placeholder="请说明撤销原因，例如申请人已现场使用或系统未及时签到"></textarea><small>{{ dismissReason.length }} / 500</small></label></div><footer class="approval-reject-actions"><button class="approval-reject-cancel" type="button" :disabled="dismissSaving" @click="closeDismiss">取消</button><button class="approval-reject-confirm" type="button" :disabled="!dismissReason.trim() || dismissSaving" @click="submitDismiss">{{ dismissSaving ? '提交中...' : '确认撤销' }}</button></footer></section></div>
  </main>
</template>

<style scoped>
.flow-form{padding:18px 20px;display:grid;grid-template-columns:1.2fr 1fr 1fr auto;gap:10px;align-items:end}.flow-form label{display:flex;flex-direction:column;gap:6px;font-size:11px;color:#65766e}.flow-form input,.flow-form select{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff}.flow-history{margin-top:14px}.flow-grid{grid-template-columns:1.2fr .5fr 1.8fr .8fr 1.2fr}
.manager-form{display:grid;grid-template-columns:1fr auto;gap:7px;margin-bottom:12px}.manager-form input,.manager-form select{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}
.report-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:14px}.report-board{grid-template-columns:1fr 1fr;align-items:stretch}.report-board .report-pie-card{grid-column:2;grid-row:1 / span 2}.report-card{padding:0}.stats-summary{padding:14px 20px 0}.stats-summary span,.stats-summary small{display:block;color:#839089;font-size:10px}.stats-summary strong{display:block;font-size:28px;margin:6px 0 4px}.report-card-head{display:flex;align-items:flex-start;justify-content:space-between;padding:17px 20px;border-bottom:1px solid #e8eeeb}.report-card-head h2{font-size:14px;margin:0 0 4px}.report-card-head small{font-size:10px;color:#839089}.report-card-head>svg{color:#4b7c68}.report-bars{padding:13px 20px 17px}.report-bar-row{display:grid;grid-template-columns:minmax(90px,1.1fr) 2fr 28px;gap:9px;align-items:center;min-height:34px;font-size:11px}.report-bar-row>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.report-bar-row>b{text-align:right;color:#356c58}.report-bar-track{height:7px;background:#edf2ef;border-radius:4px;overflow:hidden}.report-bar-track i{display:block;height:100%;background:#65a184;border-radius:4px}.report-status-list{padding:9px 20px 13px}.report-status-list>div{display:grid;grid-template-columns:1fr 40px 42px;align-items:center;min-height:33px;border-bottom:1px solid #edf1ef}.report-status-list>div:last-child{border-bottom:0}.report-status-list strong{text-align:right;font-size:15px}.report-status-list em{text-align:right;color:#82918a;font-size:10px;font-style:normal}.metric-strip-links>button{display:block;width:100%;margin:0;padding:20px;border:0;border-right:1px solid #e6ece9;border-radius:0;background:#fff;text-align:left;color:inherit}.metric-strip-links>button:last-child{border-right:0}.metric-strip-links>button:hover{background:#edf6f1}.metric-strip-links>button:focus-visible{outline:2px solid #4d9a70;outline-offset:-2px}
.report-pie-card{display:flex;flex-direction:column;min-width:0;height:100%}.report-pie{flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:22px;padding:22px 24px 26px;min-height:0}.report-pie svg{width:min(260px,72%);max-width:280px;height:auto;aspect-ratio:1}.report-pie-legend{width:100%;max-width:300px;display:grid;gap:10px}.report-pie-legend>div{display:grid;grid-template-columns:10px minmax(0,1fr) 28px 36px;gap:8px;align-items:center;font-size:12px}.report-pie-legend i{width:10px;height:10px;border-radius:50%}.report-pie-legend span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.report-pie-legend strong{text-align:right}.report-pie-legend em{text-align:right;color:#82918a;font-size:10px;font-style:normal}
.report-upcoming{margin-top:14px}.report-upcoming .quiet{display:inline-flex;align-items:center;gap:2px;padding:5px 8px;font-size:10px}.report-booking-row{grid-template-columns:1fr 1fr 1.3fr 1.3fr .7fr;min-width:680px}
.admin-shell{min-height:calc(100vh - 76px);display:grid;grid-template-columns:230px minmax(0,1fr);background:#f5f7f6;color:#20332b}.admin-sidebar{background:#183b33;color:#dce9e3;padding:26px 14px;display:flex;flex-direction:column;min-height:calc(100vh - 76px)}.workspace-mark{display:flex;gap:11px;align-items:center;padding:0 10px 26px;border-bottom:1px solid #31534b}.workspace-mark svg{color:#b7dd55}.workspace-mark b,.workspace-mark small{display:block}.workspace-mark b{font-size:14px}.workspace-mark small{color:#89a79d;font-size:11px;margin-top:4px}.admin-sidebar nav{display:grid;gap:4px;margin-top:20px}.admin-sidebar nav button{height:42px;border:0;border-radius:5px;background:transparent;color:#aac0b7;display:grid;grid-template-columns:24px 1fr 15px;align-items:center;text-align:left;padding:0 10px}.admin-sidebar nav button:hover,.admin-sidebar nav button.active{background:#285148;color:#fff}.admin-sidebar nav button.active svg:first-child{color:#c9e962}.scope-note{margin-top:auto;border-top:1px solid #31534b;padding:18px 10px 0}.scope-note span,.scope-note b{display:block}.scope-note span{font-size:10px;color:#75948a}.scope-note b{font-size:12px;margin-top:5px}.admin-content{padding:34px clamp(24px,4vw,58px);min-width:0}.admin-topline{height:66px;display:flex;align-items:flex-start;justify-content:space-between}.admin-topline .eyebrow{display:block;margin:0 0 5px;letter-spacing:1px;font-size:10px;color:#49816f}.admin-topline h1{font-size:25px;margin:0}.icon-action{width:38px;height:38px;border:1px solid #d7e1dc;border-radius:6px;background:#fff;color:#597068;display:grid;place-items:center}.admin-notice{min-height:38px;padding:0 12px;margin:4px 0 16px;background:#e8f5ed;color:#347458;display:flex;align-items:center;gap:8px;border-left:3px solid #4d9a70;font-size:12px}.admin-notice.error{background:#faece9;color:#a24d42;border-color:#bd655a}.admin-notice button{margin-left:auto;border:0;background:transparent;color:inherit}.metric-strip{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #dfe7e3;background:#fff;margin-bottom:18px}.metric-strip>div{padding:20px;border-right:1px solid #e6ece9}.metric-strip>div:last-child{border:0}.metric-strip span,.metric-strip small,.compact-list small,.data-row small{display:block;color:#7b8b84;font-size:11px}.metric-strip strong{display:block;font-size:27px;margin:7px 0 3px}.admin-section{background:#fff;border:1px solid #dfe7e3;border-radius:6px;overflow:hidden}.section-title{padding:18px 20px;border-bottom:1px solid #e8eeeb;display:flex;align-items:center;justify-content:space-between}.section-title h2{font-size:15px;margin:0 0 4px}.section-title p{margin:0;color:#839089;font-size:11px}.compact-list{padding:5px 20px}.compact-list>div{display:flex;align-items:center;gap:12px;padding:12px 0;border-bottom:1px solid #edf1ef}.compact-list>div>div{flex:1}.compact-list b{font-size:13px}.avatar{width:32px;height:32px;border-radius:50%;background:#dcece4;color:#356d59;display:grid;place-items:center;font-style:normal;font-size:12px;flex:0 0 auto}.status{font-size:10px;padding:4px 7px;border-radius:3px;background:#edf1ef;color:#607168;width:max-content}.status.active,.status.approved,.status.checked_in,.status.resolved,.status.lifted{background:#e4f3e9;color:#347658}.status.disabled,.status.locked,.status.rejected,.status.canceled,.status.dismissed,.status.confirmed,.status.no_show{background:#f3e9e7;color:#9a5a51}.status.pending_approval,.status.open{background:#fff0d8;color:#8f6a2f}.toolbar{height:50px;display:flex;align-items:center;justify-content:space-between;margin-bottom:12px;color:#74847d;font-size:12px}.search{width:min(390px,60%);height:38px;background:#fff;border:1px solid #dce5e0;display:flex;align-items:center;gap:8px;padding:0 11px;border-radius:5px}.search input{border:0;outline:0;flex:1}.command{height:36px;border:0;border-radius:5px;background:#225c4d;color:#fff;padding:0 13px;display:inline-flex;align-items:center;justify-content:center;gap:7px}.quiet{border:1px solid #d7e2dc;background:#fff;border-radius:4px;padding:7px 10px;color:#42695a}.data-table{overflow:auto}.data-row{display:grid;grid-template-columns:1.1fr 1fr 1.2fr 1.3fr .7fr;gap:16px;align-items:center;min-height:52px;padding:0 18px;border-bottom:1px solid #edf1ef;font-size:12px;min-width:760px}.data-row.table-head{min-height:38px;background:#fafbfa;color:#839089;font-size:10px}.user-grid{grid-template-columns:1.2fr 1fr 1.3fr .55fr 1.35fr}.stats-grid{grid-template-columns:1fr 1fr 2fr .7fr 1fr}.booking-grid{grid-template-columns:1.1fr 1fr 1.2fr 1.7fr .4fr .7fr}.violation-grid{grid-template-columns:.5fr 1fr .8fr 1.2fr .7fr 1.2fr}.user-cell{display:flex;align-items:center;gap:9px}.user-cell b,.data-row b{display:block;font-size:12px}.role-tags{display:flex;flex-wrap:wrap;gap:4px}.role-tags i{font-style:normal;font-size:9px;background:#eaf0ed;color:#46675b;padding:4px 6px;border-radius:3px}.row-actions{display:flex;gap:5px}.row-actions button,.closure-list button{border:1px solid #d4e1da;background:#fff;color:#3d6c5a;border-radius:4px;padding:6px 8px;font-size:10px}.row-actions button.danger{color:#a45149;border-color:#ead1cc}.admin-empty{padding:30px;text-align:center;color:#8d9a94;font-size:12px}.config-list{padding:5px 20px}.config-list label{min-height:58px;display:grid;grid-template-columns:1fr minmax(180px,320px) 38px;gap:10px;align-items:center;border-bottom:1px solid #edf1ef}.config-list label span b,.config-list label span small{display:block}.config-list input,.schedule-row input,.schedule-row select,.closure-form input,.form-columns input,.form-columns select,.form-columns textarea,.password-reset input{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;color:#243b31;background:#fff}.config-list button{width:36px;height:36px;border:0;background:#e7f0eb;color:#356a56;border-radius:4px;display:grid;place-items:center}.resource-admin-layout{display:grid;grid-template-columns:320px minmax(0,1fr);gap:14px;align-items:start}.resource-catalog{padding:6px}.resource-catalog>button{width:100%;min-height:61px;border:0;border-bottom:1px solid #edf1ef;background:#fff;display:grid;grid-template-columns:38px 1fr auto;align-items:center;text-align:left;gap:10px;padding:8px}.resource-catalog>button:hover,.resource-catalog>button.selected{background:#edf6f1}.resource-catalog b,.resource-catalog small{display:block}.resource-catalog b{font-size:12px}.resource-catalog small{font-size:10px;color:#82918a;margin-top:4px}.resource-catalog i{font-size:9px;color:#3b7b63;font-style:normal}.resource-symbol{width:34px;height:34px;border-radius:5px;background:#e3eee8;color:#477966;display:grid;place-items:center}.detail-placeholder{min-height:360px;border:1px dashed #cfdad4;display:flex;flex-direction:column;align-items:center;justify-content:center;color:#779087;gap:8px}.detail-placeholder b{font-size:14px}.detail-placeholder span{font-size:11px}.rules-block{padding:17px 20px;border-bottom:1px solid #e9efec}.subhead{display:flex;align-items:center;justify-content:space-between;margin-bottom:12px}.subhead b{font-size:12px}.subhead small{font-size:10px;color:#839089}.subhead button{border:0;background:transparent;color:#39735d;display:flex;gap:4px;align-items:center}.schedule-row{display:grid;grid-template-columns:72px 1fr 18px 1fr 70px 30px;gap:7px;align-items:center;margin-bottom:7px}.schedule-row span{text-align:center;font-size:10px}.remove-icon{height:30px;border:0;background:transparent;color:#9a625b}.save-rules{margin-top:8px}.closure-form{display:grid;grid-template-columns:1fr 1fr 1fr auto;gap:7px}.closure-list>div{display:flex;align-items:center;justify-content:space-between;padding:10px 0;border-bottom:1px solid #edf1ef}.closure-list b,.closure-list small{display:block;font-size:11px}.closure-list small{color:#82918a;margin-top:3px}.approval-admin-list{padding:8px 20px}.approval-admin-list article{display:flex;gap:14px;align-items:center;padding:16px 0;border-bottom:1px solid #edf1ef}.approval-level{width:34px;height:34px;background:#e3eee8;color:#376c58;display:grid;place-items:center;border-radius:4px;font-weight:700}.approval-admin-list article>div:nth-child(2){flex:1}.approval-admin-list b,.approval-admin-list span,.approval-admin-list small{display:block}.approval-admin-list b{font-size:13px}.approval-admin-list span,.approval-admin-list small{font-size:10px;color:#7c8c85;margin-top:3px}.approval-buttons{display:flex;gap:7px}.approval-buttons button{height:32px;border-radius:4px;padding:0 10px;display:flex;gap:5px;align-items:center}.approval-buttons .accept{border:0;background:#225c4d;color:#fff}.approval-buttons .reject{border:1px solid #e3cfcb;background:#fff;color:#9a5149}.admin-modal-bg{position:fixed;z-index:50;inset:0;background:rgba(18,31,26,.48);display:grid;place-items:center;padding:20px}.admin-modal{width:min(520px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:7px}.modal-title{padding:19px 21px;border-bottom:1px solid #e6ece9;display:flex;justify-content:space-between}.modal-title h2{font-size:17px;margin:0 0 4px}.modal-title p{font-size:11px;color:#82918a;margin:0}.modal-title button{border:0;background:transparent}.modal-body{padding:20px}.modal-body fieldset{border:0;padding:0;margin:0 0 15px}.modal-body legend{font-size:11px;color:#6f8078;margin-bottom:9px}.modal-body fieldset label{display:inline-flex;gap:6px;align-items:center;margin:0 15px 9px 0;font-size:12px}.full-command{width:100%}.password-reset{margin-top:20px;padding-top:17px;border-top:1px solid #e9eeec;display:grid;grid-template-columns:1fr auto;align-items:end;gap:8px}.password-reset label{display:flex;flex-direction:column;gap:6px;font-size:11px}.password-reset button{height:36px;border:1px solid #decac6;background:#fff;color:#965048;border-radius:4px}.resource-form-modal{width:min(650px,100%)}.form-columns{display:grid;grid-template-columns:1fr 1fr;gap:13px}.form-columns label{display:flex;flex-direction:column;gap:6px;font-size:11px}.form-columns textarea{height:72px;padding:8px;resize:vertical}.form-columns .check-field{flex-direction:row;align-items:center}.check-field input{height:auto}.wide-field{grid-column:1/-1}.mono{font-family:ui-monospace,monospace}.eyebrow{letter-spacing:1px}.admin-content button{cursor:pointer}.admin-content button:disabled{opacity:.5;cursor:not-allowed}
@media(max-width:650px){.report-pie{grid-template-columns:1fr;justify-items:center}.report-pie-legend{width:100%}}
@media(max-width:950px){.admin-shell{grid-template-columns:72px minmax(0,1fr)}.admin-sidebar{padding:22px 8px}.workspace-mark div,.admin-sidebar nav span,.admin-sidebar nav svg:last-child,.scope-note{display:none}.workspace-mark{justify-content:center;padding:0 0 22px}.admin-sidebar nav button{display:grid;grid-template-columns:1fr;place-items:center;padding:0}.metric-strip{grid-template-columns:1fr 1fr}.resource-admin-layout{grid-template-columns:1fr}.resource-catalog{display:grid;grid-template-columns:1fr 1fr}.closure-form{grid-template-columns:1fr 1fr}.closure-form input:nth-child(3){grid-column:1/-1}.manager-form{grid-template-columns:1fr auto}.report-grid,.report-board{grid-template-columns:1fr}.report-board .report-pie-card{grid-column:auto;grid-row:auto}}
@media(max-width:650px){.admin-shell{display:block}.admin-sidebar{min-height:auto;padding:10px;position:sticky;top:76px;z-index:5}.workspace-mark,.scope-note{display:none}.admin-sidebar nav{display:flex;overflow:auto;margin:0}.admin-sidebar nav button{min-width:48px}.admin-content{padding:22px 14px}.metric-strip{grid-template-columns:1fr}.metric-strip>div,.metric-strip-links>button{border-right:0;border-bottom:1px solid #e6ece9}.metric-strip-links>button:last-child{border-bottom:0}.resource-catalog{grid-template-columns:1fr}.schedule-row{grid-template-columns:65px 1fr 16px 1fr 28px}.schedule-row input:nth-of-type(3){display:none}.closure-form{grid-template-columns:1fr}.manager-form{grid-template-columns:1fr}.manager-form button{grid-column:auto}.form-columns{grid-template-columns:1fr}.wide-field{grid-column:auto}}
.admin-notice{position:fixed;z-index:100;top:88px;right:24px;width:min(390px,calc(100vw - 32px));min-height:54px;padding:0 14px;gap:10px;border-left-width:4px;box-shadow:0 14px 38px rgba(28,55,44,.2)}.admin-notice>span{min-width:0}.admin-notice b,.admin-notice small{display:block}.admin-notice b{margin-bottom:3px;color:#263b32;font-size:11px}.admin-notice small{font-size:11px;line-height:1.4}@media(max-width:650px){.admin-notice{top:88px;right:12px;width:calc(100vw - 24px)}}
.resource-type-layout{display:grid;grid-template-columns:minmax(260px, .75fr) minmax(0, 1.5fr);gap:14px;align-items:start}.resource-type-form{overflow:hidden}.resource-type-form>.section-title>svg{color:#4b7c68}.resource-type-form form{display:grid;gap:14px;padding:18px 20px 20px}.resource-type-form label{display:flex;flex-direction:column;gap:6px;font-size:11px;color:#65766e}.resource-type-form input,.resource-type-form select{height:38px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}.resource-type-form-actions{display:flex;gap:8px}.resource-type-form-actions .command{flex:1}.resource-type-list{min-width:0}.resource-type-row{display:grid;grid-template-columns:minmax(120px,1.2fr) minmax(95px,.9fr) minmax(75px,.7fr) minmax(110px,1fr) minmax(190px,1.8fr);gap:12px;align-items:center;min-height:58px;padding:0 20px;border-bottom:1px solid #edf1ef;font-size:12px}.resource-type-row.resource-type-head{min-height:38px;background:#fafbfa;color:#839089;font-size:10px}.resource-type-row b,.resource-type-row small{display:block}.resource-type-row small{font-size:10px;color:#82918a;margin-top:3px}.resource-type-actions{display:flex;gap:5px;flex-wrap:wrap}.resource-type-actions .quiet{padding:5px 8px;font-size:10px}.resource-type-actions .danger{color:#a45149;border-color:#ead1cc}.resource-type-row:last-of-type{border-bottom:0}
@media(max-width:950px){.resource-type-layout{grid-template-columns:1fr}.resource-type-row{grid-template-columns:minmax(130px,1.5fr) 1fr .8fr 1.1fr minmax(180px,1.5fr)}}
@media(max-width:650px){.resource-type-row{grid-template-columns:minmax(110px,1fr) 1fr .8fr;gap:10px;padding:0 14px}.resource-type-row>span:nth-child(4),.resource-type-head>span:nth-child(4){display:none}.resource-type-row>span:nth-child(5){grid-column:1/-1;padding:0 0 10px}.resource-type-head>span:nth-child(5){display:none}.resource-type-actions{padding-top:0}}
.overview-body{display:flex;flex-direction:column;gap:0}.resource-type-modal{width:min(480px,100%)}.resource-type-modal-form{display:grid;gap:14px}.resource-type-modal-form label{display:flex;flex-direction:column;gap:6px;font-size:11px;color:#65766e}.resource-type-modal-form input,.resource-type-modal-form select{height:38px;border:1px solid #d8e2dd;border-radius:4px;padding:0 9px;background:#fff;color:#243b31}.modal-actions{display:flex;justify-content:flex-end;gap:8px;padding-top:4px}.modal-actions .command{min-width:110px}
.resource-type-create{display:flex;align-items:center;justify-content:space-between;gap:14px;padding:22px 20px;color:#82918a;font-size:11px}.resource-type-create .command{flex:0 0 auto}
.resource-image-preview{height:150px;border:1px solid #d8e2dd;background:#edf2ef;overflow:hidden}.resource-image-preview img{width:100%;height:100%;display:block;object-fit:cover}
@media(max-width:650px){.resource-type-create{align-items:stretch;flex-direction:column}}
.log-grid{grid-template-columns:1fr 1.25fr 1.1fr 1.65fr .55fr 1.25fr;min-width:980px}.log-toolbar{gap:14px}.log-filters{display:flex;align-items:center;justify-content:flex-end;gap:8px}.log-filters select{height:36px;border:1px solid #d8e2dd;border-radius:4px;padding:0 28px 0 9px;background:#fff;color:#415b51;font-size:11px}.operation-log-table .data-row:not(.table-head){min-height:62px}.operation-log-table .mono{display:block;max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:10px}
@media(max-width:760px){.log-toolbar{height:auto;align-items:stretch;flex-direction:column}.log-toolbar .search{width:100%}.log-filters{justify-content:flex-start;flex-wrap:wrap}}
.status.expired{background:#f3e9e7;color:#9a5a51}
.booking-split{display:grid;grid-template-columns:minmax(0,1.25fr) minmax(260px,.75fr);gap:14px;align-items:start}
.booking-split>.admin-section{min-width:0}
.booking-split .section-title span{color:#74847d;font-size:12px}
.booking-split .approval-admin-list article{grid-template-columns:34px minmax(0,1fr);row-gap:8px}
.booking-split .approval-schedule{grid-column:2;padding-left:0;border-left:0}
.booking-split .approval-buttons{grid-column:2}
.compact-booking-panel .compact-booking-list{max-height:calc(100vh - 240px);overflow:auto}
.compact-booking-list{padding:4px 0}
.compact-booking-list article{display:flex;align-items:center;justify-content:space-between;gap:10px;min-height:52px;padding:8px 16px;border-bottom:1px solid #edf1ef}
.compact-booking-list article:last-child{border-bottom:0}
.compact-booking-list b,.compact-booking-list small{display:block}
.compact-booking-list b{font-size:12px}
.compact-booking-list small{margin-top:3px;color:#82918a;font-size:10px}
@media(max-width:950px){.booking-split{grid-template-columns:1fr}.compact-booking-panel .compact-booking-list{max-height:320px}}
.approval-admin-list article{min-height:78px;display:grid;grid-template-columns:34px minmax(160px,.8fr) minmax(230px,1.2fr) auto;column-gap:16px;align-items:center;padding:10px 0}.approval-admin-list article>div:nth-child(2){flex:initial}.approval-applicant,.approval-schedule{min-width:0}.approval-schedule{padding-left:16px;border-left:1px solid #e5ece8}.approval-admin-list b{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
@media(max-width:650px){.approval-admin-list{padding:8px 14px}.approval-admin-list article{grid-template-columns:34px minmax(0,1fr);row-gap:9px}.approval-schedule{grid-column:2;padding-left:0;border-left:0}.approval-buttons{grid-column:2}.approval-buttons button{flex:1;justify-content:center}}
</style>
