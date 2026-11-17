<template>
  <div class="portal">
    <!-- ================= 检索主视觉 ================= -->
    <section class="hero">
      <div class="hero-deco" aria-hidden="true">
        <span class="ring ring-1"></span>
        <span class="ring ring-2"></span>
        <span class="hero-glyph display">書</span>
      </div>

      <div class="hero-inner">
        <p class="hero-eyebrow">LIBRARY CATALOG</p>
        <h1 class="hero-title display">馆藏检索</h1>
        <p class="hero-sub">支持 书名 / 作者 / ISBN / 分类 多条件组合模糊检索</p>

        <div class="search-row">
          <el-select
            v-model="query.categoryId"
            placeholder="全部分类"
            class="s-cat"
            clearable
            @change="search"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>

          <el-input
            v-model="query.keyword"
            class="s-key"
            placeholder="请输入书名、作者或 ISBN 关键词"
            clearable
            @keyup.enter="search"
            @clear="search"
          />

          <button class="btn-gold s-btn" @click="search">检 索</button>
        </div>

        <div class="stats">
          <span class="stat-pill"
            >馆藏 <b>{{ overview.bookCount }}</b> 种</span
          >
          <span class="stat-pill"
            >可借 <b>{{ overview.stockTotal }}</b> 册</span
          >
          <span class="stat-pill"
            >读者 <b>{{ overview.readerCount }}</b> 人</span
          >
        </div>
      </div>
    </section>

    <!-- ================= 图书分类 ================= -->
    <div class="section-head">
      <h2 class="section-title">
        图书分类
        <span class="count">{{ categories.length }} 类</span>
      </h2>
      <a class="link-more" @click="clearAll">查看全部图书 →</a>
    </div>

    <div class="cat-grid" v-loading="catLoading">
      <button
        v-for="c in categories.slice(0, 6)"
        :key="c.id"
        class="cat-card"
        :class="{ active: query.categoryId === c.id }"
        @click="pickCategory(c)"
      >
        <span class="cat-name">{{ c.name }}</span>
        <span class="cat-meta">{{ c.description || '馆藏分类' }}</span>
      </button>
    </div>

    <!-- ================= 图书网格 ================= -->
    <div class="section-head">
      <h2 class="section-title">
        {{ isSearching ? '检索结果' : '新书速递' }}
        <span class="count">共 {{ total }} 种</span>
      </h2>
      <span class="link-more" v-if="isSearching" @click="clearAll">清除筛选</span>
    </div>

    <div class="book-grid" v-loading="loading">
      <article v-for="b in list" :key="b.id" class="book-card">
        <div
          class="cover"
          :class="{ 'has-cover': !!b.coverUrl }"
          :style="b.coverUrl ? null : { background: coverColor(b.id) }"
        >
          <img
            v-if="b.coverUrl"
            class="cover-img"
            :src="b.coverUrl"
            :alt="`${b.name} 封面`"
            loading="lazy"
            @error="b.coverUrl = ''"
          />
          <span class="spine" aria-hidden="true"></span>
          <template v-if="!b.coverUrl">
            <span class="cover-title">{{ b.name }}</span>
            <span class="cover-isbn">{{ b.isbn }}</span>
          </template>
          <span v-if="b.stock <= 0" class="cover-flag">已借完</span>
        </div>

        <div class="book-body">
          <h3 class="book-name" :title="b.name">{{ b.name }}</h3>
          <p class="book-meta">{{ b.author || '佚名' }} · {{ b.categoryName || '未分类' }}</p>
          <div class="book-foot">
            <span class="stock">
              可借 <b>{{ b.stock }}</b> / {{ (b.stock || 0) + (b.borrowed || 0) }}
            </span>
            <button class="detail-btn" @click="openDetail(b)">详情</button>
          </div>
        </div>
      </article>
    </div>

    <el-empty
      v-if="!loading && list.length === 0"
      description="没有找到符合条件的图书，换个关键词试试"
    />

    <div class="pager" v-if="total > query.pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
        :total="total"
        :current-page="query.pageNum"
        :page-size="query.pageSize"
        @current-change="
          p => {
            query.pageNum = p
            load()
          }
        "
      />
    </div>

    <!-- ================= 详情 ================= -->
    <el-dialog v-model="detailVisible" width="620px" title="图书详情" class="detail-dialog">
      <div v-if="current" class="detail">
        <div
          class="detail-cover"
          :class="{ 'has-cover': !!current.coverUrl }"
          :style="current.coverUrl ? null : { background: coverColor(current.id) }"
        >
          <img
            v-if="current.coverUrl"
            class="cover-img"
            :src="current.coverUrl"
            :alt="`${current.name} 封面`"
            @error="current.coverUrl = ''"
          />
          <span class="spine" aria-hidden="true"></span>
          <span v-if="!current.coverUrl" class="cover-title">{{ current.name }}</span>
        </div>

        <div class="detail-info">
          <h2 class="detail-title">{{ current.name }}</h2>
          <p class="detail-sub">{{ current.author || '佚名' }} 著</p>

          <dl class="detail-meta">
            <div>
              <dt>分类</dt>
              <dd>{{ current.categoryName || '未分类' }}</dd>
            </div>
            <div>
              <dt>出版社</dt>
              <dd>{{ current.publisher || '—' }}</dd>
            </div>
            <div>
              <dt>ISBN</dt>
              <dd>{{ current.isbn }}</dd>
            </div>
            <div>
              <dt>定价</dt>
              <dd>¥ {{ current.price ?? '—' }}</dd>
            </div>
            <div>
              <dt>馆藏</dt>
              <dd>
                可借 {{ current.stock }} 册 / 共
                {{ (current.stock || 0) + (current.borrowed || 0) }} 册
              </dd>
            </div>
            <div v-if="reso.waitingCount > 0 || reso.readyCount > 0">
              <dt>预约</dt>
              <dd>
                {{ reso.waitingCount }} 人排队<template v-if="reso.readyCount > 0"
                  >，{{ reso.readyCount }} 人待取书</template
                >
              </dd>
            </div>
          </dl>

          <p class="detail-desc">{{ current.description || '暂无简介。' }}</p>

          <div class="detail-actions">
            <!-- 可借：直接借；待取书：锁定给他的副本已备好，也能借 -->
            <button v-if="canBorrow" class="btn-gold" @click="handleBorrow(current)">借 阅</button>

            <!-- 已借完且未预约：走排队 -->
            <button
              v-else-if="current.status === 1 && reso.myStatus === null"
              class="btn-gold"
              :disabled="reserving"
              @click="handleReserve(current)"
            >
              预约排队{{ reso.waitingCount > 0 ? `（前方 ${reso.waitingCount} 人）` : '' }}
            </button>

            <!-- 排队中 -->
            <template v-else-if="reso.myStatus === 0">
              <span class="reso-state">
                排队中 · 第 <b>{{ reso.myPosition }}</b> 位
              </span>
              <el-button plain @click="handleCancelReserve">取消预约</el-button>
            </template>

            <!-- 待取书但已过保留期（部分极端时序下 reserved 副本刚被回收） -->
            <span v-else-if="reso.myStatus === 1" class="reso-state ok">
              已到馆，保留至 {{ (reso.myExpireTime || '').slice(0, 10) }}
            </span>

            <span v-else-if="current.status !== 1" class="reso-state">已下架，暂不可借阅</span>

            <el-button plain @click="handleFavorite(current)">加入收藏</el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookPage } from '../api/book'
