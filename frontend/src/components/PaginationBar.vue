<script setup>
import { computed } from "vue";
import { ChevronLeft, ChevronRight } from "lucide-vue-next";

const props = defineProps({
  page: { type: Number, default: 1 },
  size: { type: Number, default: 10 },
  total: { type: Number, default: 0 },
});
const emit = defineEmits(["update:page"]);
const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.size)));

function go(next) {
  if (next >= 1 && next <= pageCount.value && next !== props.page) emit("update:page", next);
}
</script>

<template>
  <div v-if="total > size" class="pagination-bar">
    <button type="button" :disabled="page <= 1" @click="go(page - 1)"><ChevronLeft :size="16" />上一页</button>
    <span>第 {{ page }} / {{ pageCount }} 页 · 共 {{ total }} 条</span>
    <button type="button" :disabled="page >= pageCount" @click="go(page + 1)">下一页<ChevronRight :size="16" /></button>
  </div>
</template>
