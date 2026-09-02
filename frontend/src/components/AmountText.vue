<script setup lang="ts">
defineProps<{ amount: string | number; type?: 'INCOME' | 'EXPENSE' | 'BALANCE' }>()
</script>
<template><span :class="['amount-text', type?.toLowerCase()]">{{ type === 'EXPENSE' ? '-' : type === 'INCOME' ? '+' : '' }}¥{{ Number(amount).toFixed(2) }}</span></template>
