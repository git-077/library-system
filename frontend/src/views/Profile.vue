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
      <button :class="{ on: activeTab === 'reservations' }" @click="activeTab = 'reservations'">
        我的预约
        <span class="n">{{ activeReservations }}</span>
      </button>
      <button :class="{ on: activeTab === 'notifications' }" @click="activeTab = 'notifications'">
        消息通知
        <span class="n" :class="{ hot: unreadCount > 0 }">{{ unreadCount }}</span>
      </button>
      <button :class="{ on: activeTab === 'favorites' }" @click="activeTab = 'favorites'">
        我的收藏
        <span class="n">{{ favList.length }}</span>
      </button>
      <button :class="{ on: activeTab === 'security' }" @click="activeTab = 'security'">
        账户安全
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

    <!-- 我的预约 -->
    <section v-show="activeTab === 'reservations'" class="panel body">
      <el-table :data="resoList" v-loading="resoLoading" stripe>
        <el-table-column label="书名" min-width="200">
          <template #default="{ row }">
            <div class="reso-book">
              <span
                class="reso-cover"
                :style="row.coverUrl ? null : { background: coverColor(row.bookId) }"
              >
                <img v-if="row.coverUrl" :src="row.coverUrl" alt="" />
                <i v-else>{{ (row.bookName || '?').charAt(0) }}</i>
              </span>
              <span class="reso-text">
                <b class="book-cell">{{ row.bookName || `图书#${row.bookId}` }}</b>
                <em>{{ row.bookAuthor || '佚名' }}</em>
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="104">
          <template #default="{ row }">
            <el-tag :type="resoTagType(row.status)" size="small">{{ resoText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="进度" width="184">
          <template #default="{ row }">
            <span v-if="row.status === 0" class="queue-pos"
              >第 <b>{{ row.position }}</b> 位</span
            >
            <span v-else-if="row.status === 1" class="ok-text">
              保留至 {{ (row.expireTime || '').slice(0, 10) }}
            </span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column prop="createTime" label="预约时间" width="170">
          <template #default="{ row }">{{ prettyTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="176" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 1">
              <el-button type="primary" size="small" @click="handleTakeReserved(row)"
                >立即借阅</el-button
              >
              <el-button size="small" plain @click="handleCancelReso(row)">取消</el-button>
            </template>
            <el-button
              v-else-if="row.status === 0"
              size="small"
              plain
              @click="handleCancelReso(row)"
            >
              取消预约
            </el-button>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!resoLoading && resoList.length === 0"
        description="没有进行中的预约。馆藏全部借完的书可以预约排队，到馆后会自动通知你"
      />
    </section>

    <!-- 消息通知 -->
    <section v-show="activeTab === 'notifications'" class="panel body">
      <div class="note-toolbar">
        <span>
          共 <b>{{ noteTotal }}</b> 条，其中
          <b :class="{ warn: unreadCount > 0 }">{{ unreadCount }}</b> 条未读
        </span>
        <el-button size="small" text :disabled="unreadCount === 0" @click="handleReadAllNotes">
          全部标为已读
        </el-button>
      </div>

      <div class="note-list" v-loading="noteLoading">
        <article
          v-for="n in noteList"
          :key="n.id"
          class="note-item"
          :class="{ unread: n.isRead === 0 }"
          @click="handleReadNote(n)"
        >
          <span class="note-dot" :class="noteDot(n.type)"></span>
          <div class="note-main">
            <div class="note-head">
              <b>{{ n.title }}</b>
              <time>{{ prettyTime(n.createTime) }}</time>
            </div>
            <p>{{ n.content }}</p>
          </div>
          <span v-if="n.isRead === 0" class="note-flag">未读</span>
        </article>
      </div>

      <el-empty
        v-if="!noteLoading && noteList.length === 0"
        description="暂无消息。预约到书、借阅到期前，都会在这里提醒你"
      />

      <div class="pager" v-if="noteTotal > noteQuery.pageSize">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="noteTotal"
          :current-page="noteQuery.pageNum"
          :page-size="noteQuery.pageSize"
          @current-change="
            p => {
              noteQuery.pageNum = p
              loadNotes()
            }
          "
        />
      </div>
    </section>

    <!-- 收藏 -->
    <section v-show="activeTab === 'favorites'" class="panel body">
      <div class="fav-grid" v-loading="favLoading">
        <article v-for="b in favList" :key="b.id" class="fav-card">
          <div
            class="fav-cover"
            :class="{ 'has-cover': !!b.coverUrl }"
            :style="b.coverUrl ? null : { background: coverColor(b.id) }"
          >
            <img
              v-if="b.coverUrl"
              class="fav-cover-img"
              :src="b.coverUrl"
              :alt="`${b.name} 封面`"
              loading="lazy"
              @error="b.coverUrl = ''"
            />
            <span class="spine" aria-hidden="true"></span>
            <span v-if="!b.coverUrl" class="fav-title">{{ b.name }}</span>
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

    <!-- 账户安全：自助修改密码 -->
    <section v-show="activeTab === 'security'" class="panel body">
      <div class="pwd-wrap">
        <div class="pwd-head">
          <h3>修改登录密码</h3>
          <p>修改后当前登录仍然有效，下次登录请使用新密码。如果忘记原密码，可联系管理员重置。</p>
        </div>

        <el-form
          ref="pwdFormRef"
          :model="pwdForm"
          :rules="pwdRules"
          label-width="100px"
          class="pwd-form"
        >
          <el-form-item label="原密码" prop="oldPassword">
            <el-input
              v-model="pwdForm.oldPassword"
              type="password"
              show-password
              autocomplete="current-password"
              placeholder="请输入当前登录密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="pwdForm.newPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="6-32 位，建议字母与数字组合"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="pwdForm.confirmPassword"
              type="password"
              show-password
              autocomplete="new-password"
              placeholder="再输入一次新密码"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="pwdSaving" @click="handleChangePassword">
              保存新密码
            </el-button>
            <el-button text @click="resetPwdForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMe, changePassword } from '../api/auth'
import { getMyBorrows, borrowBook, returnBook, renewBook } from '../api/borrow'
import { getMyFavorites, removeFavorite } from '../api/favorite'
import { getMyReservations, cancelReservation } from '../api/reservation'
import {
  getNotificationPage,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead
} from '../api/notification'
import { useUserStore } from '../stores/user'
import { borrowStatus, borrowStatusType } from '../utils'

const store = useUserStore()
const route = useRoute()

const TABS = ['borrows', 'reservations', 'notifications', 'favorites', 'security']
/** 支持从铃铛通知直接跳到某个标签：/profile?tab=reservations */
const initialTab = TABS.includes(route.query.tab) ? route.query.tab : 'borrows'
const activeTab = ref(initialTab)

const me = ref({})

const borrowQuery = reactive({ pageNum: 1, pageSize: 10 })
const borrowList = ref([])
const borrowTotal = ref(0)
const borrowLoading = ref(false)

const favList = ref([])
const favLoading = ref(false)

const stats = reactive({ borrowing: 0, overdue: 0, favorites: 0 })

// ---------------- 我的预约 ----------------
const resoList = ref([])
const resoLoading = ref(false)
const activeReservations = computed(
  () => resoList.value.filter(r => r.status === 0 || r.status === 1).length
)

const RESO_TEXT = { 0: '排队中', 1: '待取书', 2: '已取消', 3: '已过期', 4: '已借出' }
const RESO_TAG = { 0: 'warning', 1: 'success', 2: 'info', 3: 'info', 4: 'info' }
const resoText = s => RESO_TEXT[s] ?? '未知'
const resoTagType = s => RESO_TAG[s] ?? 'info'

/** 后端 LocalDateTime 序列化成 ISO（含 T），列表里只留到分钟 */
const prettyTime = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '—')

async function loadReservations() {
  resoLoading.value = true
  try {
    const res = await getMyReservations()
    resoList.value = res.data || []
  } finally {
    resoLoading.value = false
  }
}

async function handleCancelReso(row) {
  await ElMessageBox.confirm(`确定取消《${row.bookName}》的预约吗？排队位置不会保留。`, '取消预约')
  await cancelReservation(row.id)
  ElMessage.success('已取消预约')
  loadReservations()
}

/** 待取书的副本已经锁定给他，直接借即可（后端走 held 转借出，不占可借库存） */
async function handleTakeReserved(row) {
  await ElMessageBox.confirm(
    `确定借阅《${row.bookName}》吗？为你保留的副本已备好，请在保留期内取书。`,
    '确认借阅'
  )
  await borrowBook(row.bookId)
  ElMessage.success('借阅成功，请按时归还')
  loadReservations()
  loadBorrows()
  loadStats()
}

// ---------------- 消息通知 ----------------
const noteQuery = reactive({ pageNum: 1, pageSize: 10 })
const noteList = ref([])
const noteTotal = ref(0)
const noteLoading = ref(false)
const unreadCount = ref(0)

/** 类型 → 圆点配色：到期提醒（金）/ 到书（绿）/ 过期（红） */
function noteDot(type) {
  if (type === 'RESERVATION_READY') return 'ok'
  if (type === 'RESERVATION_EXPIRED') return 'bad'
  return 'warn'
}

async function loadNotes() {
  noteLoading.value = true
  try {
    const res = await getNotificationPage(noteQuery)
    noteList.value = res.data.records || []
    noteTotal.value = Number(res.data.total || 0)
    const c = await getUnreadCount()
    unreadCount.value = Number(c.data?.count || 0)
  } finally {
    noteLoading.value = false
  }
}

async function handleReadNote(n) {
  if (n.isRead !== 0) return
  await markNotificationRead(n.id)
  n.isRead = 1
  unreadCount.value = Math.max(0, unreadCount.value - 1)
}

async function handleReadAllNotes() {
  await markAllNotificationsRead()
  noteList.value.forEach(n => (n.isRead = 1))
  unreadCount.value = 0
  ElMessage.success('已全部标为已读')
}

// ---------------- 账户安全：自助改密 ----------------
const pwdFormRef = ref()
const pwdSaving = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 32, message: '新密码长度需为 6-32 位', trigger: 'blur' },
    {
      // 前后端都拦一次：后端是最终防线，前端校验只为少一次往返
      validator: (rule, value, callback) =>
        value && value === pwdForm.oldPassword
          ? callback(new Error('新密码不能与原密码相同'))
          : callback(),
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) =>
        value !== pwdForm.newPassword ? callback(new Error('两次输入的新密码不一致')) : callback(),
      trigger: 'blur'
    }
  ]
}