import { listCategories } from '../api/category'
import { getPortalOverview } from '../api/portal'
import { borrowBook } from '../api/borrow'
import { addFavorite } from '../api/favorite'
import { getBookReservation, reserveBook, cancelReservation } from '../api/reservation'

const query = reactive({ pageNum: 1, pageSize: 12, keyword: '', categoryId: null })
const list = ref([])
const total = ref(0)
const categories = ref([])
const overview = reactive({ bookCount: 0, stockTotal: 0, readerCount: 0 })
const loading = ref(false)
const catLoading = ref(false)
const detailVisible = ref(false)
const current = ref(null)

// 当前详情页对应书的预约态：排队人数 + 我的状态（null 表示未预约）
const reso = reactive({
  waitingCount: 0,
  readyCount: 0,
  myStatus: null,
  myReservationId: null,
  myPosition: null,
  myExpireTime: null,
  holdDays: 7
})
const reserving = ref(false)

/**
 * 能不能直接借：上架 &（有可借库存 或 有锁定给我的待取副本）。
 * 第二种情况的 stock 是 0（副本被挪进了 held），只看 stock 会让预约者以为借不了。
 */
const canBorrow = computed(
  () => current.value?.status === 1 && (current.value?.stock > 0 || reso.myStatus === 1)
)

