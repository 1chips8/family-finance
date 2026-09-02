<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ amount: string | number; type?: 'INCOME' | 'EXPENSE' | 'BALANCE' }>()
const numericAmount = computed(() => {
  const value = Number(props.amount)
  return Number.isFinite(value) ? value : 0
})
const prefix = computed(() => props.type === 'EXPENSE' ? '-' : props.type === 'INCOME' ? '+' : numericAmount.value < 0 ? '-' : '')
const formattedAmount = computed(() => Math.abs(numericAmount.value).toFixed(2))
</script>

<template><span :class="['amount-text', type?.toLowerCase(), { negative: type === 'BALANCE' && numericAmount < 0 }]">{{ prefix }}¥{{ formattedAmount }}</span></template>
