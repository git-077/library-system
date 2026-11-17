<template>
  <!-- ============================================================
       读者：门户形态（顶部导航 + 内容居中）
       ============================================================ -->
  <div v-if="!store.isAdmin" class="portal-shell">
    <header class="topbar">
      <div class="topbar-inner">
        <div class="brand" @click="router.push('/books')">
          <span class="brand-mark">书</span>
          <span class="brand-text">
            <strong>图书管理系统</strong>
            <em>馆藏门户</em>
          </span>
        </div>

        <nav class="topnav">
          <a
            v-for="m in readerMenus"
            :key="m.path"
            :class="{ active: isActive(m.path) }"
            @click="router.push(m.path)"
          >
            {{ m.title }}
          </a>
        </nav>

        <div class="topbar-actions">
          <NotificationBell />
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip">
              <span class="avatar">{{ initial }}</span>
              <span class="user-name">{{ store.username }}</span>
              <span class="role-dot">读者</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="portal-foot">
      <span>馆藏 · 借阅 · 阅读</span>
      <span class="dot">·</span>
      <span>{{ store.username }}，欢迎回来</span>
    </footer>
  </div>

  <!-- ============================================================
       管理员：管理台形态（侧边栏 + 顶栏）
       ============================================================ -->
  <el-container v-else class="console">
    <el-aside width="216px" class="aside">
      <div class="aside-brand">
        <span class="brand-mark">书</span>
        <span class="aside-brand-text">
          <strong>图书管理系统</strong>
          <em>管理台</em>
        </span>
      </div>

      <div class="aside-label">工作台</div>
      <nav class="aside-nav">
        <a
          v-for="m in adminMenus"
          :key="m.path"
          :class="{ active: isActive(m.path) }"
          @click="router.push(m.path)"
        >
          <span class="nav-bar"></span>
          <span class="nav-glyph">{{ m.glyph }}</span>
          <span>{{ m.title }}</span>
        </a>
      </nav>

      <div class="aside-foot">
        <span class="aside-tip">当前登录</span>
        <div class="aside-user">
          <span class="avatar avatar-dark">{{ initial }}</span>
          <div>
            <strong>{{ store.username }}</strong>
            <em>管理员</em>
          </div>
        </div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <span class="header-title">{{ $route.meta.title }}</span>
          <span class="header-crumb">管理台 / {{ $route.meta.title }}</span>
        </div>
        <div class="header-actions">
          <NotificationBell />
          <el-dropdown trigger="click" @command="onCommand">
            <span class="user-chip chip-console">
              <span class="avatar">{{ initial }}</span>
              {{ store.username }}
              <span class="role-dot">管理员</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import NotificationBell from '../components/NotificationBell.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const readerMenus = [
  { path: '/books', title: '馆藏检索' },
  { path: '/profile', title: '我的借阅' },
  { path: '/notices', title: '系统公告' }
]

const adminMenus = [
  { path: '/dashboard', title: '数据统计', glyph: '◲' },
  { path: '/books', title: '馆藏检索', glyph: '⌕' },
  { path: '/book-manage', title: '图书管理', glyph: '▤' },
  { path: '/categories', title: '分类管理', glyph: '❏' },
  { path: '/borrows', title: '借阅管理', glyph: '⇄' },
  { path: '/reservations', title: '预约队列', glyph: '◷' },
  { path: '/users', title: '用户管理', glyph: '☺' },
  { path: '/notices', title: '系统公告', glyph: '✎' },
  { path: '/logs', title: '操作日志', glyph: '⧉' },
  { path: '/profile', title: '个人中心', glyph: '◎' }
]

const initial = computed(() => (store.username || '?').charAt(0).toUpperCase())

const isActive = path => route.path === path

function onCommand(command) {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    store.logout()
    router.push('/login')
  }
}
</script>

<style scoped>
/* ============================================================
   通用品牌标识
   ============================================================ */
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(160deg, #2b6049, #17392c);
  color: var(--gold-soft);
  font-family: Georgia, 'Songti SC', serif;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0;
  box-shadow: 0 4px 12px -8px rgba(22, 58, 44, 0.9);
  flex: none;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--forest-mist);
  color: var(--forest);
  font-size: 12px;
  font-weight: 700;
}

/* ============================================================
   读者门户
   ============================================================ */
.portal-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--paper);
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: rgba(255, 253, 249, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.topbar-inner {
  max-width: 1120px;
  margin: 0 auto;
  height: 62px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  user-select: none;
}
.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}
.brand-text strong {
  font-size: 15px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.02em;
}
.brand-text em {
  font-style: normal;
  font-size: 10.5px;
  color: var(--ink-3);
  letter-spacing: 0.16em;
  margin-top: 2px;
}

