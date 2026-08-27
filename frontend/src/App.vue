<script setup>
import { ref, computed, onMounted } from "vue";
import axios from "axios";
import {
  CalendarDays,
  ClipboardList,
  LogIn,
  LogOut,
  Plus,
  RefreshCw,
  ShieldCheck,
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
const selectedSlots = computed(() => currentSlots.value.filter((slot) => selectedSlotKeys.value.includes(slot.key)));
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
    const [r, b, m] = await Promise.all([
      axios.get("/api/v1/resources"),
      axios.get("/api/v1/bookings/my"),
      axios.get("/api/v1/user/me"),
    ]);
    resources.value = r.data.data;
    bookings.value = b.data.data;
    user.value = m.data.data;
    if (isTeacher.value) {
      const approvalResponse = await axios.get("/api/v1/approvals/mine");
      approvals.value = approvalResponse.data?.data || [];
    } else {
      await loadAvailability(resources.value);
    }
  } catch (e) {
    notice.value = e.response?.data?.message || "服务暂不可用";
  } finally {
    loading.value = false;
  }
}
async function processApproval(task, action) {
  try {
    await axios.post(`/api/v1/approvals/${task.id}/${action}`, { comment: action === "approve" ? "Approved by teacher" : "Rejected by teacher" });
    notice.value = action === "approve" ? "Approval completed" : "Approval rejected";
    await load();
  } catch (e) {
    notice.value = e.response?.data?.message || "Approval action failed";
  }
}
function selectResource(r) {
  bookingForm.value.resourceId = r.id;
  bookingForm.value.resourceName = r.name;
  bookingForm.value.capacity = r.capacity;
  bookingForm.value.maxDurationMinutes = r.maxDurationMinutes;
  bookingForm.value.needCheckin = r.needCheckin;
  selectedSlotKeys.value = [];
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
          <small>学生</small>
        </span>
        <button class="icon-btn" @click="load" title="刷新">
          <RefreshCw :size="18" />
        </button>
        <button v-if="loggedIn" class="icon-btn logout-btn" @click="logout" title="退出登录">
          <LogOut :size="18" />
        </button>
      </div>
    </header>
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
          <div class="panel-head"><div><h2>待审批预约</h2><p>审核学生提交的资源使用申请</p></div><button class="ghost" @click="load"><RefreshCw :size="16" />刷新</button></div>
          <div class="approval-list">
            <article v-for="task in approvals" :key="task.id" class="approval-row">
              <div><b>预约 #{{ task.bookingId }}</b><span>申请人：{{ task.applicantName || task.userName || '学生' }}</span><small>{{ task.resourceName || '资源申请' }} · {{ task.startTime || '' }} - {{ task.endTime || '' }}</small></div>
              <div class="approval-actions"><button class="approve-btn" @click="processApproval(task, 'approve')">通过</button><button class="reject-btn" @click="processApproval(task, 'reject')">驳回</button></div>
            </article>
            <div v-if="!approvals.length" class="empty">暂无待审批预约</div>
          </div>
        </section>
        <section class="panel">
          <div class="panel-head"><div><h2>资源概览</h2><p>实验室和设备的当前容量</p></div><CalendarDays :size="20" /></div>
          <div class="resource-list teacher-resource-list">
            <div v-for="r in resources" :key="r.id" class="resource-row teacher-resource-row"><div class="resource-icon"><CalendarDays :size="20" /></div><div class="resource-info"><b>{{ r.name }}</b><span>{{ r.location }} · 容量 {{ r.capacity }} 人</span></div><span class="resource-status">{{ r.status === 'ACTIVE' ? '正常' : r.status }}</span></div>
            <div v-if="!resources.length" class="empty">暂无资源数据</div>
          </div>
        </section>
      </div>
      <section class="panel history"><div class="panel-head"><div><h2>我的预约</h2><p>教师账号提交的预约记录</p></div></div><div class="table"><div class="tr th"><span>预约编号</span><span>资源</span><span>时间</span><span>状态</span><span></span></div><div v-for="b in bookings" :key="b.id" class="tr"><span class="mono">{{ b.bookingNo }}</span><span>{{ b.resourceNameSnapshot }}</span><span>{{ b.startTime?.replace('T', ' ') }} - {{ b.endTime?.slice(11) }}</span><span>{{ b.status }}</span><span></span></div><div v-if="!bookings.length" class="empty">暂无预约记录</div></div></section>
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
          <span>待处理事项</span><strong>0</strong><small>当前无需审批</small>
        </div>
      </section>
      <div class="content-grid">
        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>资源目录</h2>
              <p>选择资源后填写预约时段</p>
            </div>
            <button class="ghost" @click="load">
              <RefreshCw :size="16" />刷新
            </button>
          </div>
          <div class="resource-list">
            <button
              v-for="r in resources"
              :key="r.id"
              class="resource-row"
              :class="{ selected: bookingForm.resourceId === r.id }"
              @click="selectResource(r)"
            >
              <div class="resource-icon"><CalendarDays :size="20" /></div>
              <div class="resource-info">
                <b>{{ r.name }}</b
                ><span>{{ r.location }} · 容量 {{ r.capacity }} 人</span>
              </div>
              <span class="resource-status">可预约</span>
              <small v-if="availabilityByResource[r.id]?.next" class="availability-hint">{{ availabilityByResource[r.id].next.date }} {{ availabilityByResource[r.id].next.openTime }}-{{ availabilityByResource[r.id].next.closeTime }}</small>
              <small v-else class="availability-hint unavailable">未来14天暂无开放时段</small>
            </button>
            <div v-if="!resources.length" class="empty">
              暂无资源，请先在资源服务中配置。
            </div>
          </div>
        </section>
        <section class="panel booking-panel">
          <div class="panel-head">
            <div>
              <h2>创建预约</h2>
              <p>预约时间须按 {{ bookingForm.slotMinutes }} 分钟粒度</p>
            </div>
            <Plus :size="20" />
          </div>
          <div class="form-grid">
            <label
              >开始时间<input
                v-model="bookingForm.startTime"
                type="datetime-local" /></label
            ><label
              >结束时间<input
                v-model="bookingForm.endTime"
                type="datetime-local" /></label
            ><label class="wide"
              >使用目的<input
                v-model="bookingForm.purpose"
                placeholder="请填写本次使用目的" /></label
            ><label
              >参与人数<input
                v-model.number="bookingForm.participants"
                type="number"
                min="1"
            /></label>
          </div>
          <div v-if="bookingForm.resourceId && availabilityByResource[bookingForm.resourceId]?.windows?.length" class="availability-panel">
            <div class="availability-title">可预约时间（未来14天）</div>
            <div class="availability-list">
              <button v-for="window in availabilityByResource[bookingForm.resourceId].windows" :key="`${window.date}-${window.openTime}`" type="button" class="availability-chip" @click="applyBookableSlot(window)">{{ window.date }} {{ window.openTime }}-{{ window.closeTime }}</button>
            </div>
          </div>
          <button
            class="primary full"
            :disabled="!bookingForm.resourceId || loading"
            @click="bookingModalOpen = true"
          >
            提交预约 <span>→</span>
          </button>
          <div class="notice" v-if="notice">{{ notice }}</div>
        </section>
      </div>
      <section class="panel history">
        <div class="panel-head">
          <div>
            <h2>我的预约</h2>
            <p>近期使用安排与状态</p>
          </div>
        </div>
        <div class="table">
          <div class="tr th">
            <span>预约编号</span><span>资源</span><span>时间</span
            ><span>状态</span><span></span>
          </div>
          <div v-for="b in bookings" :key="b.id" class="tr">
            <span class="mono">{{ b.bookingNo }}</span
            ><span>{{ b.resourceNameSnapshot }}</span
            ><span
              >{{ b.startTime?.replace("T", " ") }} -
              {{ b.endTime?.slice(11) }}</span
            ><span
              ><i class="dot" :class="b.status.toLowerCase()"></i
              >{{ b.status }}</span
            ><button
              v-if="['APPROVED', 'PENDING_APPROVAL'].includes(b.status)"
              class="text-btn"
              @click="cancel(b.id)"
            >
              取消</button
            ><span v-else></span>
          </div>
          <div v-if="!bookings.length" class="empty">还没有预约记录</div>
        </div>
      </section>
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
        <div class="slot-legend"><span class="legend available"></span>可预约 <span class="legend selected"></span>已选择 <span class="legend unavailable"></span>不可预约</div>
        <div class="slot-days">
          <div v-for="(slots, date) in slotGroups" :key="date" class="slot-day">
            <div class="slot-date">{{ date }}</div>
            <div class="slot-grid">
              <button v-for="slot in slots" :key="slot.key" type="button" class="slot-button" :class="{ selected: selectedSlotKeys.includes(slot.key), unavailable: !slot.available }" :disabled="!slot.available" @click="toggleSlot(slot)">
                {{ slot.startTime }}-{{ slot.endTime }}
              </button>
            </div>
          </div>
          <div v-if="!currentSlots.length" class="empty">未来14天没有配置开放时段</div>
        </div>
        <div class="modal-form">
          <label>使用目的<input v-model="bookingForm.purpose" placeholder="请输入本次使用目的" /></label>
          <label>参与人数<input v-model.number="bookingForm.participants" type="number" min="1" :max="bookingForm.capacity" /></label>
        </div>
        <div class="modal-summary" v-if="selectedSlots.length">已选择 {{ selectedSlots.length }} 个时段：{{ bookingForm.startTime.replace('T', ' ') }} - {{ bookingForm.endTime.slice(11) }}</div>
        <button class="primary full" type="button" :disabled="!selectedSlots.length || loading" @click="createBooking">提交预约</button>
        <div class="notice" v-if="notice">{{ notice }}</div>
      </section>
    </div>
  </div>
</template>
