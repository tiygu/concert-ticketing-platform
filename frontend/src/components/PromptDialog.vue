<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-[9998] flex items-center justify-center bg-black/70 backdrop-blur-sm" @click.self="cancel">
      <div class="glass-card rounded-2xl p-6 w-96 animate-[fadeIn_0.2s_ease]">
        <h3 class="text-lg font-bold text-white mb-3">{{ title }}</h3>
        <label class="block text-sm text-gray-400 mb-2">{{ label }}</label>
        <input
          v-model="inputValue"
          class="input-dark mb-6"
          @keyup.enter="submit(inputValue)"
        />
        <div class="flex justify-end gap-3">
          <button class="px-5 py-2 rounded-lg border border-white/20 text-sm hover:bg-white/10 transition-colors" @click="cancel">取消</button>
          <button class="btn-primary px-5 py-2 rounded-lg text-sm" @click="submit(inputValue)">确认</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useGlobalPrompt } from '../composables/usePrompt'

const { visible, title, label, submit, cancel } = useGlobalPrompt()
const inputValue = ref('')
</script>
