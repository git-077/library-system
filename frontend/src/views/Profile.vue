<template>
  <div class="profile">
    <!-- 个人名片 -->
    <section class="id-card">
      <div class="avatar-lg">{{ initial }}</div>

      <div class="who">
        <div class="name-row">
          <h1 class="name">{{ me.realName || me.username || store.username }}</h1>
          <span class="badge">{{ store.isAdmin ? '管理员' : '读者' }}</span>
        </div>
        <p class="uid">@{{ me.username || store.username }}</p>
      </div>

      <div class="quick">
        <div class="q">
          <b>{{ stats.borrowing }}</b>
          <span>在借</span>
        </div>
        <div class="q">
          <b :class="{ warn: stats.overdue > 0 }">{{ stats.overdue }}</b>
          <span>逾期</span>
        </div>
        <div class="q">
          <b>{{ stats.favorites }}</b>
          <span>收藏</span>
        </div>
      </div>
    </section>

    <!-- 切换 -->
    <div class="switcher">
      <button :class="{ on: activeTab === 'borrows' }" @click="activeTab = 'borrows'">
        借阅记录
        <span class="n">{{ borrowTotal }}</span>
      </button>
      <button :class="{ on: activeTab === 'favorites' }" @click="activeTab = 'favorites'">
        我的收藏
        <span class="n">{{ favList.length }}</span>
      </button>
    </div>

    <!-- 借阅记录 -->
    <section v-show="activeTab === 'borrows'" class="panel body">
      <el-table :data="borrowList" v-loading="borrowLoading" stripe>
        <el-table-column prop="bookName" label="书名" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="book-cell">{{ row.bookName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="borrowDate" label="借出时间" width="160" />
        <el-table-column prop="dueDate" label="应还时间" width="160">
          <template #default="{ row }">
            <span :class="{ overdue: isOverdue(row) }">{{ row.dueDate }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="returnDate" label="归还时间" width="160">
          <template #default="{ row }">{{ row.returnDate || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="statusType(row)" size="small">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button type="primary" size="small" @click="handleReturn(row)">归还</el-button>
              <el-button size="small" plain @click="handleRenew(row)">续借</el-button>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!borrowLoading && borrowList.length === 0"
        description="还没有借阅记录，去馆藏检索挑一本吧"
      />

      <div class="pager" v-if="borrowTotal > borrowQuery.pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="borrowTotal"
          :current-page="borrowQuery.pageNum"
          :page-size="borrowQuery.pageSize"
          @current-change="
            p => {
              borrowQuery.pageNum = p
              loadBorrows()
            }
          "
        />
      </div>
    </section>

    <!-- 收藏 -->
    <section v-show="activeTab === 'favorites'" class="panel body">
      <div class="fav-grid" v-loading="favLoading">
        <article v-for="b in favList" :key="b.id" class="fav-card">
          <div class="fav-cover" :style="{ background: coverColor(b.id) }">
            <span class="spine" aria-hidden="true"></span>
            <span class="fav-title">{{ b.name }}</span>
          </div>
          <div class="fav-info">
            <h3 :title="b.name">{{ b.name }}</h3>
            <p>{{ b.author || '佚名' }} · {{ b.categoryName || '未分类' }}</p>
            <div class="fav-actions">
              <el-button
                type="primary"
                size="small"
                :disabled="b.status !== 1 || b.stock <= 0"
                @click="handleBorrow(b)"
              >
                借阅
              </el-button>
              <el-button size="small" text @click="handleRemoveFav(b)">取消收藏</el-button>
            </div>
          </div>
        </article>
      </div>

      <el-empty
        v-if="!favLoading && favList.length === 0"
        description="暂无收藏，去馆藏检索页收藏感兴趣的图书吧"
      />
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMe } from '../api/auth'
import { getMyBorrows, borrowBook, returnBook, renewBook } from '../api/borrow'
import { getMyFavorites, removeFavorite } from '../api/favorite'
import { useUserStore } from '../stores/user'
import { borrowStatus, borrowStatusType } from '../utils'

const store = useUserStore()
const activeTab = ref('borrows')
const me = ref({})

const borrowQuery = reactive({ pageNum: 1, pageSize: 10 })
const borrowList = ref([])
const borrowTotal = ref(0)
const borrowLoading = ref(false)

const favList = ref([])
const favLoading = ref(false)

const stats = reactive({ borrowing: 0, overdue: 0, favorites: 0 })

const statusText = borrowStatus
const statusType = borrowStatusType

const initial = computed(() =>
  (me.value.realName || me.value.username || store.username || '?').charAt(0).toUpperCase()
)

const COVERS = ['#2e6b53', '#3c6e9c', '#9c7b2e', '#6a5b93', '#a2544a', '#46707a']
const coverColor = id => COVERS[Number(id ?? 0) % COVERS.length]

const isOverdue = row =>
  row.status === 0 && row.dueDate && new Date(row.dueDate.replace(/-/g, '/')) < new Date()

async function loadMe() {
  const res = await getMe()
  me.value = res.data || {}
}

async function loadBorrows() {
  borrowLoading.value = true
  try {
    const res = await getMyBorrows(borrowQuery)
    borrowList.value = res.data.records || []
    borrowTotal.value = Number(res.data.total || 0)
  } finally {
    borrowLoading.value = false
  }
}

/** 汇总统计：单独拉一次大页数据算，避免只统计当前页导致数字不准 */
async function loadStats() {
  try {
    const res = await getMyBorrows({ pageNum: 1, pageSize: 200 })
    const rows = res.data.records || []
    stats.borrowing = rows.filter(r => r.status === 0).length
    stats.overdue = rows.filter(isOverdue).length
  } catch {
    /* 统计失败不影响主流程 */
  }
}

async function loadFavorites() {
  favLoading.value = true
  try {
    const res = await getMyFavorites()
    favList.value = res.data || []
    stats.favorites = favList.value.length
  } finally {
    favLoading.value = false
  }
}

async function handleReturn(row) {
  await ElMessageBox.confirm(`确定归还《${row.bookName}》吗？`, '归还确认')
  await returnBook(row.id)
  ElMessage.success('归还成功')
  loadBorrows()
  loadStats()
}

async function handleRenew(row) {
  await ElMessageBox.confirm(
    `《${row.bookName}》续借后应还时间将顺延 30 天，确定续借吗？`,
    '续借确认'
  )
  await renewBook(row.id)
  ElMessage.success('续借成功')
  loadBorrows()
  loadStats()
}

async function handleBorrow(row) {
  await ElMessageBox.confirm(`确定借阅《${row.name}》吗？`, '借阅确认')
  await borrowBook(row.id)
  ElMessage.success('借阅成功')
  loadFavorites()
  loadBorrows()
  loadStats()
}

async function handleRemoveFav(row) {
  await ElMessageBox.confirm(`确定取消收藏《${row.name}》吗？`, '取消收藏')
  await removeFavorite(row.id)
  ElMessage.success('已取消收藏')
  loadFavorites()
}

onMounted(() => {
  loadMe()
  loadBorrows()
  loadStats()
  loadFavorites()
})
</script>

<style scoped>
.profile {
  max-width: 1120px;
  margin: 0 auto;
}

/* ---------------- 名片 ---------------- */
.id-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 16px;
  background: linear-gradient(120deg, #24503e 0%, #1f4b3a 48%, #17392c 100%);
  color: #f7f2e4;
  box-shadow: 0 20px 44px -30px rgba(22, 58, 44, 0.9);
}

.avatar-lg {
  width: 58px;
  height: 58px;
  flex: none;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(246, 227, 180, 0.14);
  border: 1px solid rgba(246, 227, 180, 0.3);
  color: var(--gold-soft);
  font-size: 22px;
  font-weight: 700;
}

.who {
  min-width: 0;
}
.name-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.name {
  font-size: 21px;
  color: #fdfaf2;
  font-weight: 700;
  letter-spacing: 0.02em;
}
.badge {
  font-size: 10.5px;
  letter-spacing: 0.08em;
  padding: 2px 9px;
  border-radius: 999px;
  background: rgba(224, 173, 58, 0.18);
  border: 1px solid rgba(224, 173, 58, 0.34);
  color: var(--gold-soft);
}
.uid {
  margin: 5px 0 0;
  font-size: 12px;
  color: rgba(250, 246, 237, 0.55);
  letter-spacing: 0.04em;
}

.quick {
  margin-left: auto;
  display: flex;
  gap: 6px;
}
.q {
  min-width: 74px;
  text-align: center;
  padding: 8px 4px;
  border-radius: 10px;
  background: rgba(250, 246, 237, 0.08);
  border: 1px solid rgba(250, 246, 237, 0.12);
}
.q b {
  display: block;
  font-size: 19px;
  color: #fdfaf2;
  font-weight: 700;
  line-height: 1.2;
}
.q b.warn {
  color: #f0b7a8;
}
.q span {
  font-size: 11px;
  color: rgba(250, 246, 237, 0.6);
  letter-spacing: 0.1em;
}

/* ---------------- 切换 ---------------- */
.switcher {
  display: flex;
  gap: 24px;
  margin: 28px 0 14px;
  border-bottom: 1px solid var(--line);
}
.switcher button {
  position: relative;
  appearance: none;
  border: none;
  background: transparent;
  padding: 0 2px 11px;
  font-size: 14.5px;
  font-family: inherit;
  color: var(--ink-3);
  cursor: pointer;
  transition: color 0.18s;
}
.switcher button.on {
  color: var(--ink);
  font-weight: 700;
}
.switcher button.on::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--forest);
}
.switcher .n {
  margin-left: 6px;
  font-size: 11.5px;
  color: var(--ink-3);
  background: var(--paper-2);
  border-radius: 999px;
  padding: 1px 7px;
}

