<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="模块">
          <el-select
            v-model="query.module"
            placeholder="全部"
            clearable
            style="width: 130px"
            @change="reload"
          >
            <el-option v-for="m in MODULES" :key="m" :label="m" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 110px"
            @change="reload"
          >
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="操作人 / 请求路径"
            clearable
            style="width: 200px"
            @keyup.enter="reload"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="detail">
              <div class="detail-row">
                <span class="detail-label">请求</span>
                <code>{{ row.httpMethod }} {{ row.uri }}</code>
              </div>
              <div class="detail-row">
                <span class="detail-label">入参</span>
                <pre class="detail-pre">{{ pretty(row.params) }}</pre>
              </div>
              <div v-if="row.errorMsg" class="detail-row">
                <span class="detail-label">失败原因</span>
                <span class="detail-error">{{ row.errorMsg }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="username" label="操作人" width="110">
          <template #default="{ row }">{{ row.username || '—' }}</template>
        </el-table-column>
        <el-table-column prop="module" label="模块" width="90">
          <template #default="{ row }">
            <span class="mod-tag">{{ row.module }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="110" />
        <el-table-column prop="uri" label="请求路径" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="mono">{{ row.httpMethod }}</span> {{ row.uri }}
          </template>
        </el-table-column>
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">{{ row.costMs }} ms</template>
        </el-table-column>
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        style="margin-top: 16px; justify-content: flex-end"
        layout="total, prev, pager, next"
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
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getLogPage } from '../api/log'

const MODULES = ['图书', '分类', '公告', '用户', '借阅']

const query = reactive({ pageNum: 1, pageSize: 10, module: '', status: null, keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const res = await getLogPage({
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      module: query.module || undefined,
      status: query.status ?? undefined,
      keyword: query.keyword || undefined
    })
    list.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function reset() {
  query.module = ''
  query.status = null
  query.keyword = ''
  reload()
}

/** 后端返回 yyyy-MM-ddTHH:mm:ss，这里转成更好读的展示格式 */
function formatTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 19)
}

function pretty(params) {
  if (!params) return '（无入参）'
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params
  }
}

onMounted(load)
</script>

<style scoped>
.detail {
  padding: 6px 18px 12px 48px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.detail-row {
  display: flex;
  gap: 12px;
  font-size: 12.5px;
  line-height: 1.6;
}
.detail-label {
  flex: none;
  width: 62px;
  color: var(--ink-3);
}
.detail-pre {
  margin: 0;
  padding: 8px 12px;
  background: var(--paper-2);
  border-radius: 8px;
  font-size: 12px;
  color: var(--ink-2);
  max-height: 220px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.detail-error {
  color: #a2544a;
}
.mod-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 999px;
  background: var(--forest-mist);
  color: var(--forest);
  font-size: 11.5px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
  font-size: 11.5px;
  color: var(--ink-3);
  margin-right: 4px;
}
</style>
