import { computed, ref, watch } from "vue";

export function usePagination(source, pageSize = 10) {
  const page = ref(1);
  const total = computed(() => source.value.length);
  const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)));
  const items = computed(() => {
    const current = Math.min(page.value, pageCount.value);
    const start = (current - 1) * pageSize;
    return source.value.slice(start, start + pageSize);
  });
  watch(pageCount, (count) => {
    if (page.value > count) page.value = count;
  });
  return { page, pageCount, items, total };
}
