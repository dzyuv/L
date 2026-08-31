<script setup>
import { ref, computed, onBeforeUnmount, onMounted, watch } from "vue";
import axios from "axios";
import AdminDashboard from "./AdminDashboard.vue";
import UserMaintenance from "./UserMaintenance.vue";
import {
  CalendarDays,
  CheckCircle2,
  CircleAlert,
  Clock3,
  ChevronRight,
  ClipboardList,
  LogIn,
  LogOut,
  Plus,
  RefreshCw,
  Search,
  ShieldCheck,
  Users as UsersIcon,
  X,
} from "lucide-vue-next";
const token = ref(localStorage.getItem("token") || "");
const user = ref(null);
const resources = ref([]);
const resourceTypes = ref([]);
const resourceQuery = ref("");
const resourceTypeFilter = ref("");
const onlyAvailableResources = ref(false);
const availabilityByResource = ref({});
const bookings = ref([]);
const currentTime = ref(new Date());
const approvals = ref([]);
const rejectionTarget = ref(null);
const rejectionReason = ref("");
const rejectionSaving = ref(false);
const teacherActiveTab = ref("overview");
const loading = ref(false);
const notice = ref("");
const bookingModalOpen = ref(false);
const selectedSlotKeys = ref([]);
const activeSlotDate = ref("");
const toast = ref({ visible: false, message: "", type: "info" });
let toastTimer;
let currentTimeTimer;
const loginForm = ref({ username: "", password: "" });
const API_ORIGIN = import.meta.env.VITE_API_ORIGIN || "";
const bookingForm = ref({
  resourceId: "",
  resourceName: "",
  startTime: "",
  endTime: "",
  purpose: "课程实验",
  participants: 1,
  capacity: 10,
  slotMinutes: 30,
  maxDurationMinutes: 120,
  needCheckin: true,
  approvalLevel: 1,
});
const teacherTabs = [
  { id: "overview", label: "工作概览", icon: ClipboardList },
  { id: "approvals", label: "预约审批", icon: ShieldCheck },
  { id: "resources", label: "资源预约", icon: CalendarDays },
  { id: "bookings", label: "我的预约", icon: Clock3 },
  { id: "maintenance", label: "故障报修", icon: CircleAlert },
];
const loggedIn = computed(() => !!token.value);
const isTeacher = computed(() => (user.value?.roles || []).includes("TEACHER"));
const isSystemAdmin = computed(() => (user.value?.roles || []).includes("SYSTEM_ADMIN"));
const isLabAdmin = computed(() => (user.value?.roles || []).includes("LAB_ADMIN"));
const isAdmin = computed(() => isSystemAdmin.value || isLabAdmin.value);
const roleLabel = computed(() => isSystemAdmin.value ? "系统管理员" : isLabAdmin.value ? "实验室管理员" : isTeacher.value ? "教师" : "学生");
const resourceBookingCounts = computed(() => bookings.value.reduce((counts, item) => {
  counts[item.resourceId] = (counts[item.resourceId] || 0) + 1;
  return counts;
}, {}));
const resourceTypeMap = computed(() => Object.fromEntries(resourceTypes.value.map((item) => [item.id, item])));
const filteredResources = computed(() => {
  const query = resourceQuery.value.trim().toLowerCase();
  return resources.value.filter((resource) => {
    const matchesQuery = !query || [resource.name, resource.location, resource.description].some((value) => String(value || "").toLowerCase().includes(query));
    const matchesType = !resourceTypeFilter.value || resource.typeId === Number(resourceTypeFilter.value);
    const hasAvailability = resourceHasAvailability(resource);
    return matchesQuery && matchesType && (!onlyAvailableResources.value || hasAvailability);
  });
});
const teacherAvailableResourceCount = computed(() => resources.value.filter(resourceHasAvailability).length);
const teacherPendingBookingCount = computed(() => bookings.value.filter((item) => item.status === "PENDING_APPROVAL").length);
const orderedBookings = computed(() => [...bookings.value].sort((left, right) => {
  const now = currentTime.value.getTime();
  const leftStart = new Date(left.startTime).getTime();
  const rightStart = new Date(right.startTime).getTime();
  const leftEnd = new Date(left.endTime).getTime();
  const rightEnd = new Date(right.endTime).getTime();
  const leftCurrentOrUpcoming = Number.isFinite(leftEnd) && leftEnd >= now;
  const rightCurrentOrUpcoming = Number.isFinite(rightEnd) && rightEnd >= now;
  if (leftCurrentOrUpcoming !== rightCurrentOrUpcoming) return leftCurrentOrUpcoming ? -1 : 1;
  if (leftCurrentOrUpcoming) return leftStart - rightStart;
  return rightStart - leftStart;
}));
const teacherUpcomingBookings = computed(() => bookings.value
  .filter((item) => ["APPROVED", "CHECKED_IN"].includes(item.status) && new Date(item.endTime) > currentTime.value)
  .sort((left, right) => String(left.startTime).localeCompare(String(right.startTime))));