function resetPwdForm() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdFormRef.value?.clearValidate()
}

async function handleChangePassword() {
  if (!pwdFormRef.value) {
    return
  }
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }
  pwdSaving.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword
    })
    ElMessage.success('密码修改成功，下次登录请使用新密码')
    resetPwdForm()
  } finally {
    pwdSaving.value = false
  }
}

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
  loadReservations()
  loadNotes()
})

// 铃铛里点某条通知会带 ?tab= 过来；同一个组件内切标签不会重新 mount，得监听 query
watch(
  () => route.query.tab,
  tab => {
    if (TABS.includes(tab)) activeTab.value = tab
  }
)
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
.switcher .n.hot {
  color: #fff;
  background: #b4553f;
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

/* ---------------- 我的预约 ---------------- */
.reso-book {
  display: flex;
  align-items: center;
  gap: 10px;
}
.reso-cover {
  width: 30px;
  height: 40px;
  flex: none;
  border-radius: 3px;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: #2e6b53;
}
.reso-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  display: block;
}
.reso-cover i {
  font-style: normal;
  font-size: 12px;
  color: #f6f1e4;
  opacity: 0.85;
}
.reso-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}
.reso-text em {
  font-style: normal;
  font-size: 11.5px;
  color: var(--ink-3);
}
.queue-pos {
  font-size: 12.5px;
  color: var(--ink-2);
}
.queue-pos b {
  color: var(--forest);
  font-size: 14px;
}
.ok-text {
  font-size: 12.5px;
  color: var(--forest);
  font-weight: 600;
}

