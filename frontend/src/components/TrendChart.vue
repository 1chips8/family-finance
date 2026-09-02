<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { Money } from '../types/domain'

type Point = { month: string; income: Money; expense: Money }
const props = defineProps<{ points: Point[] }>()
const chartRef = ref<HTMLElement>()
let chart: echarts.ECharts | undefined
function render() {
  if (!chartRef.value) return
  chart ||= echarts.init(chartRef.value)
  chart.setOption({
    color: ['#466BF5', '#ed6a5a'],
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
<template><div ref="chartRef" class="trend-chart" aria-label="近12个月收支趋势图" /></template>
