import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

/**
 * Axios 统一封装
 * - 请求拦截器：自动携带 JWT（Authorization: Bearer xxx）
 * - 响应拦截器：按统一响应格式 { code, message, data } 处理
 *   code === 200 时直接返回整个 Result 对象（调用方取 .data / .message）
 *   code === 401 时清除登录态并跳转登录页；其余错误统一弹出提示
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = 'Bearer ' + token
  }
  return config
})

request.interceptors.response.use(
  res => {
    // 文件下载（responseType: 'blob'）直接返回二进制，不参与 {code,message,data} 解析
    if (res.config.responseType === 'blob') {
      return res.data
    }
    const result = res.data
    if (result.code === 200) {
      return result
    }
    if (result.code === 401) {
      localStorage.removeItem('token')
      ElMessage.error(result.message || '登录已过期，请重新登录')
      router.push('/login')
    } else {
      ElMessage.error(result.message || '请求失败')
    }
    return Promise.reject(new Error(result.message || '请求失败'))
  },
  error => {
    const result = error.response?.data
    // 没有 response 说明请求没到后端（后端未启动 / 端口不通 / 超时），给出可操作的提示
    let message = result?.message
    if (!message) {
      message =
        error.code === 'ECONNABORTED'
          ? '请求超时，请稍后重试'
          : '无法连接后端服务，请确认后端已在 8080 端口启动'
    }
    // 5xx 时后端在响应头 X-Trace-Id 里回传追踪号：带上它，
    // 用户截图报障就等于给出了「一次请求的全部日志」的检索键
    const traceId = error.response?.headers?.['x-trace-id']
    if (error.response?.status >= 500 && traceId && !message.includes(traceId)) {
      message += `（追踪号 ${traceId}）`
    }
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request