/* ---------------- 消息通知 ---------------- */
.note-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 2px 8px;
  font-size: 12.5px;
  color: var(--ink-3);
}
.note-toolbar b {
  color: var(--ink);
  font-size: 13.5px;
}
.note-toolbar b.warn {
  color: #b4553f;
}

.note-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 80px;
}

.note-item {
  position: relative;
  display: flex;
  gap: 11px;
  padding: 13px 15px;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: #fffdf9;
  cursor: pointer;
  transition:
    border-color 0.16s,
    box-shadow 0.16s,
    background 0.16s;
}
.note-item:hover {
  border-color: #d8c9b0;
  box-shadow: var(--shadow-sm);
}
.note-item.unread {
  background: #fbf7ee;
  border-color: #e6d9bd;
}

.note-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-top: 7px;
  flex: none;
  background: #cdbfa6;
}
.note-dot.warn {
  background: var(--gold);
}
.note-dot.ok {
  background: var(--forest-soft);
}
.note-dot.bad {
  background: #b4553f;
}

.note-main {
  flex: 1;
  min-width: 0;
}
.note-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}
.note-head b {
  font-size: 13.5px;
  color: var(--ink-2);
  font-weight: 600;
}
.note-item.unread .note-head b {
  color: var(--ink);
  font-weight: 700;
}
.note-head time {
  flex: none;
  font-size: 11.5px;
  color: #a99e8b;
}
.note-main p {
  margin: 5px 0 0;
  font-size: 12.5px;
  line-height: 1.65;
  color: var(--ink-3);
}
.note-flag {
  position: absolute;
  top: 12px;
  right: 14px;
  font-size: 10px;
  color: #b4553f;
  background: #fbeee9;
  border-radius: 999px;
  padding: 1px 7px;
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
.fav-cover.has-cover {
  padding: 0;
}
.fav-cover-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  display: block;
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

/* ---------------- 账户安全 ---------------- */
.pwd-wrap {
  max-width: 560px;
}
.pwd-head h3 {
  font-size: 15px;
  font-weight: 700;
}
.pwd-head p {
  margin: 6px 0 22px;
  font-size: 12.5px;
  line-height: 1.75;
  color: var(--ink-3);
}
</style>
