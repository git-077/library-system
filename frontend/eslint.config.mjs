// ESLint 扁平配置（ESLint 9）。执行：npm run lint
import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import prettier from 'eslint-config-prettier'

export default [
  {
    // 构建产物与依赖不参与检查
    ignores: ['dist/**', 'node_modules/**', '*.config.js']
  },
  js.configs.recommended,
  // essential 只包含「会出错」的规则（未定义变量、重复 key、模板语法误用等）；
  // 风格类规则全部交给 Prettier，避免与格式化打架、也不给存量代码制造噪声
  ...pluginVue.configs['flat/essential'],
  // 关闭与 Prettier 冲突的格式类规则
  prettier,
  {
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        window: 'readonly',
        document: 'readonly',
        localStorage: 'readonly',
        console: 'readonly',
        setTimeout: 'readonly',
        clearTimeout: 'readonly'
      }
    },
    rules: {
      // 页面组件用单词名（Login / Layout 等）在本项目是合理约定
      'vue/multi-word-component-names': 'off',
      // 项目内确有需要渲染富文本摘要的场景，由使用方保证内容可信
      'vue/no-v-html': 'off',
      'no-unused-vars': ['warn', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'vue/component-api-style': ['error', ['script-setup']]
    }
  }
]