// 检索态：有关键字或选了分类
const isSearching = computed(() => !!query.keyword || !!query.categoryId)

// 书封配色按 id 取模，保证同一本书颜色稳定
const COVERS = ['#2e6b53', '#3c6e9c', '#9c7b2e', '#6a5b93', '#a2544a', '#46707a']
const coverColor = id => COVERS[Number(id ?? 0) % COVERS.length]

async function load() {
  loading.value = true
  try {
    const res = await getBookPage(query)
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  catLoading.value = true
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } finally {
    catLoading.value = false
  }
}

async function loadOverview() {
  try {
    const res = await getPortalOverview()
    Object.assign(overview, res.data)
  } catch {
    /* 概览失败不影响检索主流程 */
  }
}

function search() {
  query.pageNum = 1
  load()
}

function pickCategory(c) {
  query.categoryId = query.categoryId === c.id ? null : c.id
  search()
}

function clearAll() {
  query.keyword = ''
  query.categoryId = null
  search()
}

function openDetail(book) {
  current.value = book
  detailVisible.value = true
  loadReservation(book.id)
}

async function loadReservation(bookId) {
  Object.assign(reso, {
    waitingCount: 0,
    readyCount: 0,
    myStatus: null,
    myReservationId: null,
    myPosition: null,
    myExpireTime: null
  })
  try {
    const res = await getBookReservation(bookId)
    Object.assign(reso, res.data || {})
  } catch {
    /* 预约态是增强信息，拿不到也不该让详情页打不开 */
  }
}

async function handleReserve(book) {
  reserving.value = true
  try {
    await ElMessageBox.confirm(
      `《${book.name}》当前已全部借出。预约后将进入队列，有副本归还时会给你发到馆通知，` +
        `到馆后保留 ${reso.holdDays || 7} 天。`,
      '预约确认',
      { confirmButtonText: '确认预约', cancelButtonText: '再等等' }
    )
    await reserveBook(book.id)
    ElMessage.success('预约成功，到馆后会通知你')
    await loadReservation(book.id)
  } finally {
    reserving.value = false
  }
}

async function handleCancelReserve() {
  await ElMessageBox.confirm('确定取消这次预约吗？排队位置不会保留。', '取消预约')
  await cancelReservation(reso.myReservationId)
  ElMessage.success('已取消预约')
  await loadReservation(current.value.id)
}

async function handleBorrow(book) {
  await ElMessageBox.confirm(`确定借阅《${book.name}》吗？借期 30 天，可续借 1 次。`, '借阅确认', {
    confirmButtonText: '确认借阅',
    cancelButtonText: '再想想'
  })
  await borrowBook(book.id)
  ElMessage.success('借阅成功，可在「我的借阅」查看')
  detailVisible.value = false
  load()
  loadOverview()
}

async function handleFavorite(book) {
  await addFavorite(book.id)
  ElMessage.success('已加入收藏')
}

onMounted(() => {
  loadCategories()
  load()
  loadOverview()
})
</script>

<style scoped>
.portal {
  max-width: 1120px;
  margin: 0 auto;
  padding: 4px 0 56px;
}