const teacherPageTitle = computed(() => teacherTabs.find((item) => item.id === teacherActiveTab.value)?.label || "教师工作台");
watch(notice, (message) => {
  if (!message) return;
  const success = /成功|已通过|已驳回|已完成|已取消|已退出|签到/.test(message);
  const failed = /失败|错误|失效|不可|不能|无法|驳回|请选择|请填写|必须|暂不|没有开放/.test(message);
  toast.value = { visible: true, message, type: success ? "success" : failed ? "error" : "info" };
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toast.value.visible = false; }, 4500);
});
axios.defaults.baseURL = API_ORIGIN;
axios.interceptors.request.use((c) => {
  if (token.value) c.headers.Authorization = `Bearer ${token.value}`;
  return c;
});
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const isLoginRequest = String(error.config?.url || "").includes("/user/login");
    if (error.response?.status === 401 && !isLoginRequest) {
      token.value = "";
      user.value = null;
      localStorage.removeItem("token");
      notice.value = "登录已失效，请重新登录";
    }
    return Promise.reject(error);
  },
);
function formatDate(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}
function formatDateTime(date) {
  const datePart = formatDate(date);
  const timePart = `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
  return `${datePart}T${timePart}`;
}
function resourceImage(resource) {
  return String(resource?.imageUrl || "").trim();
}
function resourceDisplayName(resource) {
  const value = String(resource?.name || "").trim();
  return value && !/^\?+$/.test(value) ? value : `${resource?.location || "综合"}基础实验室`;
}
function resourceDisplayDescription(resource) {
  const value = String(resource?.description || "").trim();
  return value && !/^\?+$/.test(value) ? value : "适合课程实验、科研实践与设备操作";
}
function bookingResource(booking) {
  return resources.value.find((item) => item.id === booking.resourceId) || { name: booking.resourceNameSnapshot };
}
function resourceKind(resource) {
  const name = String(resource?.name || "");
  if (name.includes("计算机") || name.includes("电脑") || name.includes("工作站")) return "计算设备";
  if (name.includes("电子") || name.includes("测量") || name.includes("示波")) return "电子仪器";
  if (name.includes("材料") || name.includes("显微") || name.includes("分析")) return "材料实验";
  if (name.includes("会议") || name.includes("教室")) return "教学空间";
  return "实验空间";
}
function bookingStatusText(status) {
  return ({ PENDING_APPROVAL: "待审批", APPROVED: "已通过", REJECTED: "已驳回", CANCELED: "已取消", CHECKED_IN: "使用中", COMPLETED: "已完成", NO_SHOW: "未签到", EXPIRED: "审批超时" })[status] || status;
}
function bookingRejectReason(item) {
  return String(item?.rejectReason || "").trim();
}
const CHECKIN_BEFORE_MINUTES = 15;
const CHECKIN_AFTER_MINUTES = 30;
function bookingStartMs(item) {
  const start = new Date(item?.startTime).getTime();
  return Number.isFinite(start) ? start : NaN;
}
function bookingNeedsCheckin(item) {
  if (item?.needCheckinSnapshot) return true;
  return Boolean(bookingResource(item)?.needCheckin);
}
function checkinState(item) {
  if (item?.status !== "APPROVED" || !bookingNeedsCheckin(item)) return "hidden";
  const start = bookingStartMs(item);
  if (!Number.isFinite(start)) return "hidden";
  const now = currentTime.value.getTime();
  if (now >= start - CHECKIN_BEFORE_MINUTES * 60000 && now <= start + CHECKIN_AFTER_MINUTES * 60000) return "ready";
  if (now < start - CHECKIN_BEFORE_MINUTES * 60000) return "waiting";
  return "missed";
}
function canCheckin(item) {
  return checkinState(item) === "ready";
}
function showCheckin(item) {
  return item?.status === "APPROVED" && bookingNeedsCheckin(item);
}
function checkinTitle(item) {
  const state = checkinState(item);
  if (state === "ready") return "点击完成签到";
  if (state === "waiting") return "开始前 15 分钟至开始后 30 分钟可签到";
  if (state === "missed") return "已过签到时间";
  return "";
}
function checkinHintText(item) {
  const state = checkinState(item);
  if (state === "ready") return "请立即签到，超时将记为未到";
  if (state === "waiting") return "开始前 15 分钟至开始后 30 分钟可签到";
  if (state === "missed") return "已过签到时间，超时未签到将记为未到";
  return "";
}
function canCancel(item) {
  if (!["APPROVED", "PENDING_APPROVAL"].includes(item?.status)) return false;
  const start = bookingStartMs(item);
  return Number.isFinite(start) && start > currentTime.value.getTime();
}
function bookingDay(value) {
  return value ? String(value).slice(8, 10) : "--";
}
function bookingMonth(value) {
  if (!value) return "----.--";
  return `${String(value).slice(0, 4)}.${String(value).slice(5, 7)}`;
}
function handleResourceImageError(event) {
  event.currentTarget.style.display = "none";
  event.currentTarget.parentElement.classList.add("resource-image-fallback");
}
function bookableSlot(window, now = new Date()) {
  const openAt = new Date(`${window.date}T${window.openTime}`);
  const closeAt = new Date(`${window.date}T${window.closeTime}`);
  const slotMinutes = Number(window.slotMinutes) || 30;
  const maxDurationMinutes = Number(window.maxDurationMinutes) || bookingForm.value.maxDurationMinutes || 60;
  let startAt = openAt;
  if (formatDate(now) === window.date && now >= openAt) {
    const elapsedSlots = Math.floor((now.getTime() - openAt.getTime()) / 60000 / slotMinutes) + 1;
    startAt = new Date(openAt.getTime() + elapsedSlots * slotMinutes * 60000);
  }
  const durationMinutes = Math.min(slotMinutes, maxDurationMinutes);
  const endAt = new Date(startAt.getTime() + durationMinutes * 60000);
  if (startAt <= now || endAt > closeAt) return null;
  return { ...window, slotMinutes, maxDurationMinutes, startAt, endAt };
}
function applyBookableSlot(window) {
  const slot = bookableSlot(window);
  if (!slot) return;
  bookingForm.value.startTime = formatDateTime(slot.startAt);
  bookingForm.value.endTime = formatDateTime(slot.endAt);
  bookingForm.value.slotMinutes = slot.slotMinutes;
  bookingForm.value.maxDurationMinutes = slot.maxDurationMinutes;
}
function slotKey(date, time) { return `${date}T${time}`; }
function buildSlots(windows, occupied = [], closures = [], now = new Date()) {
  const occupiedKeys = new Set(occupied.map((value) => String(value).replace(" ", "T").slice(0, 16)));
  return windows.flatMap((window) => {
    const open = new Date(`${window.date}T${window.openTime}`);
    const close = new Date(`${window.date}T${window.closeTime}`);
    const step = Number(window.slotMinutes) || 30;
    const slots = [];
    for (let start = open; start.getTime() + step * 60000 <= close.getTime(); start = new Date(start.getTime() + step * 60000)) {
      if (start <= now) continue;
      const startTime = start.toTimeString().slice(0, 5);
      const key = slotKey(window.date, startTime);
      const end = new Date(start.getTime() + step * 60000);
      const matchingClosure = closures.find((closure) => {
        const closureStart = new Date(closure.startTime);
        const closureEnd = new Date(closure.endTime);
        return closureStart < end && closureEnd > start;
      });
      const closed = Boolean(matchingClosure);
      slots.push({ key, date: window.date, startTime, endTime: end.toTimeString().slice(0, 5), slotMinutes: step, available: !occupiedKeys.has(key) && !closed, closed, closureReason: matchingClosure?.reason || "维护关闭" });
    }
    return slots;
  });
}
const currentSlots = computed(() => {
  const entry = availabilityByResource.value[bookingForm.value.resourceId];
  if (!entry) return [];
  return buildSlots(entry.windows || [], entry.occupied || [], entry.closures || [], currentTime.value);
});
const slotGroups = computed(() => currentSlots.value.reduce((groups, slot) => {
  (groups[slot.date] ||= []).push(slot);
  return groups;
}, {}));
const slotDates = computed(() => Object.entries(slotGroups.value)
  .filter(([date]) => date >= formatDate(currentTime.value))
  .sort(([left], [right]) => left.localeCompare(right))
  .map(([date, slots]) => ({ date, slots, availableCount: slots.filter((slot) => slot.available).length })));
const activeDateSlots = computed(() => slotGroups.value[activeSlotDate.value] || []);
const selectedSlots = computed(() => currentSlots.value.filter((slot) => selectedSlotKeys.value.includes(slot.key)));
watch(slotDates, (dates) => {
  if (!bookingModalOpen.value || !dates.length) return;
  if (!dates.some((item) => item.date === activeSlotDate.value)) {
    activeSlotDate.value = dates.find((item) => item.availableCount)?.date || dates[0].date;
  }
}, { deep: true });
function slotDateDay(date) { return String(date || "").slice(8, 10); }
function slotDateMonth(date) { return `${String(date || "").slice(5, 7)}月`; }
function slotDateWeek(date) {
  const index = new Date(`${date}T00:00:00`).getDay();
  return `周${"日一二三四五六"[index]}`;
}
function selectSlotDate(date) {
  if (activeSlotDate.value === date) return;
  activeSlotDate.value = date;
  if (selectedSlots.value.some((slot) => slot.date !== date)) {
    selectedSlotKeys.value = [];
    bookingForm.value.startTime = "";
    bookingForm.value.endTime = "";
  }
}
function toggleSlot(slot) {
  if (!slot.available) return;
  notice.value = "";
  const rangeSelection = selectedSlots.value.slice().sort((a, b) => a.key.localeCompare(b.key));
  if (!rangeSelection.length) {
    selectedSlotKeys.value = [slot.key];
    bookingForm.value.startTime = `${slot.date}T${slot.startTime}`;
    bookingForm.value.endTime = `${slot.date}T${slot.endTime}`;
    bookingForm.value.slotMinutes = slot.slotMinutes;
    return;
  }
  if (rangeSelection.length === 1) {
    const first = rangeSelection[0];
    if (slot.date !== first.date) {
      notice.value = "请选择同一天的开始和结束时段";
      return;
    }
    const firstIndex = currentSlots.value.indexOf(first);
    const targetIndex = currentSlots.value.indexOf(slot);
    const range = currentSlots.value.slice(Math.min(firstIndex, targetIndex), Math.max(firstIndex, targetIndex) + 1);
    if (range.some((item) => item.date !== slot.date || !item.available)) {
      notice.value = "开始和结束时段之间包含不可预约时间";
      return;
    }
    const maxSlots = Math.floor((Number(bookingForm.value.maxDurationMinutes) || 120) / (slot.slotMinutes || 30));
    if (range.length > maxSlots) {
      notice.value = `单次预约最长 ${bookingForm.value.maxDurationMinutes} 分钟`;
      return;
    }
    selectedSlotKeys.value = range.map((item) => item.key);
    bookingForm.value.startTime = `${range[0].date}T${range[0].startTime}`;
    bookingForm.value.endTime = `${range[range.length - 1].date}T${range[range.length - 1].endTime}`;
    bookingForm.value.slotMinutes = range[0].slotMinutes;
    return;
  }
  selectedSlotKeys.value = [slot.key];
  bookingForm.value.startTime = `${slot.date}T${slot.startTime}`;
  bookingForm.value.endTime = `${slot.date}T${slot.endTime}`;
  bookingForm.value.slotMinutes = slot.slotMinutes;
}
function closeBookingModal() { bookingModalOpen.value = false; }
async function login() {
  loginForm.value.username = loginForm.value.username.trim();
  if (!loginForm.value.username || !loginForm.value.password) {
    notice.value = "请输入工号和密码";
    return;
  }
  token.value = "";
  localStorage.removeItem("token");
  try {
    const r = await axios.post("/api/v1/user/login", loginForm.value);
    if (r.data?.code !== "SUCCESS" || !r.data?.data?.accessToken) {
      throw new Error(r.data?.message || "登录响应无效");
    }
    token.value = r.data.data.accessToken;
    user.value = r.data.data.user;
    localStorage.setItem("token", token.value);
    notice.value = "登录成功";
    await load();
  } catch (e) {
    notice.value = token.value
      ? e.response?.data?.message || "登录成功，但部分数据暂时无法加载"
      : e.response?.data?.message || "登录失败";
  }
}
async function load() {
  loading.value = true;
  try {
    const m = await axios.get("/api/v1/user/me");
    user.value = m.data.data;
    if (isAdmin.value) {
      if (isLabAdmin.value && !isSystemAdmin.value) {
        const r = await axios.get("/api/v1/resources");
        resources.value = r.data.data || [];
      } else {
        resources.value = [];
      }
      bookings.value = [];
      approvals.value = [];
      return;
    }
    const [r, b, types] = await Promise.all([
      axios.get("/api/v1/resources"),
      axios.get("/api/v1/bookings/my"),
      axios.get("/api/v1/resource-types"),
    ]);
    resources.value = r.data.data;
    bookings.value = b.data.data;
    resourceTypes.value = types.data?.data || [];
    if (isTeacher.value) {
      const approvalResponse = await axios.get("/api/v1/approvals/mine");
      approvals.value = approvalResponse.data?.data || [];
    }
    // Teachers retain the complete booking workflow, including calendar slots.
    await loadAvailability(resources.value);
  } catch (e) {
    notice.value = e.response?.data?.message || "服务暂不可用";
  } finally {
    loading.value = false;
  }
}
async function approveTask(task) {
  try {
    await axios.post(`/api/v1/approvals/${task.id}/approve`, {});
    notice.value = "审批已通过";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "审批操作失败";
    await load();
  }
}
function openRejection(task) {
  rejectionTarget.value = task;
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
    notice.value = "请填写驳回原因";
    return;
  }
  rejectionSaving.value = true;
  try {
    await axios.post(`/api/v1/approvals/${rejectionTarget.value.id}/reject`, { comment: reason });
    rejectionTarget.value = null;
    rejectionReason.value = "";
    notice.value = "预约申请已驳回";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "驳回操作失败";
    rejectionTarget.value = null;
    rejectionReason.value = "";
    await load();
  } finally {
    rejectionSaving.value = false;
  }
}
function selectResource(r) {
  bookingForm.value.resourceId = r.id;
  bookingForm.value.resourceName = resourceDisplayName(r);
  bookingForm.value.capacity = r.capacity;
  bookingForm.value.maxDurationMinutes = r.maxDurationMinutes;
  bookingForm.value.needCheckin = r.needCheckin;
  selectedSlotKeys.value = [];
  activeSlotDate.value = slotDates.value.find((item) => item.availableCount)?.date || slotDates.value[0]?.date || "";
  bookingModalOpen.value = true;
}
function resourceTypeName(resource) {
  return resourceTypeMap.value[resource?.typeId]?.name || resourceKind(resource);
}
function nextAvailableSlot(resource) {
  const entry = availabilityByResource.value[resource?.id];
  if (!entry) return null;
  return buildSlots(entry.windows || [], entry.occupied || [], entry.closures || [], currentTime.value)
    .find((slot) => slot.available) || null;
}
function resourceHasAvailability(resource) {
  return Boolean(nextAvailableSlot(resource));
}
async function loadAvailability(items) {
  const now = new Date();
  const end = new Date(now);
  end.setDate(end.getDate() + 14);
  const entries = await Promise.all(items.map(async (resource) => {
    try {
      const startDate = formatDate(now);
      const endDate = formatDate(end);
      const [calendarResponse, occupiedResponse] = await Promise.all([
        axios.get(`/api/v1/resources/${resource.id}/calendar`, { params: { start: startDate, end: endDate } }),
        axios.get(`/api/v1/bookings/resource/${resource.id}/occupied`, { params: { start: `${startDate}T00:00:00`, end: `${endDate}T23:59:59` } }),
      ]);
      const windows = (calendarResponse.data?.data?.days || []).flatMap((day) => (day.open || []).map((open) => ({
        date: day.date,
        openTime: open.openTime?.slice(0, 5),
        closeTime: open.closeTime?.slice(0, 5),
        slotMinutes: open.slotMinutes || 30,
        maxDurationMinutes: open.maxDurationMinutes || resource.maxDurationMinutes || 60,
      })));
      const closures = (calendarResponse.data?.data?.days || []).flatMap((day) => day.closures || []);
      const occupied = occupiedResponse.data?.data || [];
      const slots = buildSlots(windows, occupied, closures, now);
      return [resource.id, { windows, occupied, closures, slots, next: slots.find((slot) => slot.available) || null }];
    } catch {
      return [resource.id, { windows: [], next: null }];
    }
  }));
  availabilityByResource.value = Object.fromEntries(entries);
}
async function loadNextAvailableSlot(resourceId) {
  const from = new Date();
  const end = new Date(from);
  end.setDate(end.getDate() + 14);
  const fmt = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
  };
  try {
    const response = await axios.get(`/api/v1/resources/${resourceId}/calendar`, {
      params: { start: fmt(from), end: fmt(end) },
    });
    const now = new Date();
    const candidates = response.data?.data?.days?.flatMap((item) =>
      (item.open || []).map((open) => ({ day: item, open })),
    ) || [];
    const candidate = candidates.find(({ day, open }) => {
      const openAt = new Date(`${day.date}T${open.openTime}`);
      const closeAt = new Date(`${day.date}T${open.closeTime}`);
      const slotMinutes = open.slotMinutes || 30;
      const duration = Math.min(slotMinutes, open.maxDurationMinutes || bookingForm.value.maxDurationMinutes || 60);
      let startAt = openAt;
      if (fmt(now) === day.date && now > startAt) {
        const elapsed = Math.ceil((now.getTime() - openAt.getTime()) / 60000 / slotMinutes) * slotMinutes;
        startAt = new Date(openAt.getTime() + Math.max(0, elapsed) * 60000);
      }
      return startAt > now && new Date(startAt.getTime() + duration * 60000) <= closeAt;
    });
    const day = candidate?.day;
    const schedule = candidate?.open;
    if (!day || !schedule) {
      notice.value = "该资源未来14天没有开放时间，请联系管理员配置排班";
      bookingForm.value.startTime = "";
      bookingForm.value.endTime = "";
      return;
    }
    const slotMinutes = schedule.slotMinutes || 30;
    const maxDuration = schedule.maxDurationMinutes || bookingForm.value.maxDurationMinutes || 60;
    const open = new Date(`${day.date}T${schedule.openTime}`);
    const close = new Date(`${day.date}T${schedule.closeTime}`);
    let start = new Date(open);
    if (fmt(now) === day.date && now > start) {
      const elapsed = Math.max(0, Math.ceil((now.getTime() - open.getTime()) / 60000 / slotMinutes) * slotMinutes);
      start = new Date(open.getTime() + elapsed * 60000);
    }
    const duration = Math.min(slotMinutes, maxDuration);
    const end = new Date(start.getTime() + duration * 60000);
    if (start <= now || end > close) return;
    const time = (date) => `${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
    bookingForm.value.startTime = `${day.date}T${time(start)}`;
    bookingForm.value.endTime = `${day.date}T${time(end)}`;
    bookingForm.value.slotMinutes = slotMinutes;
    bookingForm.value.maxDurationMinutes = maxDuration;
  } catch (e) {
    notice.value = e.response?.data?.message || "无法读取资源开放时间";
  }
}
function logout() {
  token.value = "";
  user.value = null;
  resources.value = [];
  bookings.value = [];
  approvals.value = [];
  localStorage.removeItem("token");
  notice.value = "已退出登录";
}
async function createBooking() {
  const selected = selectedSlots.value.slice().sort((a, b) => a.key.localeCompare(b.key));
  if (!selected.length) {
    notice.value = "请先选择至少一个半小时预约时段";
    return;
  }
  for (let i = 1; i < selected.length; i += 1) {
    const previous = new Date(`${selected[i - 1].date}T${selected[i - 1].startTime}`);
    const current = new Date(`${selected[i].date}T${selected[i].startTime}`);
    if (current.getTime() - previous.getTime() !== selected[i - 1].slotMinutes * 60000) {
      notice.value = "请选择连续的预约时段";
      return;
    }
  }
  if (!bookingForm.value.startTime || !bookingForm.value.endTime) {
    notice.value = "请选择资源开放时间内的预约时段";
    return;
  }
  const startAt = new Date(bookingForm.value.startTime);
  const endAt = new Date(bookingForm.value.endTime);
  if (Number.isNaN(startAt.getTime()) || Number.isNaN(endAt.getTime()) || endAt <= startAt) {
    notice.value = "结束时间必须晚于开始时间";
    return;
  }
  const maxDurationMinutes = Number(bookingForm.value.maxDurationMinutes) || 120;
  if (endAt.getTime() - startAt.getTime() > maxDurationMinutes * 60000) {
    bookingForm.value.endTime = formatDateTime(new Date(startAt.getTime() + maxDurationMinutes * 60000));
    notice.value = `预约时长已调整为资源允许的最长 ${maxDurationMinutes} 分钟，请再次提交`;
    return;
  }
  try {
    await axios.post("/api/v1/bookings", bookingForm.value, {
      headers: { "Idempotency-Key": crypto.randomUUID() },
    });
    notice.value = "预约提交成功";
    bookingModalOpen.value = false;
    selectedSlotKeys.value = [];
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "预约失败";
  }
}
async function cancel(id) {
  try {
    await axios.post(
      `/api/v1/bookings/${id}/cancel`,
      {},
      { headers: { "Idempotency-Key": crypto.randomUUID() } },
    );
    notice.value = "预约已取消";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "取消失败";
  }
}
async function checkin(id) {
  const item = bookings.value.find((booking) => booking.id === id);
  if (item && !canCheckin(item)) {
    notice.value = checkinTitle(item) || "当前不在允许签到的时间范围内";
    return;
  }
  try {
    await axios.post(`/api/v1/bookings/${id}/checkin`, {});
    notice.value = "签到成功，已开始使用";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "签到失败";
    await load();
  }
}
onMounted(() => {
  currentTimeTimer = window.setInterval(() => { currentTime.value = new Date(); }, 60000);
  if (token.value) load();
});
onBeforeUnmount(() => window.clearInterval(currentTimeTimer));
</script>
<template>
  <div class="app">
    <header>
      <div class="brand">
        <div class="brand-mark">L</div>
        <div><strong>LabReserve</strong><small>实验室设备预约平台</small></div>
      </div>
      <div class="header-actions">
        <span v-if="user" class="identity">
          <b>{{ user.realName || user.username || "当前用户" }}</b>
          <small>{{ roleLabel }}</small>
        </span>
        <button class="icon-btn" @click="load" title="刷新">
          <RefreshCw :size="18" />
        </button>
        <button v-if="loggedIn" class="icon-btn logout-btn" @click="logout" title="退出登录">
          <LogOut :size="18" />
        </button>
      </div>
    </header>
    <Transition name="toast">
      <div v-if="toast.visible" class="app-toast" :class="toast.type" role="status" aria-live="polite">
        <CheckCircle2 v-if="toast.type === 'success'" :size="20" />
        <CircleAlert v-else :size="20" />
        <span><b>{{ toast.type === 'success' ? '操作成功' : toast.type === 'error' ? '操作未完成' : '系统提示' }}</b><small>{{ toast.message }}</small></span>
        <button title="关闭提示" @click="toast.visible = false"><X :size="16" /></button>
      </div>
    </Transition>
    <main v-if="!loggedIn" class="login-page">
      <section class="login-panel">
        <div class="eyebrow">LABORATORY ACCESS</div>
        <h1>让每一次实验<br /><em>都被妥善安排</em></h1>
        <p>统一管理实验室、仪器设备与会议空间，清晰预约，安心使用。</p>
        <div class="login-form">
          <label
            >学号 / 工号<input
              v-model="loginForm.username"
              placeholder="请输入学号或工号" /></label
          ><label
            >密码<input v-model="loginForm.password" type="password" placeholder="请输入密码" /></label
          ><button class="primary" @click="login">
            <LogIn :size="18" />进入系统
          </button>
          <div class="demo">演示账号：S20260001 / 12345678</div>
        </div>
      </section>
      <aside class="login-aside">
        <div class="aside-grid">
          <div>
            <CalendarDays :size="22" /><b>实时日历</b
            ><span>空闲与占用一目了然</span>
          </div>
          <div>
            <ShieldCheck :size="22" /><b>安全审批</b
            ><span>规范每一次资源使用</span>
          </div>
          <div>
            <ClipboardList :size="22" /><b>使用记录</b
            ><span>完整留存实验轨迹</span>
          </div>
        </div>
      </aside>
    </main>
    <AdminDashboard v-else-if="isAdmin" :user="user" :initial-resources="resources" />
    <main v-else-if="isTeacher" class="teacher-shell">
      <aside class="teacher-sidebar">
        <div class="teacher-workspace-mark"><ShieldCheck :size="20" /><div><b>教师工作台</b><small>{{ user?.realName || user?.username }}</small></div></div>
        <nav aria-label="教师功能导航">
          <button v-for="tab in teacherTabs" :key="tab.id" type="button" :class="{ active: teacherActiveTab === tab.id }" :title="tab.label" @click="teacherActiveTab = tab.id">
            <component :is="tab.icon" :size="17" /><span>{{ tab.label }}</span><em v-if="tab.id === 'approvals' && approvals.length">{{ approvals.length }}</em><ChevronRight v-else :size="14" />
          </button>
        </nav>
        <div class="teacher-scope"><span>当前审批范围</span><b>你负责的实验室资源</b><small>未配置负责人或本人申请自己负责的资源时，由实验室管理员审批</small></div>
      </aside>

      <section class="teacher-content">
        <div class="teacher-topline">
          <div><span class="eyebrow">TEACHER WORKSPACE</span><h1>{{ teacherPageTitle }}</h1></div>
          <button class="teacher-refresh" type="button" :disabled="loading" title="刷新教师工作台" @click="load"><RefreshCw :size="18" /></button>
        </div>

        <template v-if="teacherActiveTab === 'overview'">
          <section class="teacher-metrics">
            <div><span>待审批预约</span><strong>{{ approvals.length }}</strong><small>你负责的资源</small></div>
            <div><span>可预约资源</span><strong>{{ teacherAvailableResourceCount }}</strong><small>未来 14 天有空闲</small></div>
            <div><span>我的待审批</span><strong>{{ teacherPendingBookingCount }}</strong><small>由其他审批人处理</small></div>
            <div><span>近期使用安排</span><strong>{{ teacherUpcomingBookings.length }}</strong><small>已通过且尚未结束</small></div>
          </section>
          <div class="teacher-overview-grid">
            <section class="teacher-section">
              <div class="teacher-section-title"><div><h2>待处理审批</h2><p>你作为资源负责人的预约申请</p></div><button type="button" @click="teacherActiveTab = 'approvals'">查看全部 <ChevronRight :size="14" /></button></div>
              <div class="teacher-approval-list compact">
                <article v-for="task in approvals.slice(0, 5)" :key="task.id">
                  <span class="teacher-level">L{{ task.level }}</span>
                  <div><b>{{ task.resourceName || `预约 #${task.bookingId}` }}</b><span>{{ task.applicantName || `用户 ${task.applicantUserId}` }}</span><small>{{ task.startTime?.replace('T', ' ') }} - {{ task.endTime?.slice(11, 16) }}</small></div>
                  <div class="teacher-approval-actions"><button class="reject" type="button" title="驳回预约" @click="openRejection(task)"><X :size="15" /></button><button class="approve" type="button" title="通过预约" @click="approveTask(task)"><CheckCircle2 :size="15" /></button></div>
                </article>
                <div v-if="!approvals.length" class="teacher-empty">暂无待审批任务</div>
              </div>
            </section>
            <section class="teacher-section">
              <div class="teacher-section-title"><div><h2>近期使用安排</h2><p>教师账号已获批的资源使用计划</p></div><button type="button" @click="teacherActiveTab = 'bookings'">查看记录 <ChevronRight :size="14" /></button></div>
              <div class="teacher-schedule-list">
                <article v-for="item in teacherUpcomingBookings.slice(0, 5)" :key="item.id">
                  <div class="teacher-schedule-date"><strong>{{ bookingDay(item.startTime) }}</strong><span>{{ bookingMonth(item.startTime) }}</span></div>
                  <div><b>{{ item.resourceNameSnapshot }}</b><span><Clock3 :size="13" />{{ item.startTime?.slice(11, 16) }} - {{ item.endTime?.slice(11, 16) }}</span><small>{{ item.purpose }}</small></div>
                  <div class="teacher-schedule-actions">
                    <span class="teacher-status" :class="item.status.toLowerCase()">{{ bookingStatusText(item.status) }}</span>
                    <button v-if="showCheckin(item)" class="teacher-checkin" type="button" :disabled="!canCheckin(item)" :title="checkinTitle(item)" @click="checkin(item.id)">签到</button>
                  </div>
                </article>
                <div v-if="!teacherUpcomingBookings.length" class="teacher-empty">近期没有已通过的使用安排</div>
              </div>
            </section>
          </div>
        </template>

        <section v-else-if="teacherActiveTab === 'approvals'" class="teacher-section">
          <div class="teacher-section-title"><div><h2>预约审批</h2><p>一级由教师审批，二级由实验室管理员终审。未指定到人的一级任务，所有教师都可以处理</p></div><span>{{ approvals.length }} 项待处理</span></div>
          <div class="teacher-approval-list">
            <article v-for="task in approvals" :key="task.id">
              <span class="teacher-level">L{{ task.level }}</span>
              <div class="teacher-approval-summary"><b>{{ task.resourceName || `预约 #${task.bookingId}` }}</b><span>申请人：{{ task.applicantName || `用户 ${task.applicantUserId}` }}</span><small>预约 #{{ task.bookingId }}</small></div>
              <div class="teacher-approval-timing"><span>使用时间</span><b>{{ task.startTime?.replace('T', ' ').slice(0, 16) || '-' }} 至 {{ task.endTime?.replace('T', ' ').slice(0, 16) || '-' }}</b><small>审批截止 {{ task.deadline?.replace('T', ' ').slice(0, 16) || '-' }}</small></div>
              <div class="teacher-approval-actions labeled"><button class="reject" type="button" @click="openRejection(task)"><X :size="15" />驳回</button><button class="approve" type="button" @click="approveTask(task)"><CheckCircle2 :size="15" />通过</button></div>
            </article>
            <div v-if="!approvals.length" class="teacher-empty">暂无待审批预约</div>
          </div>
        </section>

        <section v-else-if="teacherActiveTab === 'resources'" class="teacher-section teacher-resource-section">
          <div class="teacher-section-title"><div><h2>资源预约</h2><p>教师可以按类别和空闲状态筛选实验室与设备</p></div><span>显示 {{ filteredResources.length }} / {{ resources.length }}</span></div>
          <div class="resource-filters teacher-resource-filters">
            <label class="resource-search"><Search :size="15" /><input v-model="resourceQuery" placeholder="搜索资源名称、位置或用途" /></label>
            <select v-model="resourceTypeFilter"><option value="">全部类别</option><option v-for="type in resourceTypes" :key="type.id" :value="type.id">{{ type.name }}</option></select>
            <label class="availability-filter"><input v-model="onlyAvailableResources" type="checkbox" />只看未来 14 天有空闲</label>
            <button v-if="resourceQuery || resourceTypeFilter || onlyAvailableResources" class="clear-resource-filter" type="button" @click="resourceQuery = ''; resourceTypeFilter = ''; onlyAvailableResources = false">清除筛选</button>
          </div>
          <div class="resource-card-grid teacher-resource-grid">
            <button v-for="r in filteredResources" :key="r.id" class="resource-card" :class="{ selected: bookingForm.resourceId === r.id }" type="button" @click="selectResource(r)">
              <div class="resource-card-image" :class="{ 'resource-image-fallback': !resourceImage(r) }"><img v-if="resourceImage(r)" :src="resourceImage(r)" :alt="`${resourceDisplayName(r)} 场景图`" loading="lazy" @error="handleResourceImageError" /><span v-else class="resource-image-empty">暂无图片</span><span class="resource-card-kind">{{ resourceTypeName(r) }}</span><span class="resource-card-status" :class="{ unavailable: !resourceHasAvailability(r) }"><i></i>{{ resourceHasAvailability(r) ? '有空闲' : '暂无空闲' }}</span></div>
              <div class="resource-card-body"><div class="resource-card-title"><strong>{{ resourceDisplayName(r) }}</strong><ChevronRight :size="16" /></div><p>{{ resourceDisplayDescription(r) }}</p><div class="resource-card-meta"><span><CalendarDays :size="14" />{{ r.location }}</span><span><UsersIcon :size="14" />{{ r.capacity }} 人</span></div><div class="resource-card-footer"><span v-if="nextAvailableSlot(r)">最近开放 {{ nextAvailableSlot(r).date }} {{ nextAvailableSlot(r).startTime }}</span><span v-else>未来 14 天暂无开放时段</span><b>申请预约</b></div></div>
            </button>
            <div v-if="!filteredResources.length" class="teacher-empty resource-card-empty">{{ resources.length ? '没有符合筛选条件的资源' : '暂无可预约资源' }}</div>
          </div>
        </section>

        <section v-else-if="teacherActiveTab === 'bookings'" class="teacher-section">
          <div class="teacher-section-title"><div><h2>我的预约</h2><p>教师本人提交的预约申请及处理状态</p></div><button type="button" @click="teacherActiveTab = 'resources'"><Plus :size="14" />新建预约</button></div>
          <div class="teacher-booking-table">
            <div class="teacher-booking-row head"><span>预约编号</span><span>资源</span><span>使用时间</span><span>用途</span><span>状态</span><span>操作</span></div>
            <div v-for="item in orderedBookings" :key="item.id" class="teacher-booking-row"><span class="mono">{{ item.bookingNo }}</span><span><b>{{ item.resourceNameSnapshot }}</b><small>{{ bookingResource(item)?.location }}</small></span><span>{{ item.startTime?.replace('T', ' ').slice(0, 16) }}<small>至 {{ item.endTime?.replace('T', ' ').slice(0, 16) }}</small></span><span>{{ item.purpose }}</span><span><span class="teacher-status" :class="item.status.toLowerCase()">{{ bookingStatusText(item.status) }}</span><small v-if="item.status === 'REJECTED'" class="teacher-reject-reason">{{ bookingRejectReason(item) || '暂无说明' }}</small><small v-else-if="showCheckin(item)">{{ checkinHintText(item) }}</small></span><span class="teacher-booking-actions"><button v-if="showCheckin(item)" class="teacher-checkin" type="button" :disabled="!canCheckin(item)" :title="checkinTitle(item)" @click="checkin(item.id)">签到</button><button v-if="canCancel(item)" class="teacher-cancel" type="button" @click="cancel(item.id)">取消</button><em v-if="!showCheckin(item) && !canCancel(item)">-</em></span></div>
            <div v-if="!bookings.length" class="teacher-empty">暂无预约记录</div>
          </div>
        </section>

        <div v-else-if="teacherActiveTab === 'maintenance'" class="teacher-tab-maintenance">
          <UserMaintenance :key="`teacher-maintenance-${user?.id}`" internal :resources="resources" :bookings="bookings" />
        </div>
      </section>
    </main>
    <main v-else class="dashboard student-dashboard">
      <section class="intro">
        <div>
          <div class="eyebrow">WORKSPACE / OVERVIEW</div>
          <h1>你好，{{ user?.realName || "同学" }}</h1>
          <p>今天也从一个清晰的预约开始。</p>
        </div>
        <div class="status-chip"><span></span>系统运行正常</div>
      </section>
      <div class="student-workspace">
        <div class="student-main">
          <section class="panel student-resource-gallery">
            <div class="panel-head">
              <div>
                <h2>资源目录</h2>
                <p>按类别和空闲状态筛选，选择资源后填写预约时段</p>
              </div>
              <button class="ghost" @click="load">
                <RefreshCw :size="16" />刷新
              </button>
            </div>
            <div class="resource-filters">
              <label class="resource-search"><Search :size="15" /><input v-model="resourceQuery" placeholder="搜索实验室名称、位置或用途" /></label>
              <select v-model="resourceTypeFilter"><option value="">全部类别</option><option v-for="type in resourceTypes" :key="type.id" :value="type.id">{{ type.name }}</option></select>
              <label class="availability-filter"><input v-model="onlyAvailableResources" type="checkbox" />只看未来14天有空闲</label>
              <button v-if="resourceQuery || resourceTypeFilter || onlyAvailableResources" class="clear-resource-filter" @click="resourceQuery = ''; resourceTypeFilter = ''; onlyAvailableResources = false">清除筛选</button>
              <span class="resource-filter-count">显示 {{ filteredResources.length }} / {{ resources.length }}</span>
            </div>
            <div class="resource-card-grid">
              <button
                v-for="r in filteredResources"
                :key="r.id"
                class="resource-card"
                :class="{ selected: bookingForm.resourceId === r.id }"
                @click="selectResource(r)"
              >
                <div class="resource-card-image" :class="{ 'resource-image-fallback': !resourceImage(r) }"><img v-if="resourceImage(r)" :src="resourceImage(r)" :alt="`${resourceDisplayName(r)} 场景图`" loading="lazy" @error="handleResourceImageError" /><span v-else class="resource-image-empty">暂无图片</span><span class="resource-card-kind">{{ resourceTypeName(r) }}</span><span class="resource-card-status" :class="{ unavailable: !resourceHasAvailability(r) }"><i></i>{{ resourceHasAvailability(r) ? '有空闲' : '暂无空闲' }}</span></div>
                <div class="resource-card-body"><div class="resource-card-title"><strong>{{ resourceDisplayName(r) }}</strong><ChevronRight :size="16" /></div><p>{{ resourceDisplayDescription(r) }}</p><div class="resource-card-meta"><span><CalendarDays :size="14" />{{ r.location }}</span><span><UsersIcon :size="14" />{{ r.capacity }} 人</span></div><div class="resource-card-footer"><span v-if="nextAvailableSlot(r)">最近开放 {{ nextAvailableSlot(r).date }} {{ nextAvailableSlot(r).startTime }}</span><span v-else>未来14天暂无开放时段</span><b>{{ resourceBookingCounts[r.id] || 0 }} 次我的预约</b></div></div>
              </button>
              <div v-if="!filteredResources.length" class="empty resource-card-empty">
                {{ resources.length ? '没有符合当前筛选条件的资源。' : '暂无资源，请先在资源服务中配置。' }}
              </div>
            </div>
          </section>
        </div>
        <aside class="student-side">
          <section class="panel history student-bookings">
            <div class="panel-head">
              <div>
                <h2>我的预约</h2>
                <p>近期使用安排与状态</p>
              </div>
            </div>
            <div class="student-booking-list">
              <article v-for="b in orderedBookings" :key="b.id" class="student-booking-card">
                <div class="booking-card-info">
                  <h3>{{ resourceDisplayName(bookingResource(b)) }}</h3>
                  <p><Clock3 :size="13" />{{ b.startTime?.replace('T', ' ').slice(0, 16) }} - {{ b.endTime?.slice(11, 16) }}<span>{{ b.purpose }}</span></p>
                  <p v-if="b.status === 'REJECTED'" class="booking-reject-reason">驳回原因：{{ bookingRejectReason(b) || '暂无说明' }}</p>
                  <p v-else-if="showCheckin(b)" class="booking-checkin-hint">{{ checkinHintText(b) }}</p>
                </div>
                <span class="booking-card-status" :class="b.status.toLowerCase()">{{ bookingStatusText(b.status) }}</span>
                <div class="booking-card-actions">
                  <button v-if="showCheckin(b)" class="booking-checkin" type="button" :disabled="!canCheckin(b)" :title="checkinTitle(b)" @click="checkin(b.id)">签到</button>
                  <button v-if="canCancel(b)" class="booking-cancel" type="button" @click="cancel(b.id)">取消</button>
                </div>
              </article>
              <div v-if="!bookings.length" class="empty">还没有预约记录</div>
            </div>
          </section>
          <UserMaintenance :key="`student-maintenance-${user?.id}`" compact :resources="resources" :bookings="bookings" />
        </aside>
      </div>
    </main>
    <div v-if="bookingModalOpen" class="booking-modal-backdrop" @click.self="closeBookingModal">
      <section class="booking-modal" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <div class="eyebrow">RESERVATION SLOTS</div>
            <h2>{{ bookingForm.resourceName }}</h2>
            <p>选择连续的半小时预约时段，暗色时段不可预约</p>
          </div>
          <button class="modal-close" type="button" @click="closeBookingModal" title="关闭" aria-label="关闭预约窗口"><X :size="22" /></button>
        </div>
        <div class="booking-modal-content">
          <div v-if="slotDates.length" class="slot-date-tabs" aria-label="选择预约日期">
            <button v-for="item in slotDates" :key="item.date" type="button" :class="{ active: activeSlotDate === item.date }" @click="selectSlotDate(item.date)">
              <span>{{ slotDateWeek(item.date) }}</span><strong>{{ slotDateDay(item.date) }}</strong><small>{{ slotDateMonth(item.date) }} · {{ item.availableCount ? `${item.availableCount} 个可选` : '暂无可约' }}</small>
            </button>
          </div>
          <div class="slot-legend"><span class="legend available"></span>可预约 <span class="legend selected"></span>已选择 <span class="legend maintenance"></span>维护关闭 <span class="legend unavailable"></span>不可预约</div>
          <div class="slot-days">
            <div v-if="activeDateSlots.length" class="slot-day">
              <div class="slot-date">{{ activeSlotDate }} · {{ slotDateWeek(activeSlotDate) }}</div>
              <div class="slot-grid">
                <button v-for="slot in activeDateSlots" :key="slot.key" type="button" class="slot-button" :class="{ selected: selectedSlotKeys.includes(slot.key), unavailable: !slot.available, maintenance: slot.closed }" :disabled="!slot.available" :title="slot.closed ? slot.closureReason : undefined" @click="toggleSlot(slot)">
                  <span>{{ slot.startTime }}-{{ slot.endTime }}</span><small v-if="slot.closed">维护</small>
                </button>
              </div>
            </div>
            <div v-else class="empty">未来14天没有配置开放时段</div>
          </div>
          <div class="modal-form">
            <label>使用目的<input v-model="bookingForm.purpose" placeholder="请输入本次使用目的" /></label>
            <label>参与人数<input v-model.number="bookingForm.participants" type="number" min="1" :max="bookingForm.capacity" /></label>
          </div>
        </div>
        <footer class="booking-modal-footer">
          <div class="modal-summary" :class="{ empty: !selectedSlots.length }"><template v-if="selectedSlots.length"><b>已选 {{ selectedSlots.length }} 个时段</b><span>{{ bookingForm.startTime.replace('T', ' ') }} - {{ bookingForm.endTime.slice(11) }}</span></template><template v-else><b>请选择连续时段</b><span>提交前可确认日期和时间</span></template></div>
          <button class="primary" type="button" :disabled="!selectedSlots.length || loading" @click="createBooking">提交预约</button>
        </footer>
      </section>
    </div>
    <div v-if="rejectionTarget" class="approval-reject-backdrop" @click.self="closeRejection">
      <section class="approval-reject-modal" role="dialog" aria-modal="true" aria-labelledby="teacher-reject-title">
        <header class="approval-reject-head">
          <div><h2 id="teacher-reject-title">驳回预约</h2><p>填写原因后，申请人可在预约记录中查看</p></div>
          <button class="modal-close" type="button" title="关闭" aria-label="关闭驳回窗口" :disabled="rejectionSaving" @click="closeRejection"><X :size="22" /></button>
        </header>
        <div class="approval-reject-body">
          <div class="approval-reject-context"><b>{{ rejectionTarget.resourceName || `预约 #${rejectionTarget.bookingId}` }}</b><span>申请人：{{ rejectionTarget.applicantName || `用户 ${rejectionTarget.applicantUserId}` }}</span><small>{{ rejectionTarget.startTime?.replace('T', ' ').slice(0, 16) || '-' }} 至 {{ rejectionTarget.endTime?.replace('T', ' ').slice(0, 16) || '-' }}</small></div>
          <label class="approval-reject-field"><span>驳回原因 <em>必填</em></span><textarea v-model="rejectionReason" maxlength="500" autofocus placeholder="请说明驳回原因，便于申请人修改后重新提交"></textarea><small>{{ rejectionReason.length }} / 500</small></label>
        </div>
        <footer class="approval-reject-actions"><button class="approval-reject-cancel" type="button" :disabled="rejectionSaving" @click="closeRejection">取消</button><button class="approval-reject-confirm" type="button" :disabled="!rejectionReason.trim() || rejectionSaving" @click="submitRejection">{{ rejectionSaving ? '提交中...' : '确认驳回' }}</button></footer>
      </section>
    </div>
  </div>
</template>
