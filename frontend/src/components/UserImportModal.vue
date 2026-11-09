<script setup>
import { ref } from "vue";
import axios from "axios";
import { Upload, X } from "lucide-vue-next";

const emit = defineEmits(["close", "imported"]);
const csvText = ref("");
const saving = ref(false);
const notice = ref("");
const failed = ref(false);
const result = ref(null);

const TEMPLATE = "工号,姓名,邮箱,手机,角色,初始密码\nS20261001,示例学生,student-import@example.com,13800001001,学生,12345678\nT20261001,示例教师,teacher-import@example.com,13900001001,教师,12345678\n";

function show(message, isFailed = false) {
  notice.value = message;
  failed.value = isFailed;
}

function downloadTemplate() {
  const blob = new Blob(["\uFEFF" + TEMPLATE], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "用户导入模板.csv";
  link.click();
  URL.revokeObjectURL(url);
}

function onFile(event) {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    csvText.value = String(reader.result || "");
    result.value = null;
  };
  reader.readAsText(file, "UTF-8");
  event.target.value = "";
}

async function submitImport() {
  if (!csvText.value.trim()) return show("请粘贴 CSV 或选择文件", true);
  saving.value = true;
  notice.value = "";
  result.value = null;
  try {
    const response = await axios.post("/api/v1/admin/users/import", { csv: csvText.value });
    const data = response.data?.data || {};
    result.value = data;
    const created = Number(data.createdCount || 0);
    const skipped = Number(data.skippedCount || 0);
    const failedCount = Number(data.failedCount || 0);
    show(`导入完成：成功 ${created} 人，跳过 ${skipped} 人，失败 ${failedCount} 人`, failedCount > 0 && created === 0);
    if (created > 0) emit("imported");
  } catch (e) {
    show(e.response?.data?.message || "用户导入失败", true);
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div class="import-backdrop" @click.self="emit('close')">
    <section class="import-modal" role="dialog" aria-modal="true" aria-labelledby="user-import-title">
      <header class="import-head">
        <div>
          <h2 id="user-import-title">导入用户</h2>
          <p>CSV 列：工号,姓名,邮箱,手机,角色,初始密码。角色只能填学生或教师，实验室管理员和系统管理员请在用户列表中单独分配。</p>
        </div>
        <button class="modal-close" type="button" title="关闭" aria-label="关闭导入窗口" @click="emit('close')"><X :size="22" /></button>
      </header>
      <div v-if="notice" class="import-notice" :class="{ failed }">{{ notice }}</div>
      <div class="import-body">
        <div class="import-actions">
          <button class="quiet" type="button" @click="downloadTemplate">下载模板</button>
          <label class="file-button">选择 CSV 文件<input type="file" accept=".csv,text/csv,text/plain" @change="onFile" /></label>
        </div>
        <textarea v-model="csvText" class="import-box" placeholder="工号,姓名,邮箱,手机,角色,初始密码"></textarea>
        <p class="hint">仅可导入学生和教师。已存在的工号会跳过；密码留空则使用 12345678。单次最多 200 人。</p>
        <div v-if="result && (result.skipped?.length || result.failed?.length)" class="import-issues">
          <div v-for="item in [...(result.failed || []), ...(result.skipped || [])]" :key="`${item.line}-${item.username}-${item.reason}`">
            <b>第 {{ item.line }} 行</b>
            <span>{{ item.username || "未识别工号" }}</span>
            <small>{{ item.reason }}</small>
          </div>
        </div>
        <button class="command" type="button" :disabled="saving" @click="submitImport"><Upload :size="16" />{{ saving ? "导入中..." : "开始导入" }}</button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.import-backdrop{position:fixed;z-index:50;inset:0;background:rgba(18,31,26,.48);display:grid;place-items:center;padding:20px}
.import-modal{width:min(640px,100%);max-height:90vh;overflow:auto;background:#fff;border-radius:7px}
.import-head{padding:19px 21px;border-bottom:1px solid #e6ece9;display:flex;justify-content:space-between;gap:12px}
.import-head h2{font-size:17px;margin:0 0 4px}
.import-head p{font-size:11px;color:#82918a;margin:0;line-height:1.5}
.modal-close{border:0;background:transparent;color:#5d7268}
.import-notice{margin:12px 21px 0;min-height:38px;padding:0 12px;background:#e8f5ed;color:#347458;display:flex;align-items:center;border-left:3px solid #4d9a70;font-size:12px}
.import-notice.failed{background:#faece9;color:#a24d42;border-color:#bd655a}
.import-body{padding:16px 21px 21px;display:grid;gap:12px}
.import-actions{display:flex;gap:8px;flex-wrap:wrap}
.quiet,.file-button,.command{height:36px;border-radius:5px;padding:0 12px;display:inline-flex;align-items:center;justify-content:center;gap:6px;font-size:12px}
.quiet,.file-button{border:1px solid #d6e0db;background:#fff;color:#42695a}
.file-button{cursor:pointer}
.file-button input{display:none}
.command{border:0;background:#225c4d;color:#fff}
.command:disabled{opacity:.5}
.import-box{width:100%;min-height:180px;border:1px solid #d8e2dd;border-radius:4px;padding:10px;font-family:ui-monospace,monospace;font-size:12px;resize:vertical;color:#243b31}
.hint{margin:0;color:#82918a;font-size:11px}
.import-issues{max-height:140px;overflow:auto;border:1px solid #edf1ef;border-radius:4px}
.import-issues>div{display:grid;grid-template-columns:70px 1fr 1.6fr;gap:8px;align-items:center;padding:8px 10px;border-bottom:1px solid #edf1ef;font-size:11px}
.import-issues>div:last-child{border-bottom:0}
.import-issues small{color:#9a5a51}
</style>
