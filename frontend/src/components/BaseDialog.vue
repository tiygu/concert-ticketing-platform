<template>
  <Teleport to="body">
    <div v-if="modelValue" class="fixed inset-0 z-[9997] flex items-center justify-center bg-black/70 backdrop-blur-sm" @click.self="close">
      <div class="glass-card rounded-2xl animate-[fadeIn_0.2s_ease] max-h-[85vh] overflow-y-auto" :style="{ width: width || '480px' }">
        <div v-if="$slots.title || title" class="flex items-center justify-between p-6 pb-0">
          <h3 class="text-lg font-bold text-white">{{ title }}</h3>
          <button class="text-gray-400 hover:text-white text-xl leading-none" @click="close">&times;</button>
        </div>
        <div class="p-6" :class="{ 'pt-4': $slots.title || title }">
          <slot />
        </div>
        <div v-if="$slots.footer" class="flex justify-end gap-3 p-6 pt-0">
          <slot name="footer" :close="close" />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: boolean
  title?: string
  width?: string
}>(), {})

const emit = defineEmits<{ 'update:modelValue': [v: boolean] }>()

function close() { emit('update:modelValue', false) }
</script>
