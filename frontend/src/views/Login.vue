<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧：品牌区 -->
      <aside class="brand-pane">
        <div class="brand-top">
          <span class="brand-mark">书</span>
          <span class="brand-name">图书管理系统</span>
        </div>

        <div class="brand-copy">
          <p class="eyebrow-line">LIBRARY · SINCE 2026</p>
          <h1 class="brand-title display">看得见的馆藏<br />借得到的书</h1>
          <p class="brand-desc">
            检索、借阅、续借、收藏，一站式完成。<br />
            馆藏状态实时同步，逾期一目了然。
          </p>
        </div>

        <div class="shelf" aria-hidden="true">
          <span class="spine s1"></span>
          <span class="spine s2"></span>
          <span class="spine s3"></span>
          <span class="spine s4"></span>
          <span class="spine s5"></span>
          <span class="spine s6"></span>
          <span class="spine s7"></span>
        </div>

        <p class="brand-foot">馆藏 · 借阅 · 阅读</p>
      </aside>

      <!-- 右侧：表单区 -->
      <section class="form-pane">
        <div class="form-head">
          <div class="segment">
            <button :class="{ on: activeTab === 'login' }" @click="activeTab = 'login'">
              登录
            </button>
            <button :class="{ on: activeTab === 'register' }" @click="activeTab = 'register'">
              注册
            </button>
            <span
              class="seg-thumb"
              :style="{ transform: `translateX(${activeTab === 'login' ? 0 : 100}%)` }"
            ></span>
          </div>
          <p class="form-hint">
            {{
              activeTab === 'login' ? '欢迎回来，请登录你的账号' : '注册一个读者账号，立即开始借阅'
            }}
          </p>
        </div>

        <!-- 登录 -->
        <el-form
          v-if="activeTab === 'login'"
          :model="loginForm"
          label-width="0"
          @submit.prevent
          class="form"
        >
          <label class="field">
            <span class="field-label">用户名</span>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              @keyup.enter="handleLogin"
            />
          </label>
          <label class="field">
            <span class="field-label">密码</span>
            <el-input
              v-model="loginForm.password"
              type="password"
              show-password
              placeholder="请输入密码"
              size="large"
              @keyup.enter="handleLogin"
            />
          </label>
          <button class="btn-gold submit" :disabled="loading" @click.prevent="handleLogin">
            {{ loading ? '登录中…' : '登 录' }}
          </button>
        </el-form>

        <!-- 注册 -->
        <el-form v-else :model="registerForm" label-width="0" @submit.prevent class="form">
          <label class="field">
            <span class="field-label">用户名</span>
            <el-input
              v-model="registerForm.username"
              placeholder="3~20 位字母数字下划线"
              size="large"
            />
          </label>
          <label class="field">
            <span class="field-label">密码</span>
            <el-input
              v-model="registerForm.password"
              type="password"
              show-password
              placeholder="6~20 位"
              size="large"
            />
          </label>
          <label class="field">
            <span class="field-label">真实姓名<span class="opt">选填</span></span>
            <el-input v-model="registerForm.realName" placeholder="便于管理员核对" size="large" />
          </label>
          <button class="btn-gold submit" :disabled="loading" @click.prevent="handleRegister">
            {{ loading ? '提交中…' : '注 册' }}
          </button>
        </el-form>

        <div class="demo-accounts">
          <span class="demo-label">演示账号</span>
          <button class="demo-chip" @click="fill('admin', 'admin123')">
            管理员 <b>admin / admin123</b>
          </button>
          <button class="demo-chip" @click="fill('reader', '123456')">
            读者 <b>reader / 123456</b>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../api/auth'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', realName: '' })

// 一键填入演示账号，省得手打
function fill(username, password) {
  activeTab.value = 'login'
  loginForm.username = username
  loginForm.password = password
}

