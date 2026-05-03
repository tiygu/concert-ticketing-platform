<template>
  <div class="overflow-x-auto">
    <table class="w-full text-sm">
      <thead class="bg-white/5 text-gray-400">
        <tr>
          <th v-for="col in columns" :key="col.key" class="text-left p-4 font-medium whitespace-nowrap">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-white/5">
        <tr v-if="loading">
          <td :colspan="columns.length" class="p-12 text-center text-gray-400">
            <div class="inline-block w-6 h-6 border-2 border-neon-purple border-t-transparent rounded-full animate-spin" />
          </td>
        </tr>
        <tr v-else-if="!data.length">
          <td :colspan="columns.length" class="p-12 text-center text-gray-400">
            <EmptyState />
          </td>
        </tr>
        <tr v-for="(row, idx) in data" :key="(row as any)[rowKey] ?? idx" class="hover:bg-white/5 transition-colors">
          <td v-for="col in columns" :key="col.key" class="p-4">
            <slot :name="'cell-' + col.key" :row="row" :value="(row as any)[col.key]">
              {{ (row as any)[col.key] }}
            </slot>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import EmptyState from './EmptyState.vue'

withDefaults(defineProps<{
  columns: { key: string; label: string }[]
  data: unknown[]
  loading?: boolean
  rowKey?: string
}>(), { rowKey: 'id' })
</script>
