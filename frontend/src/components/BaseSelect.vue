<template>
  <div class="w-full">
    <label v-if="label" class="block text-sm text-gray-400 mb-2">{{ label }}</label>
    <select
      :value="modelValue"
      class="input-dark appearance-none cursor-pointer"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <option v-if="placeholder" value="" class="bg-[#1a1a2e]">{{ placeholder }}</option>
      <option
        v-for="opt in options"
        :key="opt.value"
        :value="opt.value"
        class="bg-[#1a1a2e]"
      >
        {{ opt.label }}
      </option>
    </select>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue?: string | number
  options: { value: string | number; label: string }[]
  placeholder?: string
  label?: string
}>(), {})

defineEmits<{ 'update:modelValue': [v: string | number] }>()
</script>