.topnav {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}
.topnav a {
  position: relative;
  padding: 6px 14px;
  font-size: 13.5px;
  color: var(--ink-2);
  cursor: pointer;
  border-radius: 999px;
  transition:
    color 0.18s,
    background 0.18s;
}
.topnav a:hover {
  color: var(--forest);
  background: #f3ede1;
}
.topnav a.active {
  color: var(--forest);
  font-weight: 600;
}
.topnav a.active::after {
  content: '';
  position: absolute;
  left: 14px;
  right: 14px;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: var(--gold);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 5px 12px 5px 6px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #fff;
  font-size: 13px;
  color: var(--ink-2);
  transition:
    border-color 0.18s,
    box-shadow 0.18s;
}
.user-chip:hover {
  border-color: #d6c8ae;
  box-shadow: var(--shadow-sm);
}
.user-name {
  font-weight: 600;
  color: var(--ink);
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.role-dot {
  font-size: 10.5px;
  color: var(--gold-deep);
  background: var(--gold-wash);
  border-radius: 999px;
  padding: 1px 7px;
  letter-spacing: 0.04em;
}

.topbar-inner > :last-child {
  margin-left: auto;
}

/* 铃铛 + 用户胶囊：右侧成组，间距比 default 紧一点，视觉上是一块 */
.topbar-actions,
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.portal-main {
  flex: 1;
  width: 100%;
  max-width: 1160px;
  margin: 0 auto;
  padding: 22px 20px 40px;
}

.portal-foot {
  padding: 18px 0 26px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-3);
  letter-spacing: 0.06em;
}
.portal-foot .dot {
  margin: 0 8px;
  opacity: 0.5;
}

/* ============================================================
   管理台
   ============================================================ */
.console {
  height: 100vh;
}

.aside {
  background: linear-gradient(180deg, #1a4233 0%, #16372a 100%);
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(0, 0, 0, 0.18);
}

.aside-brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(246, 227, 180, 0.12);
}
.aside-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.aside-brand-text strong {
  font-size: 14px;
  color: #f6f1e4;
  font-weight: 700;
}
.aside-brand-text em {
  font-style: normal;
  font-size: 10px;
  color: rgba(246, 241, 228, 0.45);
  letter-spacing: 0.18em;
  margin-top: 2px;
}

.aside-label {
  padding: 18px 22px 8px;
  font-size: 10.5px;
  letter-spacing: 0.2em;
  color: rgba(246, 241, 228, 0.34);
}

.aside-nav {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 10px;
  overflow-y: auto;
}

.aside-nav a {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  padding: 0 12px;
  border-radius: 9px;
  font-size: 13.5px;
  color: rgba(246, 241, 228, 0.68);
  cursor: pointer;
  transition:
    background 0.16s,
    color 0.16s;
}
.aside-nav a:hover {
  background: rgba(246, 241, 228, 0.07);
  color: #f6f1e4;
}
.aside-nav a.active {
  background: rgba(224, 173, 58, 0.14);
  color: #f8f2e0;
  font-weight: 600;
}
.nav-bar {
  position: absolute;
  left: -10px;
  top: 9px;
  bottom: 9px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: transparent;
}
.aside-nav a.active .nav-bar {
  background: var(--gold);
}
.nav-glyph {
  width: 16px;
  text-align: center;
  opacity: 0.8;
  font-size: 13px;
}

.aside-foot {
  margin-top: auto;
  padding: 14px 18px 18px;
  border-top: 1px solid rgba(246, 227, 180, 0.1);
}
.aside-tip {
  font-size: 10.5px;
  letter-spacing: 0.16em;
  color: rgba(246, 241, 228, 0.34);
}
.aside-user {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 8px;
}
.aside-user strong {
  display: block;
  font-size: 13px;
  color: #f6f1e4;
  font-weight: 600;
}
.aside-user em {
  font-style: normal;
  font-size: 10.5px;
  color: rgba(246, 241, 228, 0.45);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: rgba(255, 253, 249, 0.9);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--line);
  height: 64px;
}
.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}
.header-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.02em;
}
.header-crumb {
  font-size: 12px;
  color: var(--ink-3);
}
.chip-console {
  background: #fff;
}

.main {
  background: var(--paper);
  padding: 20px 22px 32px;
  overflow: auto;
}

@media (max-width: 860px) {
  .topnav {
    display: none;
  }
}
</style>
