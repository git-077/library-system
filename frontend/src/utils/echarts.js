/**
 * ECharts 按需引入（只注册本项目用到的图表与组件）。
 * 全量 import 'echarts' 会让业务包多出约 800KB，这里按需注册后 gzip 后约 90KB。
 */
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  DatasetComponent,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { LabelLayout, UniversalTransition } from 'echarts/features'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  LineChart,
  PieChart,
  BarChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DatasetComponent,
  LabelLayout,
  UniversalTransition,
  CanvasRenderer
])

/** 系列色板：与 theme.css 的森林绿/烫金/书封配色保持一致 */
export const CHART_COLORS = [
  '#1f4b3a',
  '#e0ad3a',
  '#3c6e9c',
  '#a2544a',
  '#6a5b93',
  '#46707a',
  '#9c7b2e',
  '#2f6b53',
  '#8a8276',
  '#c1912a'
]

/** 统一的图表基础样式（字体、轴线颜色取自设计令牌） */
export const BASE_TEXT_STYLE = {
  color: '#4b463f',
  fontFamily:
    "'Inter', 'PingFang SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
}

export default echarts
