<template>
  <div ref="host" class="echart-host" :style="{ height }"></div>
</template>

<script setup>
/**
 * ECharts 轻量封装：只负责挂载、更新 option、跟随容器尺寸变化。
 * 业务侧只管把 option 传进来，无需关心实例生命周期（避免内存泄漏）。
 */
import { onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue'
import echarts from '../utils/echarts'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '260px' }
})

const host = ref(null)
const chart = shallowRef(null)
let observer = null

function render() {
  if (!chart.value) return
  // notMerge=true：数据整体替换时避免旧系列残留
  chart.value.setOption(props.option, true)
}

onMounted(() => {
  chart.value = echarts.init(host.value)
  render()
  // 侧边栏折叠/窗口缩放时容器尺寸会变，用 ResizeObserver 比监听 window.resize 更准
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(host.value)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
  chart.value = null
})
</script>

<style scoped>
.echart-host {
  width: 100%;
}
</style>
