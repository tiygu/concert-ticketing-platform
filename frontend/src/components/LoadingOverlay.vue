<template>
  <div v-if="visible" class="flex items-center justify-center py-16">
    <div class="w-10 h-10 border-3 border-neon-purple border-t-transparent rounded-full animate-spin" />
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ visible?: boolean }>(), { visible: true })
</script>
