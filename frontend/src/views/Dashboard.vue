<template>
  <div class="dash">
    <header class="dash-head">
      <div>
        <p class="eyebrow">OVERVIEW</p>
        <h1 class="dash-title">数据统计</h1>
        <p class="dash-sub">全馆实时运行概览，数据来自借阅与馆藏主表</p>
      </div>
      <span class="stamp">更新于 {{ stamp }}</span>
    </header>

    <div class="stat-grid">
      <article v-for="card in cards" :key="card.label" class="stat" :class="card.tone">
        <div class="stat-top">
          <span class="glyph">{{ card.glyph }}</span>
          <span class="label">{{ card.label }}</span>
        </div>
        <div class="value">
          {{ card.value }}<em>{{ card.unit }}</em>
        </div>
        <p class="caption">{{ card.caption }}</p>
      </article>
    </div>

    <div class="chart-row">
      <section class="panel">
        <div class="panel-head">
          <div>
            <h2 class="panel-title">借还趋势</h2>
            <p class="panel-sub">近 {{ days }} 天每日借出与归还册数</p>
          </div>
          <div class="range">
            <button
              v-for="r in ranges"
              :key="r"
              type="button"
              :class="{ on: r === days }"
              @click="switchRange(r)"
            >
              {{ r }} 天
            </button>
          </div>
        </div>
        <EChart :option="trendOption" height="284px" />
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <h2 class="panel-title">馆藏分布</h2>
            <p class="panel-sub">按分类统计在册书目品种</p>
          </div>
        </div>
        <EChart :option="pieOption" height="284px" />
      </section>
    </div>

    <section class="panel rank">
      <div class="panel-head">
        <div>
          <h2 class="panel-title">借阅排行</h2>
          <p class="panel-sub">历史累计借阅次数 Top {{ topBooks.length || '—' }}</p>
        </div>
        <span class="hint">含已归还记录</span>
      </div>

      <ul v-if="topBooks.length" class="rank-list">
        <li v-for="(b, i) in topBooks" :key="b.bookId" class="rank-item">
          <span class="rank-no" :class="{ top: i < 3 }">{{ i + 1 }}</span>
          <div class="rank-main">
            <div class="rank-line">
              <span class="rank-name">{{ b.bookName }}</span>
              <span class="rank-count">{{ b.borrowCount }} 次</span>
            </div>
            <div class="rank-meta">
              {{ b.author }}
              <i v-if="b.categoryName" class="dot">·</i>
              <span v-if="b.categoryName">{{ b.categoryName }}</span>
            </div>
            <div class="rank-bar">
              <span :style="{ width: barWidth(b.borrowCount) }"></span>
            </div>
          </div>
        </li>
      </ul>
      <p v-else class="empty">暂无借阅记录</p>
    </section>

    <section class="note">
      <h2 class="note-title">指标口径</h2>
      <p>· <b>在借数量</b>指当前尚未归还的借阅记录条数，不含已归还与已逾期归还的记录。</p>
      <p>· <b>逾期数量</b>为在借且当前时间已超过应还时间的记录数，需重点跟进。</p>
      <p>· <b>在馆总库存</b>为全部上架图书的 <code>stock</code> 之和，不含已借出的册数。</p>
      <p>· <b>借还趋势</b>按 <code>borrow_date</code> / <code>return_date</code> 的自然日聚合，无记录的日期补 0。</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import EChart from '../components/EChart.vue'
import { getBorrowTrend, getCategoryDistribution, getOverview, getTopBooks } from '../api/stat'
import echarts, { BASE_TEXT_STYLE, CHART_COLORS } from '../utils/echarts'

const overview = ref({})
const trend = ref([])
const categories = ref([])
const topBooks = ref([])
const days = ref(30)
const ranges = [7, 30, 90]
const stamp = ref('—')

const cards = computed(() => {
  const o = overview.value
  return [
    {
      label: '读者总数',
      value: o.userCount ?? 0,
      unit: '人',
      glyph: '☺',
      caption: '已注册账号（含管理员）'
    },
    {
      label: '图书总数',
      value: o.bookCount ?? 0,
      unit: '种',
      glyph: '▤',
      caption: '在册书目品种数'
    },
    {
      label: '分类数量',
      value: o.categoryCount ?? 0,
      unit: '类',
      glyph: '❏',
      caption: '当前馆藏分类体系'
    },
    {
      label: '在借数量',
      value: o.activeBorrowCount ?? 0,
      unit: '本',
      glyph: '⇄',
      caption: '尚未归还的借阅记录'
    },
    {
      label: '逾期数量',
      value: o.overdueCount ?? 0,
      unit: '本',
      glyph: '!',
      caption: o.overdueCount > 0 ? '存在逾期，需催还' : '暂无逾期，状态良好',
      tone: o.overdueCount > 0 ? 'alert' : ''
    },
    {
      label: '在馆总库存',
      value: o.totalStock ?? 0,
      unit: '册',
      glyph: '▦',
      caption: '上架图书可借册数合计'
    }
  ]
})

