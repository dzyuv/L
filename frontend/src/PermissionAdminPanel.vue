<script setup>
import { computed, onMounted, ref, watch } from "vue";
import axios from "axios";
import { Save } from "lucide-vue-next";

const emit = defineEmits(["notice"]);
const loading = ref(false);
const saving = ref(false);
const roles = ref([]);
const catalog = ref([]);
const required = ref({});
const selectedRole = ref("");
const selectedPermissions = ref([]);

const currentRole = computed(() => roles.value.find((item) => item.code === selectedRole.value) || null);
const groups = computed(() => {
  const map = new Map();
  for (const item of catalog.value) {
    const key = item.group || "其他";
    if (!map.has(key)) map.set(key, []);
    map.get(key).push(item);
  }
  return [...map.entries()].map(([name, items]) => ({ name, items }));
});
const requiredCodes = computed(() => new Set(required.value[selectedRole.value] || []));
const dirty = computed(() => {
  const original = [...(currentRole.value?.permissions || [])].sort();
  const current = [...selectedPermissions.value].sort();
  return original.join("\u0001") !== current.join("\u0001");
});

function roleName(code) {
  return ({ STUDENT: "学生", TEACHER: "教师", LAB_ADMIN: "实验室管理员", SYSTEM_ADMIN: "系统管理员" })[code] || code;
}

function isRequired(code) {
  return requiredCodes.value.has(code);
}

function toggle(code) {
  if (isRequired(code)) return;
  if (selectedPermissions.value.includes(code)) {
    selectedPermissions.value = selectedPermissions.value.filter((item) => item !== code);
  } else {
    selectedPermissions.value = [...selectedPermissions.value, code];
  }
}

function selectRole(code) {
  selectedRole.value = code;
  const role = roles.value.find((item) => item.code === code);
  selectedPermissions.value = [...(role?.permissions || [])];
}

async function load() {
  loading.value = true;
  try {
    const response = await axios.get("/api/v1/admin/roles");
    const data = response.data?.data || {};
    roles.value = data.items || [];
    catalog.value = data.catalog || [];
    required.value = data.required || {};
    const next = selectedRole.value && roles.value.some((item) => item.code === selectedRole.value)
      ? selectedRole.value
      : (roles.value[0]?.code || "");
    selectRole(next);
  } catch (e) {
    emit("notice", e.response?.data?.message || "权限数据加载失败", true);
  } finally {
    loading.value = false;
  }
}

async function save() {
  if (!selectedRole.value) return;
  saving.value = true;
  try {
    await axios.put(`/api/v1/admin/roles/${selectedRole.value}/permissions`, { permissions: selectedPermissions.value });
    emit("notice", "角色权限已保存，相关用户需重新登录后生效");
    await load();
  } catch (e) {
    emit("notice", e.response?.data?.message || "权限保存失败", true);
  } finally {
    saving.value = false;
  }
}

watch(selectedRole, (code) => {
  const role = roles.value.find((item) => item.code === code);
  if (role) selectedPermissions.value = [...(role.permissions || [])];
});

onMounted(load);
</script>

<template>
  <div class="permission-layout">
    <section class="admin-section">
      <div class="section-title"><div><h2>角色</h2><p>选择角色后配置其可执行的操作</p></div></div>
      <div class="role-list">
        <button v-for="item in roles" :key="item.code" type="button" :class="{ selected: item.code === selectedRole }" @click="selectRole(item.code)">
          <b>{{ roleName(item.code) }}</b>
          <small>{{ item.permissions?.length || 0 }} 项权限</small>
        </button>
        <div v-if="!roles.length && !loading" class="admin-empty">暂无角色</div>
      </div>
    </section>
    <section class="admin-section permission-detail">
      <div class="section-title">
        <div>
          <h2>{{ currentRole ? roleName(currentRole.code) : "权限清单" }}</h2>
          <p>{{ currentRole ? `已选 ${selectedPermissions.length} 项。系统管理员的基础权限不可取消。` : "请选择左侧角色" }}</p>
        </div>
        <button class="command" type="button" :disabled="!currentRole || saving || !dirty" @click="save"><Save :size="16" />保存权限</button>
      </div>
      <div v-if="currentRole" class="permission-groups">
        <article v-for="group in groups" :key="group.name">
          <h3>{{ group.name }}</h3>
          <label v-for="item in group.items" :key="item.code" :class="{ locked: isRequired(item.code) }">
            <input type="checkbox" :value="item.code" :checked="selectedPermissions.includes(item.code)" :disabled="isRequired(item.code)" @change="toggle(item.code)" />
            <span>
              <b>{{ item.name }}</b>
              <small>{{ item.code }}{{ isRequired(item.code) ? " · 必选" : "" }}</small>
            </span>
          </label>
        </article>
      </div>
      <div v-else class="admin-empty">选择角色后可勾选权限</div>
    </section>
  </div>
</template>

<style scoped>
.permission-layout{display:grid;grid-template-columns:240px minmax(0,1fr);gap:14px;align-items:start}
.admin-section{background:#fff;border:1px solid #dfe7e3;border-radius:6px;overflow:hidden}
.section-title{padding:18px 20px;border-bottom:1px solid #e8eeeb;display:flex;align-items:center;justify-content:space-between;gap:12px}
.section-title h2{font-size:15px;margin:0 0 4px}
.section-title p{margin:0;color:#839089;font-size:11px}
.role-list{padding:8px}
.role-list>button{width:100%;min-height:58px;border:0;border-bottom:1px solid #edf1ef;background:#fff;text-align:left;padding:10px 12px}
.role-list>button:last-child{border-bottom:0}
.role-list>button:hover,.role-list>button.selected{background:#edf6f1}
.role-list b,.role-list small{display:block}
.role-list b{font-size:13px}
.role-list small{margin-top:4px;color:#82918a;font-size:10px}
.command{height:36px;border:0;border-radius:5px;background:#225c4d;color:#fff;padding:0 13px;display:inline-flex;align-items:center;gap:7px}
.command:disabled{opacity:.5}
.permission-groups{padding:8px 20px 20px;display:grid;gap:18px}
.permission-groups h3{margin:12px 0 8px;font-size:12px;color:#4b7c68}
.permission-groups label{display:grid;grid-template-columns:18px 1fr;gap:10px;align-items:flex-start;padding:8px 0;border-bottom:1px solid #f0f4f2;font-size:12px;cursor:pointer}
.permission-groups label:last-child{border-bottom:0}
.permission-groups b,.permission-groups small{display:block}
.permission-groups small{margin-top:3px;color:#82918a;font-size:10px}
.permission-groups label.locked{opacity:.85}
.admin-empty{padding:30px;text-align:center;color:#8d9a94;font-size:12px}
@media(max-width:950px){.permission-layout{grid-template-columns:1fr}}
</style>
