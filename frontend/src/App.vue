<script setup>
import { ref, computed, onMounted, watch } from "vue";
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
  ShieldCheck,
  Users as UsersIcon,
  X,
} from "lucide-vue-next";
const token = ref(localStorage.getItem("token") || "");
const user = ref(null);
const resources = ref([]);
const availabilityByResource = ref({});
const bookings = ref([]);
const approvals = ref([]);
const loading = ref(false);
const notice = ref("");
const bookingModalOpen = ref(false);
const selectedSlotKeys = ref([]);
const activeSlotDate = ref("");
const toast = ref({ visible: false, message: "", type: "info" });
let toastTimer;
const loginForm = ref({ username: "S20260001", password: "12345678" });
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
const pendingBookingCount = computed(() => bookings.value.filter((item) => item.status === "PENDING_APPROVAL").length);
watch(notice, (message) => {
  if (!message) return;
  const success = /成功|已通过|已完成|已取消|已退出/.test(message);
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
    if (error.response?.status === 401) {
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
  const name = String(resource?.name || "");
  if (name.includes("显微") || name.includes("材料") || name.includes("分析")) return "https://images.unsplash.com/photo-1576086213369-97a306d36557?auto=format&fit=crop&w=900&q=82";
  if (name.includes("计算机") || name.includes("电脑") || name.includes("工作站")) return "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=900&q=82";
  if (name.includes("电子") || name.includes("测量") || name.includes("示波")) return "https://images.unsplash.com/photo-1581091226825-a6a2a5aee158?auto=format&fit=crop&w=900&q=82";
  if (name.includes("会议") || name.includes("教室")) return "https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=900&q=82";
  return "https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=900&q=82";
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
  return ({ PENDING_APPROVAL: "待审批", APPROVED: "已通过", REJECTED: "已驳回", CANCELED: "已取消", CHECKED_IN: "使用中", COMPLETED: "已完成", NO_SHOW: "未签到" })[status] || status;
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
function buildSlots(windows, occupied = [], now = new Date()) {
  const occupiedKeys = new Set(occupied.map((value) => String(value).replace(" ", "T").slice(0, 16)));
  return windows.flatMap((window) => {
    const open = new Date(`${window.date}T${window.openTime}`);
    const close = new Date(`${window.date}T${window.closeTime}`);
    const step = Number(window.slotMinutes) || 30;
    const slots = [];
    for (let start = open; start.getTime() + step * 60000 <= close.getTime(); start = new Date(start.getTime() + step * 60000)) {
      const startTime = start.toTimeString().slice(0, 5);
      const key = slotKey(window.date, startTime);
      slots.push({ key, date: window.date, startTime, endTime: new Date(start.getTime() + step * 60000).toTimeString().slice(0, 5), slotMinutes: step, available: start > now && !occupiedKeys.has(key) });
    }
    return slots;
  });
}
const currentSlots = computed(() => {
  const entry = availabilityByResource.value[bookingForm.value.resourceId];
  if (!entry) return [];
  return buildSlots(entry.windows || [], entry.occupied || []);
});
const slotGroups = computed(() => currentSlots.value.reduce((groups, slot) => {
  (groups[slot.date] ||= []).push(slot);
  return groups;
}, {}));
const slotDates = computed(() => Object.entries(slotGroups.value)
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
  return;
  const index = selectedSlotKeys.value.indexOf(slot.key);
  if (index >= 0) selectedSlotKeys.value.splice(index, 1);
  else {
    const current = selectedSlots.value.slice().sort((a, b) => a.key.localeCompare(b.key));
    if (current.length) {
      if (slot.date !== current[0].date) {
        notice.value = "一次预约只能选择同一天的连续时段";
        return;
      }
      const adjacent = slot.key === current[0].key || slot.key === current[current.length - 1].key ||
        Math.abs(new Date(`${slot.date}T${slot.startTime}`).getTime() - new Date(`${current[0].date}T${current[0].startTime}`).getTime()) === current[0].slotMinutes * 60000 ||
        Math.abs(new Date(`${slot.date}T${slot.startTime}`).getTime() - new Date(`${current[current.length - 1].date}T${current[current.length - 1].startTime}`).getTime()) === current[current.length - 1].slotMinutes * 60000;
      if (!adjacent) {
        notice.value = "请选择相邻的时段，预约时间必须连续";
        return;
      }
      const maxSlots = Math.floor((Number(bookingForm.value.maxDurationMinutes) || 120) / (slot.slotMinutes || 30));
      if (current.length >= maxSlots) {
        notice.value = `单次预约最长 ${bookingForm.value.maxDurationMinutes} 分钟`;
        return;
      }
    }
    selectedSlotKeys.value.push(slot.key);
  }
  const selected = selectedSlots.value.slice().sort((a, b) => a.key.localeCompare(b.key));
  if (!selected.length) {
    bookingForm.value.startTime = "";
    bookingForm.value.endTime = "";
    return;
  }
  bookingForm.value.startTime = `${selected[0].date}T${selected[0].startTime}`;
  bookingForm.value.endTime = `${selected[selected.length - 1].date}T${selected[selected.length - 1].endTime}`;
  bookingForm.value.slotMinutes = selected[0].slotMinutes;
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
      const r = await axios.get("/api/v1/resources");
      resources.value = r.data.data || [];
      bookings.value = [];
      approvals.value = [];
      return;
    }
    const [r, b] = await Promise.all([
      axios.get("/api/v1/resources"),
      axios.get("/api/v1/bookings/my"),
    ]);
    resources.value = r.data.data;
    bookings.value = b.data.data;
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
async function processApproval(task, action) {
  try {
    await axios.post(`/api/v1/approvals/${task.id}/${action}`, { comment: action === "approve" ? "Approved by teacher" : "Rejected by teacher" });
    notice.value = action === "approve" ? "审批已通过" : "预约申请已驳回";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "审批操作失败";
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
      const occupied = occupiedResponse.data?.data || [];
      const slots = buildSlots(windows, occupied, now);
      return [resource.id, { windows, occupied, slots, next: slots.find((slot) => slot.available) || null }];
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
onMounted(() => {
  if (token.value) load();
});
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
              placeholder="例如 S20260001" /></label
          ><label
            >密码<input v-model="loginForm.password" type="password" /></label
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
    <main v-else-if="isTeacher" class="dashboard teacher-dashboard">
      <section class="intro">
        <div>
          <div class="eyebrow">TEACHER WORKSPACE</div>
          <h1>教师工作台</h1>
          <p>集中查看待审批预约和近期教学资源使用情况。</p>
        </div>
        <div class="status-chip"><span></span>教师账号</div>
      </section>
      <section class="stats">
        <div><span>待审批预约</span><strong>{{ approvals.length }}</strong><small>需要你处理的申请</small></div>
        <div><span>资源总数</span><strong>{{ resources.length }}</strong><small>当前可管理资源</small></div>
        <div><span>我的预约</span><strong>{{ bookings.length }}</strong><small>个人历史记录</small></div>
      </section>
      <div class="content-grid">
        <section class="panel">
          <div class="panel-head"><div><h2>待审批预约</h2><p>审核授权资源范围内、由其他用户提交的申请</p></div><button class="ghost" @click="load"><RefreshCw :size="16" />刷新</button></div>
          <div class="approval-list">
            <article v-for="task in approvals" :key="task.id" class="approval-row">
              <div><b>预约 #{{ task.bookingId }}</b><span>申请人：{{ task.applicantName || task.userName || `用户 ${task.applicantUserId}` }}</span><small>{{ task.resourceName || '资源申请' }} · {{ task.startTime?.replace('T', ' ') || '' }} - {{ task.endTime?.replace('T', ' ') || '' }}</small></div>
              <div class="approval-actions"><button class="approve-btn" @click="processApproval(task, 'approve')">通过</button><button class="reject-btn" @click="processApproval(task, 'reject')">驳回</button></div>
            </article>
            <div v-if="!approvals.length" class="empty">暂无待审批预约</div>
          </div>
        </section>
        <section class="panel">
          <div class="panel-head"><div><h2>资源概览</h2><p>实验室和设备的当前容量</p></div><CalendarDays :size="20" /></div>
          <div class="resource-list teacher-resource-list">
            <button v-for="r in resources" :key="r.id" class="resource-row teacher-resource-row" @click="selectResource(r)"><div class="resource-icon"><CalendarDays :size="20" /></div><div class="resource-info"><b>{{ r.name }}</b><span>{{ r.location }} · 容量 {{ r.capacity }} 人</span></div><span class="resource-status">{{ r.status === 'ACTIVE' ? '正常' : r.status }}</span><span class="teacher-book-action">申请预约 <ChevronRight :size="15" /></span></button>
            <div v-if="!resources.length" class="empty">暂无资源数据</div>
          </div>
        </section>
      </div>
      <section class="panel history"><div class="panel-head"><div><h2>我的预约</h2><p>教师账号提交的预约记录</p></div></div><div class="table"><div class="tr th"><span>预约编号</span><span>资源</span><span>时间</span><span>状态</span><span></span></div><div v-for="b in bookings" :key="b.id" class="tr"><span class="mono">{{ b.bookingNo }}</span><span>{{ b.resourceNameSnapshot }}</span><span>{{ b.startTime?.replace('T', ' ') }} - {{ b.endTime?.slice(11) }}</span><span>{{ b.status }}</span><span></span></div><div v-if="!bookings.length" class="empty">暂无预约记录</div></div></section>
      <UserMaintenance :key="`teacher-maintenance-${user?.id}`" />
      <div class="notice" v-if="notice">{{ notice }}</div>
    </main>
    <main v-else class="dashboard">
      <section class="intro">
        <div>
          <div class="eyebrow">WORKSPACE / OVERVIEW</div>
          <h1>你好，{{ user?.realName || "同学" }}</h1>
          <p>今天也从一个清晰的预约开始。</p>
        </div>
        <div class="status-chip"><span></span>系统运行正常</div>
      </section>
      <section class="stats">
        <div>
          <span>可预约资源</span><strong>{{ resources.length }}</strong
          ><small>实验室、设备与会议室</small>
        </div>
        <div>
          <span>我的预约</span><strong>{{ bookings.length }}</strong
          ><small>包含历史记录</small>
        </div>
        <div>
          <span>待审批预约</span><strong>{{ pendingBookingCount }}</strong><small>{{ pendingBookingCount ? '正在等待管理员处理' : '当前没有待审批申请' }}</small>
        </div>
      </section>
      <div class="student-content">
        <section class="panel student-resource-gallery">
          <div class="panel-head">
            <div>
              <h2>资源目录</h2>
              <p>选择资源后填写预约时段</p>
            </div>
            <button class="ghost" @click="load">
              <RefreshCw :size="16" />刷新
            </button>
          </div>
          <div class="resource-card-grid">
            <button
              v-for="r in resources"
              :key="r.id"
              class="resource-card"
              :class="{ selected: bookingForm.resourceId === r.id }"
              @click="selectResource(r)"
            >
              <div class="resource-card-image"><img :src="resourceImage(r)" :alt="`${resourceDisplayName(r)} 场景图`" loading="lazy" @error="handleResourceImageError" /><span class="resource-card-kind">{{ resourceKind(r) }}</span><span class="resource-card-status"><i></i>可预约</span></div>
              <div class="resource-card-body"><div class="resource-card-title"><strong>{{ resourceDisplayName(r) }}</strong><ChevronRight :size="16" /></div><p>{{ resourceDisplayDescription(r) }}</p><div class="resource-card-meta"><span><CalendarDays :size="14" />{{ r.location }}</span><span><UsersIcon :size="14" />{{ r.capacity }} 人</span></div><div class="resource-card-footer"><span v-if="availabilityByResource[r.id]?.next">最近开放 {{ availabilityByResource[r.id].next.date }} {{ availabilityByResource[r.id].next.openTime }}</span><span v-else>未来14天暂无开放时段</span><b>{{ resourceBookingCounts[r.id] || 0 }} 次我的预约</b></div></div>
            </button>
            <div v-if="!resources.length" class="empty resource-card-empty">
              暂无资源，请先在资源服务中配置。
            </div>
          </div>
        </section>
      </div>
      <section class="panel history student-bookings">
        <div class="panel-head">
          <div>
            <h2>我的预约</h2>
            <p>近期使用安排与状态</p>
          </div>
        </div>
        <div class="student-booking-list">
          <article v-for="b in bookings" :key="b.id" class="student-booking-card">
            <div class="booking-date"><strong>{{ bookingDay(b.startTime) }}</strong><span>{{ bookingMonth(b.startTime) }}</span></div>
            <div class="booking-resource-thumb"><img :src="resourceImage(bookingResource(b))" alt="" loading="lazy" @error="handleResourceImageError" /></div>
            <div class="booking-card-info"><span class="mono">{{ b.bookingNo }}</span><h3>{{ resourceDisplayName(bookingResource(b)) }}</h3><p><Clock3 :size="14" />{{ b.startTime?.slice(11, 16) }} - {{ b.endTime?.slice(11, 16) }}<span>{{ b.purpose }}</span></p></div>
            <span class="booking-card-status" :class="b.status.toLowerCase()"><i></i>{{ bookingStatusText(b.status) }}</span>
            <button v-if="['APPROVED', 'PENDING_APPROVAL'].includes(b.status)" class="booking-cancel" @click="cancel(b.id)">取消预约</button>
          </article>
          <div v-if="!bookings.length" class="empty">还没有预约记录</div>
        </div>
      </section>
      <div class="notice student-notice" v-if="notice">{{ notice }}</div>
      <UserMaintenance :key="`student-maintenance-${user?.id}`" />
    </main>
    <div v-if="bookingModalOpen" class="booking-modal-backdrop" @click.self="closeBookingModal">
      <section class="booking-modal" role="dialog" aria-modal="true">
        <div class="modal-head">
          <div>
            <div class="eyebrow">RESERVATION SLOTS</div>
            <h2>{{ bookingForm.resourceName }}</h2>
            <p>选择连续的半小时预约时段，暗色时段不可预约</p>
          </div>
          <button class="icon-btn" type="button" @click="closeBookingModal" title="关闭">×</button>
        </div>
        <div class="booking-modal-content">
          <div v-if="slotDates.length" class="slot-date-tabs" aria-label="选择预约日期">
            <button v-for="item in slotDates" :key="item.date" type="button" :class="{ active: activeSlotDate === item.date }" @click="selectSlotDate(item.date)">
              <span>{{ slotDateWeek(item.date) }}</span><strong>{{ slotDateDay(item.date) }}</strong><small>{{ slotDateMonth(item.date) }} · {{ item.availableCount ? `${item.availableCount} 个可选` : '已约满' }}</small>
            </button>
          </div>
          <div class="slot-legend"><span class="legend available"></span>可预约 <span class="legend selected"></span>已选择 <span class="legend unavailable"></span>不可预约</div>
          <div class="slot-days">
            <div v-if="activeDateSlots.length" class="slot-day">
              <div class="slot-date">{{ activeSlotDate }} · {{ slotDateWeek(activeSlotDate) }}</div>
              <div class="slot-grid">
                <button v-for="slot in activeDateSlots" :key="slot.key" type="button" class="slot-button" :class="{ selected: selectedSlotKeys.includes(slot.key), unavailable: !slot.available }" :disabled="!slot.available" @click="toggleSlot(slot)">
                  {{ slot.startTime }}-{{ slot.endTime }}
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
  </div>
</template>