const maxBorrow = computed(() => Math.max(1, ...topBooks.value.map(b => b.borrowCount)))
const barWidth = count => `${Math.max(6, Math.round((count / maxBorrow.value) * 100))}%`

const trendOption = computed(() => {
  // 天数多时轴标签会挤在一起，按区间抽稀
  const every = days.value <= 7 ? 0 : days.value <= 30 ? 4 : 14
  return {
    textStyle: BASE_TEXT_STYLE,
    color: ['#1f4b3a', '#e0ad3a'],
    grid: { left: 6, right: 14, top: 38, bottom: 2, containLabel: true },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(29,28,26,0.9)',
      borderWidth: 0,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: { type: 'line', lineStyle: { color: 'rgba(31,75,58,0.25)' } }
    },
    legend: {
      right: 2,
      top: 2,
      icon: 'roundRect',
      itemWidth: 10,
      itemHeight: 10,
      itemGap: 14,
      textStyle: { fontSize: 11.5, color: '#4b463f' }
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: trend.value.map(t => t.date.slice(5)),
      axisLine: { lineStyle: { color: '#e5dbc9' } },
      axisTick: { show: false },
      axisLabel: { color: '#8a8276', fontSize: 10.5, interval: every }
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#efe7d9' } },
      axisLabel: { color: '#8a8276', fontSize: 10.5 }
    },
    series: [
      {
        name: '借出',
        type: 'line',
        smooth: 0.2,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#1f4b3a', borderColor: '#fff', borderWidth: 1.5 },
        lineStyle: { width: 2.2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(31,75,58,0.22)' },
            { offset: 1, color: 'rgba(31,75,58,0.01)' }
          ])
        },
        data: trend.value.map(t => t.borrowCount)
      },
      {
        name: '归还',
        type: 'line',
        smooth: 0.2,
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#e0ad3a', borderColor: '#fff', borderWidth: 1.5 },
        lineStyle: { width: 2.2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(224,173,58,0.24)' },
            { offset: 1, color: 'rgba(224,173,58,0.01)' }
          ])
        },
        data: trend.value.map(t => t.returnCount)
      }
    ]
  }
})

const totalBooks = computed(() =>
  categories.value.reduce((sum, c) => sum + (c.bookCount || 0), 0)
)

const pieOption = computed(() => ({
  textStyle: BASE_TEXT_STYLE,
  color: CHART_COLORS,
  title: {
    text: String(totalBooks.value),
    subtext: '在册品种',
    left: 'center',
    top: '29%',
    textStyle: { fontSize: 22, fontWeight: 700, color: '#1d1c1a' },
    subtextStyle: { fontSize: 11, color: '#8a8276' }
  },
  tooltip: {
    trigger: 'item',
    backgroundColor: 'rgba(29,28,26,0.9)',
    borderWidth: 0,
    textStyle: { color: '#fff', fontSize: 12 },
    formatter: '{b}<br/>在册 {c} 种（{d}%）'
  },
  legend: {
    bottom: 0,
    left: 'center',
    width: '92%',
    icon: 'circle',
    itemWidth: 8,
    itemHeight: 8,
    itemGap: 9,
    textStyle: { fontSize: 10.5, color: '#4b463f' }
  },
  series: [
    {
      name: '馆藏分布',
      type: 'pie',
      radius: ['44%', '66%'],
      center: ['50%', '36%'],
      avoidLabelOverlap: true,
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      labelLine: { show: false },
      emphasis: {
        scaleSize: 6,
        label: { show: false },
        itemStyle: { shadowBlur: 12, shadowColor: 'rgba(31,75,58,0.25)' }
      },
      data: categories.value
        .filter(c => c.bookCount > 0)
        .map(c => ({ name: c.categoryName, value: c.bookCount }))
    }
  ]
}))

async function switchRange(r) {
  if (days.value === r) return
  days.value = r
  const res = await getBorrowTrend(r)
  trend.value = res.data || []
}

