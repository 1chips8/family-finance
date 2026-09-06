<script setup lang="ts">
import { computed } from 'vue'
import type { BudgetItem } from '../types/domain'

const props = defineProps<{ item: BudgetItem; label?: string }>()
const actualPercent = computed(() => Math.max(0, (Number(props.item.usageRate) || 0) * 100))
const percent = computed(() => Math.min(100, actualPercent.value))
const statusLabel = computed(() => ({ NORMAL: '正常', WARNING: '接近上限', OVER: '已超支' }[props.item.status] || '待设置'))
</script>

<template>
  <div class="budget-progress" :class="`budget-${item.status.toLowerCase()}`" :data-status="item.status">
    <div class="budget-progress-heading"><strong>{{ label || item.categoryName || '家庭总预算' }}</strong><span>{{ statusLabel }}</span></div>
    <div class="budget-progress-track" role="progressbar" :aria-label="`${label || item.categoryName || '预算'}使用率`" :aria-valuenow="percent" aria-valuemin="0" aria-valuemax="100"><i :style="{ width: `${percent}%` }" /></div>
    <div class="budget-progress-values"><span>已用 ¥{{ Number(item.spent || 0).toFixed(2) }} / 预算 ¥{{ Number(item.budget || 0).toFixed(2) }}</span><strong>{{ actualPercent.toFixed(1) }}%</strong></div>
    <small>剩余 ¥{{ Number(item.remaining || 0).toFixed(2) }}</small>
  </div>
</template>
