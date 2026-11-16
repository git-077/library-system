import { defineStore } from 'pinia'

/**
 * 用户状态：token 与基本信息持久化到 localStorage，
 * 刷新页面后登录态不丢失
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userId: localStorage.getItem('userId') || '',
    username: localStorage.getItem('username') || '',
    role: localStorage.getItem('role') || ''
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.role === 'admin'
  },
  actions: {
    setLogin({ token, userId, username, role }) {
      this.token = token
      this.userId = userId
      this.username = username
      this.role = role
      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('username', username)
      localStorage.setItem('role', role)
    },
    logout() {
      this.token = ''
      this.userId = ''
      this.username = ''
      this.role = ''
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      localStorage.removeItem('role')
    }
  }
})