onMounted(async () => {
  const [ov, tr, cd, tb] = await Promise.all([
    getOverview(),
    getBorrowTrend(days.value),
    getCategoryDistribution(),
    getTopBooks(8)
  ])
  overview.value = ov.data || {}
  trend.value = tr.data || []
  categories.value = cd.data || []
  topBooks.value = tb.data || []
  const d = new Date()
  stamp.value = `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
})
</script>

<style scoped>
.dash {
  max-width: 1120px;
}

.dash-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.eyebrow {
  margin: 0 0 8px;
  font-size: 10.5px;
  letter-spacing: 0.28em;
  color: var(--ink-3);
  font-weight: 600;
}
.dash-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
}
.dash-sub {
  margin: 7px 0 0;
  font-size: 12.5px;
  color: var(--ink-3);
}
.stamp {
  font-size: 11.5px;
  color: var(--ink-3);
  letter-spacing: 0.06em;
  border: 1px solid var(--line);
  background: #fffdf9;
  border-radius: 999px;
  padding: 4px 12px;
  white-space: nowrap;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.stat {
  position: relative;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 18px 20px 16px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
}
.stat::before {
  /* 顶部一道极细的墨绿压条，作为系列标识 */
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 0;
  height: 2px;
  background: linear-gradient(90deg, var(--forest), rgba(31, 75, 58, 0.06));
  opacity: 0.65;
}
.stat:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.stat-top {
  display: flex;
  align-items: center;
  gap: 8px;
}
.glyph {
  width: 22px;
  height: 22px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--forest-mist);
  color: var(--forest);
  font-size: 12px;
  font-weight: 700;
}
.label {
  font-size: 12.5px;
  color: var(--ink-2);
  letter-spacing: 0.04em;
}

.value {
  margin: 12px 0 4px;
  font-size: 36px;
  line-height: 1;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.01em;
}
.value em {
  font-style: normal;
  font-size: 12px;
  font-weight: 500;
  color: var(--ink-3);
  margin-left: 6px;
  letter-spacing: 0.06em;
}

.caption {
  margin: 0;
  font-size: 11.5px;
  color: var(--ink-3);
}

/* 逾期告警态 */
.stat.alert {
  background: #fff9f6;
  border-color: #efd8d0;
}
.stat.alert::before {
  background: linear-gradient(90deg, #a2544a, rgba(162, 84, 74, 0.06));
}
.stat.alert .value {
  color: #a2544a;
}
.stat.alert .glyph {
  background: #f7e7e4;
  color: #a2544a;
}

/* ---------- 图表卡片 ---------- */
.chart-row {
  display: grid;
  grid-template-columns: 1.55fr 1fr;
  gap: 14px;
  margin-top: 14px;
}

.panel {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  padding: 16px 18px 12px;
  /* 关键：ECharts 会给 canvas 写死内联宽度，作为 grid item 会撑开轨道，
     导致右侧卡片溢出容器；min-width: 0 允许轨道收缩到 1fr 计算结果 */
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 6px;
}
.panel-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.02em;
}
.panel-sub {
  margin: 4px 0 0;
  font-size: 11.5px;
  color: var(--ink-3);
}
.hint {
  font-size: 11px;
  color: var(--ink-3);
  border: 1px solid var(--line-soft);
  background: #fffdf9;
  border-radius: 999px;
  padding: 2px 9px;
  white-space: nowrap;
}

/* 区间切换 */
.range {
  display: inline-flex;
  border: 1px solid var(--line);
  border-radius: 9px;
  overflow: hidden;
  background: #fffdf9;
}
.range button {
  border: 0;
  background: transparent;
  padding: 5px 11px;
  font-size: 11.5px;
  color: var(--ink-3);
  cursor: pointer;
  transition:
    background 0.18s,
    color 0.18s;
}
.range button + button {
  border-left: 1px solid var(--line);
}
.range button:hover {
  color: var(--forest);
}
.range button.on {
  background: var(--forest);
  color: #fff;
}

/* ---------- 排行榜 ---------- */
.rank {
  margin-top: 14px;
  padding-bottom: 8px;
}
.rank-list {
  list-style: none;
  margin: 10px 0 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 4px 26px;
}
.rank-item {
  display: flex;
  gap: 12px;
  padding: 9px 4px;
  border-bottom: 1px dashed var(--line-soft);
}
.rank-item:last-child {
  border-bottom: 0;
}
.rank-no {
  flex: none;
  width: 22px;
  height: 22px;
  margin-top: 2px;
  display: grid;
  place-items: center;
  border-radius: 7px;
  background: var(--paper-2);
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
}
.rank-no.top {
  background: var(--gold-soft);
  color: var(--gold-deep);
}
.rank-main {
  flex: 1;
  min-width: 0;
}
.rank-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}
.rank-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.rank-count {
  flex: none;
  font-size: 11.5px;
  color: var(--forest);
  font-weight: 600;
}
.rank-meta {
  margin-top: 2px;
  font-size: 11px;
  color: var(--ink-3);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.dot {
  margin: 0 4px;
  font-style: normal;
}
.rank-bar {
  margin-top: 6px;
  height: 3px;
  border-radius: 999px;
  background: var(--paper-2);
  overflow: hidden;
}
.rank-bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--forest), var(--gold));
}
.empty {
  margin: 18px 4px 14px;
  font-size: 12.5px;
  color: var(--ink-3);
}

/* 说明 */
.note {
  margin-top: 14px;
  padding: 18px 22px 20px;
  border-radius: 14px;
  background: #fffdf9;
  border: 1px solid var(--line);
  border-left: 3px solid var(--gold);
}
.note-title {
  font-size: 13px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.08em;
  margin-bottom: 8px;
}
.note p {
  margin: 5px 0;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-2);
}
.note b {
  color: var(--ink);
  font-weight: 600;
}
.note code {
  background: var(--paper-2);
  border-radius: 4px;
  padding: 1px 5px;
  font-size: 11.5px;
  color: var(--forest);
}

@media (max-width: 980px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
  .rank-list {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 900px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
@media (max-width: 560px) {
  .stat-grid {
    grid-template-columns: 1fr;
  }
}
</style>
