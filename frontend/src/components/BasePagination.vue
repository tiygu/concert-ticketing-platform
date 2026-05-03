<template>
  <div v-if="totalPages > 1" class="flex items-center justify-between py-4 border-t border-white/10">
    <p class="text-sm text-gray-400">
      显示 {{ start }} - {{ end }} 共 {{ total }} 条
    </p>
    <div class="flex gap-1">
      <button
        class="px-3 py-1 rounded border border-white/20 text-sm hover:bg-white/10 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
        :disabled="current <= 1"
        @click="$emit('change', current - 1)"
      >上一页</button>
      <button
        v-for="p in visiblePages"
        :key="p"
        class="px-3 py-1 rounded text-sm transition-colors"
        :class="p === current ? 'bg-purple-500/30 text-white' : 'hover:bg-white/10 text-gray-400'"
        @click="$emit('change', p)"
      >{{ p }}</button>
      <button
        class="px-3 py-1 rounded border border-white/20 text-sm hover:bg-white/10 transition-colors disabled:opacity-30 disabled:cursor-not-allowed"
        :disabled="current >= totalPages"
        @click="$emit('change', current + 1)"
      >下一页</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  current: number
  pageSize: number
  total: number
}>(), {})

defineEmits<{ change: [page: number] }>()

const totalPages = computed(() => Math.ceil(props.total / props.pageSize) || 0)
const start = computed(() => (props.current - 1) * props.pageSize + 1)
const end = computed(() => Math.min(props.current * props.pageSize, props.total))

const visiblePages = computed(() => {
  const pages: number[] = []
  const tp = totalPages.value
  const curr = props.current
  let s = Math.max(1, curr - 2)
  const e = Math.min(tp, s + 4)
  s = Math.max(1, e - 4)
  for (let i = s; i <= e; i++) pages.push(i)
  return pages
})
</script>
