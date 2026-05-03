<template>
  <div class="w-full">
    <label v-if="label" class="block text-sm text-gray-400 mb-2">{{ label }}</label>
    <input
      :value="modelValue"
      :type="inputType"
      class="input-dark"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  label?: string
  pickerType?: 'date' | 'datetime' | 'daterange'
}>(), { pickerType: 'datetime' })

defineEmits<{ 'update:modelValue': [v: string] }>()

const inputType = computed(() => props.pickerType === 'daterange' ? 'text' : (props.pickerType === 'datetime' ? 'datetime-local' : 'date'))
</script>