.body {
  padding: 8px 18px 16px;
}

.book-cell {
  font-weight: 600;
  color: var(--ink);
}
.overdue {
  color: #a2544a;
  font-weight: 600;
}
.muted {
  color: var(--ink-3);
}
.pager {
  display: flex;
  justify-content: flex-end;
  padding: 14px 0 4px;
}

/* ---------------- 收藏 ---------------- */
.fav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
  padding: 14px 2px 4px;
}

.fav-card {
  display: flex;
  gap: 14px;
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: #fffdf9;
  transition:
    box-shadow 0.18s,
    transform 0.18s;
}
.fav-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow);
}

.fav-cover {
  position: relative;
  width: 62px;
  aspect-ratio: 3 / 4;
  flex: none;
  border-radius: 6px;
  overflow: hidden;
  display: grid;
  place-items: center;
  padding: 6px;
}
.spine {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: linear-gradient(90deg, rgba(0, 0, 0, 0.3), transparent);
}
.fav-title {
  font-size: 9.5px;
  line-height: 1.3;
  color: #f8f3e6;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.fav-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.fav-info h3 {
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-info p {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--ink-3);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fav-actions {
  margin-top: auto;
  padding-top: 8px;
  display: flex;
  gap: 6px;
  align-items: center;
}

@media (max-width: 720px) {
  .id-card {
    flex-wrap: wrap;
  }
  .quick {
    width: 100%;
    margin-left: 0;
    justify-content: space-between;
  }
}
</style>
