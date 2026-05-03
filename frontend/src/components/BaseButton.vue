<template>
  <button
    :class="btnClass"
    :disabled="disabled || loading"
    :type="nativeType"
    class="inline-flex items-center justify-center gap-2 rounded-lg transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
  >
    <span v-if="loading" class="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
    <slot />
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  variant?: 'primary' | 'danger' | 'success' | 'ghost' | 'link'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  nativeType?: 'button' | 'submit'
}>(), { variant: 'primary', size: 'md', nativeType: 'button' })

const btnClass = computed(() => {
  const size = { sm: 'px-3 py-1.5 text-xs', md: 'px-5 py-2 text-sm', lg: 'px-6 py-3 text-base' }[props.size]
  const base = size + ' font-bold'

  const variants: Record<string, string> = {
    primary: base + ' btn-primary',
    danger: base + ' btn-danger',
    success: base + ' bg-gradient-to-r from-green-500 to-teal-600 text-white font-bold hover:shadow-lg hover:shadow-green-500/30',
    ghost: base + ' btn-ghost',
    link: base + ' text-cyan-400 hover:underline bg-transparent'
  }
  return variants[props.variant] || variants.primary
})
</script>
