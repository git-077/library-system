<template>
  <el-popover
    v-model:visible="visible"
    placement="bottom-end"
    :width="352"
    trigger="click"
    popper-class="bell-popper"
    @show="loadList"
  >
    <template #reference>
      <button class="bell" :class="{ ringing: count > 0 }" type="button" title="我的通知">
        <span class="bell-glyph" aria-hidden="true">🔔</span>
        <span v-if="count > 0" class="bell-badge">{{ count > 99 ? '99+' : count }}</span>
      </button>
    </template>

    <div class="bell-panel">
      <header class="bell-head">
        <strong>我的通知</strong>
        <a v-if="count > 0" @click="handleReadAll">全部标为已读</a>
      </header>

      <div class="bell-body" v-loading="loading">
        <a
          v-for="n in list"
          :key="n.id"
          class="note"
          :class="{ unread: n.isRead === 0 }"
          @click="openNote(n)"
        >
          <span class="note-dot" :class="dotClass(n.type)"></span>
          <span class="note-main">
            <span class="note-title">{{ n.title }}</span>
            <span class="note-text">{{ n.content }}</span>
            <span class="note-time">{{ formatTime(n.createTime) }}</span>
          </span>
        </a>

        <p v-if="!loading && list.length === 0" class="bell-empty">
          还没有通知。预约的书到馆、借阅快到期时，会在这里提醒你。
        </p>
      </div>

      <footer v-if="list.length > 0" class="bell-foot">
        <a @click="goProfile">查看全部记录 →</a>
      </footer>
    </div>
  </el-popover>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getNotificationPage,
  getUnreadCount,
  markNotificationRead,
  markAllNotificationsRead
} from '../api/notification'

const router = useRouter()
const visible = ref(false)
const count = ref(0)
const list = ref([])
const loading = ref(false)
let timer = null

/** 轮询未读数：站内信没有推送通道，60 秒一次足够，也不至于给后端添压力 */
async function refreshCount() {
  try {
    const res = await getUnreadCount()
    count.value = Number(res.data?.count || 0)
  } catch {
    /* 轮询失败静默：不能因为铃铛把页面搞出错误弹窗 */
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getNotificationPage({ pageNum: 1, pageSize: 8 })
    list.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

/** 类型 → 圆点配色：提醒（金）/ 到书（绿）/ 过期（红） */
function dotClass(type) {
  if (type === 'RESERVATION_READY') return 'ok'
  if (type === 'RESERVATION_EXPIRED') return 'bad'
  return 'warn'
}

function formatTime(value) {
  if (!value) return ''
  const d = new Date(String(value).replace(/-/g, '/').replace('T', ' '))
  if (Number.isNaN(d.getTime())) return String(value)
  const diff = Date.now() - d.getTime()
  if (diff < 60 * 1000) return '刚刚'
  if (diff < 60 * 60 * 1000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 24 * 60 * 60 * 1000) return `${Math.floor(diff / 3600000)} 小时前`
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

async function openNote(n) {
  if (n.isRead === 0) {
    try {
      await markNotificationRead(n.id)
      n.isRead = 1
      refreshCount()
    } catch {
      /* 标记失败不影响跳转 */
    }
  }
  visible.value = false
  // 到书/过期通知直达「我的预约」，到期提醒直达「我的借阅」
  router.push({
    path: '/profile',
    query: { tab: n.type === 'DUE_SOON' ? 'borrows' : 'reservations' }
  })
}

async function handleReadAll() {
  await markAllNotificationsRead()
  list.value.forEach(n => (n.isRead = 1))
  count.value = 0
  ElMessage.success('已全部标为已读')
}

function goProfile() {
  visible.value = false
  router.push({ path: '/profile', query: { tab: 'notifications' } })
}

onMounted(() => {
  refreshCount()
  timer = setInterval(refreshCount, 60 * 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.bell {
  position: relative;
  appearance: none;
  border: 1px solid var(--line);
  background: #fff;
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  cursor: pointer;
  transition:
    border-color 0.18s,
    box-shadow 0.18s,
    transform 0.18s;
  flex: none;
}
.bell:hover {
  border-color: #d6c8ae;
  box-shadow: var(--shadow-sm);
}
.bell-glyph {
  font-size: 14px;
  line-height: 1;
  filter: grayscale(0.15);
}
/* 有未读时轻微晃动一次，别做成持续动画，会分神 */
.bell.ringing .bell-glyph {
  animation: bell-swing 1.6s ease-in-out 2;
}
@keyframes bell-swing {
  0%,
  100% {
    transform: rotate(0);
  }
  15% {
    transform: rotate(12deg);
  }
  30% {
    transform: rotate(-10deg);
  }
  45% {
    transform: rotate(6deg);
  }
  60% {
    transform: rotate(-4deg);
  }
}

.bell-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background: #b4553f;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  line-height: 16px;
  text-align: center;
  box-shadow: 0 0 0 2px #fffdf9;
}

/* ---------------- 面板 ---------------- */
.bell-panel {
  margin: -4px;
}
.bell-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 8px 10px;
  border-bottom: 1px solid var(--line);
}
.bell-head strong {
  font-size: 13.5px;
  color: var(--ink);
}
.bell-head a,
.bell-foot a {
  font-size: 12px;
  color: var(--ink-3);
  cursor: pointer;
}
.bell-head a:hover,
.bell-foot a:hover {
  color: var(--forest);
}

.bell-body {
  max-height: 380px;
  overflow-y: auto;
  padding: 6px 0;
  min-height: 72px;
}

.note {
  display: flex;
  gap: 9px;
  padding: 9px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.16s;
}
.note:hover {
  background: #f7f2e7;
}
.note-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-top: 6px;
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
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.note-title {
  font-size: 13px;
  color: var(--ink-2);
  line-height: 1.45;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.note.unread .note-title {
  font-weight: 700;
  color: var(--ink);
}
.note-text {
  font-size: 12px;
  color: var(--ink-3);
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.note-time {
  font-size: 11px;
  color: #a99e8b;
}

.bell-empty {
  margin: 18px 8px 20px;
  font-size: 12.5px;
  line-height: 1.7;
  color: var(--ink-3);
  text-align: center;
}

.bell-foot {
  padding: 9px 8px 2px;
  border-top: 1px solid var(--line);
  text-align: center;
}
</style>
