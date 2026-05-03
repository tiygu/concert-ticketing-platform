<template>
  <div class="inline-flex items-center gap-1">
    <button
      v-for="i in max"
      :key="i"
      :class="i <= modelValue ? 'text-yellow-400' : 'text-gray-600'"
      class="text-lg transition-colors"
      :disabled="readonly"
      @click="!readonly && $emit('update:modelValue', i)"
    >★</button>
    <span v-if="showScore && modelValue > 0" class="ml-2 text-sm text-gray-400">
      {{ modelValue.toFixed(1) }}
    </span>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: number
  max?: number
  readonly?: boolean
  showScore?: boolean
}>(), { max: 5 })

defineEmits<{ 'update:modelValue': [v: number] }>()
</script>
