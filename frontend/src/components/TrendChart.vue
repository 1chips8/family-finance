<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { Money } from '../types/domain'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

type Point = { month: string; income: Money; expense: Money }
const props = defineProps<{ points: Point[] }>()
const chartRef = ref<HTMLElement>()
let chart: echarts.EChartsType | undefined
const summary = computed(() => {
  if (!props.points.length) return '当前没有可展示的收支趋势数据。'
  const income = props.points.reduce((sum, point) => sum + Number(point.income || 0), 0)
  const expense = props.points.reduce((sum, point) => sum + Number(point.expense || 0), 0)
  return `趋势图包含 ${props.points.length} 个月，累计收入 ¥${income.toFixed(2)}，累计支出 ¥${expense.toFixed(2)}。`
})
function render() {
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption({
    color: ['#277a65', '#b8402f'],
    tooltip: { trigger: 'axis', valueFormatter: (value: unknown) => `¥${Number(value).toFixed(2)}` },
    legend: { bottom: 0, icon: 'circle', textStyle: { color: '#69718c' } },
    grid: { top: 16, right: 12, bottom: 38, left: 48 },
    xAxis: { type: 'category', data: props.points.map((point) => point.month.slice(5)), axisLine: { lineStyle: { color: '#e6e9f0' } }, axisLabel: { color: '#8790a7' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf0f5' } }, axisLabel: { color: '#8790a7' } },
    series: [
      { name: '收入', type: 'line', smooth: true, symbol: 'none', data: props.points.map((point) => Number(point.income)), areaStyle: { opacity: 0.08 } },
      { name: '支出', type: 'line', smooth: true, symbol: 'none', data: props.points.map((point) => Number(point.expense)), areaStyle: { opacity: 0.07 } },
    ],
  })
}
function resize() { chart?.resize() }
watch(() => props.points, () => nextTick(render), { deep: true })
onMounted(() => { render(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); chart?.dispose() })
</script>
<template><div class="trend-chart-wrap"><div ref="chartRef" class="trend-chart" role="img" aria-label="近12个月收支趋势图" :aria-describedby="`trend-summary-${props.points.length}`" /><p :id="`trend-summary-${props.points.length}`" class="sr-only">{{ summary }}</p></div></template>
