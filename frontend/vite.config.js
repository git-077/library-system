import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 后端地址可通过环境变量覆盖（后端换端口时同步改这个即可）
const BACKEND = process.env.VITE_PROXY_TARGET || 'http://localhost:8080'

// 本地开发时 /api 代理到后端 8080，避免跨域；
// 生产环境由 Nginx 完成同样的反向代理
export default defineConfig({
  plugins: [vue()],
  server: {
    // 同时兼容 http://localhost:5173 与 http://127.0.0.1:5173 两种访问方式
    host: '0.0.0.0',
    port: 5173,
    strictPort: true,
    // 允许内网穿透域名访问（Cloudflare quick tunnel 的 *。trycloudflare.com 每次重启会变，
    // 所以按后缀放行）；只在本机/局域网调试时此项不影响
    allowedHosts: ['.trycloudflare.com', '.vicp.net', '.gicp.net', '.zicp.net', '.oicp.net', '.oray.net'],
    proxy: {
      '/api': {
        target: BACKEND,
        changeOrigin: true
      },
      // 上传的封面等静态资源由后端 /uploads/** 提供，同样代理过去，
      // 这样前端里 <img src="/uploads/xxx.jpg"> 在生产（Nginx）和开发（Vite）下走同一路径
      '/uploads': {
        target: BACKEND,
        changeOrigin: true
      }
    }
  },
  build: {
    // 生产构建：按依赖拆包，避免单个 1MB 大包拖慢首屏
    // vendor 变动频率低，可长期命中浏览器缓存；业务代码单独成块
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          element: ['element-plus'],
          axios: ['axios'],
          // 图表库只在统计页用到，单独成块后可懒加载、长期缓存
          echarts: ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers']
        }
      }
    },
    // 超过 600KB 才提示，避免无意义告警
    chunkSizeWarningLimit: 600
  }
})
