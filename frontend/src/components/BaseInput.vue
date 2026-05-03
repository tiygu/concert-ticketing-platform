<template>
  <div class="w-full">
    <label v-if="label" class="block text-sm text-gray-400 mb-2">{{ label }}</label>
    <textarea
      v-if="type === 'textarea'"
      :value="modelValue"
      :placeholder="placeholder"
      :maxlength="maxlength"
      :rows="rows || 4"
      class="input-dark resize-none"
      :class="{ 'border-red-400 focus:border-red-400 focus:shadow-red-500/30': error }"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
    <input
      v-else
      :value="modelValue"
      :type="type"
      :placeholder="placeholder"
      :maxlength="maxlength"
      class="input-dark"
      :class="{ 'border-red-400 focus:border-red-400 focus:shadow-red-500/30': error }"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <p v-if="error" class="text-red-400 text-xs mt-1">{{ error }}</p>
    <p v-if="showWordLimit && maxlength" class="text-gray-500 text-xs mt-1 text-right">
      {{ (modelValue || '').length }} / {{ maxlength }}
    </p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  type?: string
  label?: string
  error?: string
  maxlength?: number
  showWordLimit?: boolean
  rows?: number
}>(), { type: 'text' })

defineEmits<{ 'update:modelValue': [v: string] }>()
</script>
