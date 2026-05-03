<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-[9998] flex items-center justify-center bg-black/70 backdrop-blur-sm" @click.self="cancel">
      <div class="glass-card rounded-2xl p-6 w-96 animate-[fadeIn_0.2s_ease]">
        <h3 class="text-lg font-bold text-white mb-2">{{ title }}</h3>
        <p class="text-gray-400 text-sm mb-6">{{ message }}</p>
        <div class="flex justify-end gap-3">
          <button class="px-5 py-2 rounded-lg border border-white/20 text-sm hover:bg-white/10 transition-colors" @click="cancel">取消</button>
          <button class="btn-primary px-5 py-2 rounded-lg text-sm" @click="confirm">确认</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { useGlobalConfirm } from '../composables/useConfirm'

const { visible, title, message, confirm, cancel } = useGlobalConfirm()
</script>