async function handleLogin() {
  // 去掉首尾空格：粘贴账号时极易带上不可见空格，导致「用户不存在 → 用户名或密码错误」
  const username = loginForm.username.trim()
  const password = loginForm.password.trim()
  if (!username || !password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ username, password })
    store.setLogin(res.data)
    ElMessage.success('登录成功')
    const redirect = route.query.redirect
    if (redirect) {
      router.push(redirect)
    } else {
      router.push(res.data.role === 'admin' ? '/dashboard' : '/books')
    }
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  registerForm.username = registerForm.username.trim()
  registerForm.realName = registerForm.realName.trim()
  if (!registerForm.username || !registerForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  if (registerForm.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  loading.value = true
  try {
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    loginForm.username = registerForm.username
    loginForm.password = ''
    activeTab.value = 'login'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px 20px;
  background:
    radial-gradient(900px 460px at 12% 0%, #f7efe1 0%, transparent 70%),
    radial-gradient(700px 400px at 88% 100%, #eef2ec 0%, transparent 72%), var(--paper);
}

.login-card {
  width: min(960px, 100%);
  min-height: 540px;
  display: grid;
  grid-template-columns: 1.05fr 1fr;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

/* ---------------- 左侧品牌 ---------------- */
.brand-pane {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 34px 36px 28px;
  background: linear-gradient(158deg, #27573f 0%, #1f4b3a 42%, #14342a 100%);
  color: #f7f2e4;
  overflow: hidden;
}
.brand-pane::after {
  content: '';
  position: absolute;
  width: 420px;
  height: 420px;
  right: -160px;
  top: -170px;
  border-radius: 50%;
  border: 1px solid rgba(224, 173, 58, 0.16);
  box-shadow: inset 0 0 0 60px rgba(224, 173, 58, 0.03);
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
  z-index: 1;
}
.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: rgba(246, 227, 180, 0.14);
  border: 1px solid rgba(246, 227, 180, 0.28);
  color: var(--gold-soft);
  font-family: Georgia, 'Songti SC', serif;
  font-size: 17px;
  font-weight: 700;
}
.brand-name {
  font-size: 14px;
  letter-spacing: 0.06em;
  color: rgba(250, 246, 237, 0.86);
}

.brand-copy {
  margin-top: 54px;
  position: relative;
  z-index: 1;
}
.eyebrow-line {
  margin: 0 0 14px;
  font-size: 10.5px;
  letter-spacing: 0.3em;
  color: rgba(246, 227, 180, 0.72);
}
.brand-title {
  font-size: 34px;
  line-height: 1.42;
  color: #fdfaf2;
  font-weight: 700;
  letter-spacing: 0.03em;
}
.brand-desc {
  margin: 16px 0 0;
  font-size: 13px;
  line-height: 1.9;
  color: rgba(250, 246, 237, 0.62);
}

/* 书脊装饰 */
.shelf {
  margin-top: auto;
  padding-top: 40px;
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 96px;
  position: relative;
  z-index: 1;
}
.spine {
  border-radius: 3px 3px 1px 1px;
  background: linear-gradient(180deg, rgba(246, 227, 180, 0.28), rgba(246, 227, 180, 0.08));
  border-top: 2px solid rgba(224, 173, 58, 0.5);
  flex: 1;
}
.s1 {
  height: 62px;
}
.s2 {
  height: 84px;
}
.s3 {
  height: 48px;
}
.s4 {
  height: 74px;
}
.s5 {
  height: 58px;
}
.s6 {
  height: 90px;
}
.s7 {
  height: 42px;
}

.brand-foot {
  margin: 16px 0 0;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: rgba(250, 246, 237, 0.34);
}

/* ---------------- 右侧表单 ---------------- */
.form-pane {
  padding: 44px 44px 34px;
  display: flex;
  flex-direction: column;
}

.segment {
  position: relative;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: var(--paper-2);
  border-radius: 10px;
  padding: 4px;
}
.segment button {
  position: relative;
  z-index: 1;
  appearance: none;
  border: none;
  background: transparent;
  height: 34px;
  font-size: 13.5px;
  font-weight: 500;
  color: var(--ink-3);
  cursor: pointer;
  font-family: inherit;
  transition: color 0.2s;
}
.segment button.on {
  color: var(--ink);
  font-weight: 700;
}
.seg-thumb {
  position: absolute;
  z-index: 0;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: 34px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px -4px rgba(31, 75, 58, 0.4);
  transition: transform 0.24s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-hint {
  margin: 14px 0 26px;
  font-size: 12.5px;
  color: var(--ink-3);
}

.form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
}
.field-label {
  font-size: 12.5px;
  font-weight: 600;
  color: var(--ink-2);
  letter-spacing: 0.04em;
}
.opt {
  margin-left: 6px;
  font-weight: 400;
  color: var(--ink-3);
  font-size: 11px;
}

.form :deep(.el-input__wrapper) {
  background: #fdfbf6 !important;
  border-radius: 9px !important;
  box-shadow: 0 0 0 1px var(--line) inset !important;
  padding: 4px 12px;
}
.form :deep(.el-input__wrapper.is-focus) {
  background: #fff !important;
  box-shadow: 0 0 0 1px var(--forest) inset !important;
}

.submit {
  margin-top: 8px;
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 0.2em;
}
.submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 演示账号 */
.demo-accounts {
  margin-top: auto;
  padding-top: 24px;
  border-top: 1px dashed var(--line);
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.demo-label {
  font-size: 11px;
  color: var(--ink-3);
  letter-spacing: 0.14em;
}
.demo-chip {
  appearance: none;
  border: 1px solid var(--line);
  background: #fffdf9;
  border-radius: 999px;
  padding: 4px 11px;
  font-size: 11.5px;
  color: var(--ink-3);
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color 0.16s,
    color 0.16s,
    background 0.16s;
}
.demo-chip b {
  color: var(--ink-2);
  font-weight: 600;
}
.demo-chip:hover {
  border-color: var(--forest);
  color: var(--forest);
  background: #f6faf7;
}
.demo-chip:hover b {
  color: var(--forest);
}

@media (max-width: 860px) {
  .login-card {
    grid-template-columns: 1fr;
    min-height: 0;
  }
  .brand-pane {
    padding: 26px 24px;
  }
  .brand-copy {
    margin-top: 26px;
  }
  .brand-title {
    font-size: 26px;
  }
  .shelf,
  .brand-foot {
    display: none;
  }
  .form-pane {
    padding: 28px 22px 26px;
  }
}
</style>