/* ---------------- 主视觉 ---------------- */
.hero {
  position: relative;
  overflow: hidden;
  border-radius: 16px;
  background: linear-gradient(135deg, #24503e 0%, #1f4b3a 46%, #17392c 100%);
  box-shadow: 0 20px 44px -28px rgba(22, 58, 44, 0.85);
  isolation: isolate;
}

.hero::after {
  /* 顶部一道极淡的暖光，避免大面积深绿显闷 */
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 80% at 12% 0%, rgba(224, 173, 58, 0.14), transparent 60%);
  pointer-events: none;
}

.hero-inner {
  position: relative;
  z-index: 2;
  padding: 34px 40px 30px;
}

.hero-eyebrow {
  margin: 0 0 10px;
  font-size: 11px;
  letter-spacing: 0.28em;
  color: rgba(244, 239, 228, 0.58);
  font-weight: 600;
}

.hero-title {
  font-size: 30px;
  line-height: 1.25;
  color: #fdfaf3;
  font-weight: 700;
}

.hero-sub {
  margin: 8px 0 22px;
  font-size: 13px;
  color: rgba(244, 239, 228, 0.72);
}

.search-row {
  display: flex;
  gap: 10px;
  max-width: 720px;
}

.s-cat {
  width: 150px;
  flex: none;
}
.s-key {
  flex: 1;
}
.s-btn {
  flex: none;
  height: 40px;
}

/* 搜索框在深底上要"浮"起来 */
.search-row :deep(.el-select__wrapper),
.search-row :deep(.el-input__wrapper) {
  background: #fff !important;
  box-shadow: none !important;
  border-radius: 8px !important;
  min-height: 40px;
}

.stats {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 20px;
}

.stat-pill {
  font-size: 12.5px;
  color: rgba(250, 246, 237, 0.9);
  background: rgba(250, 246, 237, 0.12);
  border: 1px solid rgba(250, 246, 237, 0.16);
  padding: 4px 12px;
  border-radius: 999px;
  backdrop-filter: blur(2px);
}
.stat-pill b {
  color: var(--gold-soft);
  font-weight: 700;
  margin: 0 2px;
}

/* 装饰：同心圆 + 大字 */
.hero-deco {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}
.ring {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(224, 173, 58, 0.18);
}
.ring-1 {
  width: 320px;
  height: 320px;
  right: -70px;
  top: -120px;
}
.ring-2 {
  width: 200px;
  height: 200px;
  right: 10px;
  top: -60px;
  border-color: rgba(224, 173, 58, 0.12);
}
.hero-glyph {
  position: absolute;
  right: 42px;
  bottom: -46px;
  font-size: 168px;
  line-height: 1;
  color: rgba(250, 246, 237, 0.05);
  user-select: none;
}

/* ---------------- 分类卡 ---------------- */
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(168px, 1fr));
  gap: 12px;
}

.cat-card {
  text-align: left;
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition:
    transform 0.18s,
    box-shadow 0.18s,
    border-color 0.18s;
  font-family: inherit;
}
.cat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
  border-color: #d8c9b0;
}
.cat-card.active {
  border-color: var(--forest);
  background: #f6faf7;
  box-shadow: 0 0 0 1px var(--forest) inset;
}

.cat-name {
  display: block;
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.02em;
}
.cat-meta {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: var(--ink-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------------- 图书卡 ---------------- */
.book-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(196px, 1fr));
  gap: 16px;
  min-height: 120px;
}

.book-card {
  background: var(--card);
  border: 1px solid var(--line);
  border-radius: 14px;
  overflow: hidden;
  transition:
    transform 0.2s,
    box-shadow 0.2s;
  display: flex;
  flex-direction: column;
}
.book-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-lg);
}

