<template>
  <span class="px-2 py-1 rounded text-xs inline-flex items-center gap-1" :class="tagClass">
    <slot>{{ label }}</slot>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  type?: 'success' | 'warning' | 'danger' | 'info' | 'purple'
  label?: string
}>(), { type: 'info' })

const tagClass = computed(() => {
  const m: Record<string, string> = {
    success: 'bg-green-500/20 text-green-400',
    warning: 'bg-yellow-500/20 text-yellow-400',
    danger: 'bg-red-500/20 text-red-400',
    info: 'bg-blue-500/20 text-blue-400',
    purple: 'bg-purple-500/20 text-purple-400'
  }
  return m[props.type] || m.info
})
</script>
