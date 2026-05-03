<template>
  <div class="glass-card rounded-2xl" :class="paddingClass">
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{ padding?: string | boolean }>(), { padding: 'p-6' })
const paddingClass = computed(() => (props.padding === false ? '' : (typeof props.padding === 'string' ? props.padding : 'p-6')))
</script>