.cover {
  position: relative;
  /* 3:4 是出版物常用开本，也让真实封面图无需裁切即可完整展示 */
  aspect-ratio: 3 / 4;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px 14px;
  overflow: hidden;
  background: #22332c;
}
/* 真实封面：铺满并按顶端对齐裁切，底部留白自然被裁掉 */
.cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  display: block;
}
.cover.has-cover {
  padding: 0;
}
.cover.has-cover .spine {
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.4), rgba(0, 0, 0, 0));
}
/* 书脊：左侧一道暗色 + 金色细线 */
.spine {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 7px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.28), rgba(0, 0, 0, 0));
  border-right: 1px solid rgba(246, 227, 180, 0.22);
}
.cover-title {
  font-size: 17px;
  font-weight: 700;
  color: #f8f3e6;
  text-align: center;
  letter-spacing: 0.04em;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.16);
}
.cover-isbn {
  position: absolute;
  bottom: 10px;
  right: 10px;
  font-size: 9.5px;
  letter-spacing: 0.08em;
  color: rgba(248, 243, 230, 0.5);
}
.cover-flag {
  position: absolute;
  top: 10px;
  left: 14px;
  font-size: 10px;
  padding: 2px 7px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.32);
  color: #f3e6c8;
}

.book-body {
  padding: 13px 15px 15px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  flex: 1;
}

.book-name {
  font-size: 14.5px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.book-meta {
  margin: 0;
  font-size: 12px;
  color: var(--ink-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.book-foot {
  margin-top: auto;
  padding-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.stock {
  font-size: 11.5px;
  color: var(--ink-3);
}
.stock b {
  color: var(--forest);
  font-weight: 700;
  font-size: 13px;
}

.detail-btn {
  appearance: none;
  border: none;
  background: var(--forest);
  color: #f7f2e4;
  font-size: 12.5px;
  font-weight: 500;
  padding: 5px 15px;
  border-radius: 7px;
  cursor: pointer;
  transition:
    background 0.16s,
    transform 0.16s;
  font-family: inherit;
}
.detail-btn:hover {
  background: var(--forest-soft);
}
.detail-btn:active {
  transform: scale(0.97);
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

/* ---------------- 详情弹窗 ---------------- */
.detail {
  display: flex;
  gap: 22px;
}

.detail-cover {
  position: relative;
  flex: none;
  width: 168px;
  aspect-ratio: 3 / 4;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 14px;
  overflow: hidden;
  box-shadow: 0 14px 28px -18px rgba(0, 0, 0, 0.6);
}
.detail-cover.has-cover {
  padding: 0;
}

.detail-info {
  flex: 1;
  min-width: 0;
}

.detail-title {
  font-size: 20px;
  line-height: 1.35;
}
.detail-sub {
  margin: 6px 0 14px;
  font-size: 13px;
  color: var(--ink-3);
}

.detail-meta {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 18px;
}
.detail-meta > div {
  display: flex;
  gap: 8px;
  font-size: 13px;
}
.detail-meta dt {
  color: var(--ink-3);
  flex: none;
}
.detail-meta dd {
  margin: 0;
  color: var(--ink-2);
}

.detail-desc {
  margin: 14px 0 18px;
  font-size: 13px;
  line-height: 1.7;
  color: var(--ink-2);
  max-height: 96px;
  overflow: auto;
}

.detail-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.btn-gold.disabled {
  background: #e8e0d2;
  color: #a79c8a;
  box-shadow: none;
  cursor: not-allowed;
}

/* 预约态说明文字：比按钮弱一级，不抢主操作 */
.reso-state {
  font-size: 12.5px;
  color: var(--ink-3);
  padding: 6px 12px;
  border-radius: 8px;
  background: var(--paper-2);
  border: 1px dashed var(--line);
}
.reso-state b {
  color: var(--forest);
  font-size: 13.5px;
}
.reso-state.ok {
  color: var(--forest);
  background: #f2f8f4;
  border-color: #cfe3d7;
  border-style: solid;
}

@media (max-width: 720px) {
  .hero-inner {
    padding: 26px 20px 24px;
  }
  .hero-title {
    font-size: 24px;
  }
  .search-row {
    flex-direction: column;
  }
  .s-cat {
    width: 100%;
  }
  .detail {
    flex-direction: column;
  }
  .detail-cover {
    width: 100%;
    max-width: 200px;
    margin: 0 auto;
  }
}
</style>
