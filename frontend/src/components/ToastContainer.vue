<template>
  <div class="fixed bottom-8 right-8 z-[9999] flex flex-col gap-3 pointer-events-none">
    <TransitionGroup
      enter-from-class="opacity-0 translate-y-4"
      enter-active-class="transition-all duration-300"
      leave-active-class="transition-all duration-300"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div
        v-for="t in toasts"
        :key="t.id"
        class="glass-card rounded-xl px-5 py-3 flex items-center gap-3 pointer-events-auto cursor-pointer shadow-lg max-w-sm"
        :class="toastBorder(t.type)"
        @click="remove(t.id)"
      >
        <span class="text-lg">{{ icon(t.type) }}</span>
        <span class="text-sm text-white">{{ t.message }}</span>
      </div>
    </TransitionGroup>
  </div>
</template>

<script setup lang="ts">
import { useToast } from '../composables/useToast'

const { toasts, remove } = useToast()

function icon(type: string) {
  const m: Record<string, string> = { success: '✔', error: '✘', warning: '⚠', info: 'ℹ' }
  return m[type] || 'ℹ'
}

function toastBorder(type: string) {
  const m: Record<string, string> = {
    success: 'border-l-emerald-400',
    error: 'border-l-red-400',
    warning: 'border-l-yellow-400',
    info: 'border-l-cyan-400'
  }
  return m[type] || 'border-l-cyan-400'
}
</script>
