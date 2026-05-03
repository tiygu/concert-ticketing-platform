<template>
  <div class="flex flex-col items-center justify-center py-8 text-gray-400">
    <span class="text-5xl mb-3 opacity-30">○</span>
    <p class="text-sm">{{ description }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ description?: string }>(), { description: '暂无数据' })
</script>
