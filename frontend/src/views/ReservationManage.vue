<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 148px"
            @change="reload"
          >
            <el-option
              v-for="s in STATUS_OPTIONS"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="书名">
          <el-input
            v-model="query.keyword"
            placeholder="按书名筛选"
            clearable
            style="width: 200px"
            @keyup.enter="reload"
            @clear="reload"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert type="info" :closable="false" show-icon class="tip">
        <template #title>
          副本归还后会自动锁定给队首并发送到书通知，保留
          <b>{{ holdDays }}</b>
          天；超时未取自动顺延给下一位。管理员可直接取消某个预约（例如读者电话告知不需要了）。
        </template>
      </el-alert>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="预约ID" width="88" />
        <el-table-column prop="username" label="读者" width="120" />
        <el-table-column label="书名" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.bookName || `图书#${row.bookId}` }}</template>
        </el-table-column>

        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="进度" width="170">
          <template #default="{ row }">
            <span v-if="row.status === 0"
              >排第 <b class="pos">{{ row.position }}</b> 位</span
            >
            <span v-else-if="row.status === 1" class="ok">
              保留至 {{ short(row.expireTime) }}
            </span>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="预约时间" width="160">
          <template #default="{ row }">{{ short(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="结束时间" width="160">
          <template #default="{ row }">{{ short(row.finishTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="104" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0 || row.status === 1"
              size="small"
              plain
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <span v-else class="muted">—</span>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getReservationPage, cancelReservation } from '../api/reservation'

const STATUS_OPTIONS = [
  { value: 0, label: '排队中' },
  { value: 1, label: '待取书' },
  { value: 2, label: '已取消' },
  { value: 3, label: '已过期' },
  { value: 4, label: '已借出' }
]
const TEXT = STATUS_OPTIONS.reduce((m, s) => ({ ...m, [s.value]: s.label }), {})
const TAG = { 0: 'warning', 1: 'success', 2: 'info', 3: 'danger', 4: 'info' }

const query = reactive({ pageNum: 1, pageSize: 10, status: null, keyword: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
// 与后端 library.reservation.hold-days 默认值一致，仅用于说明文案
const holdDays = ref(7)

const statusText = s => TEXT[s] ?? '未知'
const tagType = s => TAG[s] ?? 'info'
const short = v => (v ? String(v).replace('T', ' ').slice(0, 16) : '—')

async function load() {
  loading.value = true
  try {
    const res = await getReservationPage(query)
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

function reload() {
  query.pageNum = 1
  load()
}

function resetQuery() {
  query.status = null
  query.keyword = ''
  reload()
}

async function handleCancel(row) {
  await ElMessageBox.confirm(
    `确定取消「${row.username}」对《${row.bookName}》的预约吗？若该预约已待取书，副本会自动顺延给下一位。`,
    '取消预约'
  )
  await cancelReservation(row.id)
  ElMessage.success('已取消')
  load()
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin: 4px 0 16px;
  border-radius: 8px;
}
.tip :deep(.el-alert__title) {
  font-size: 12.5px;
  line-height: 1.7;
}
.pos {
  color: var(--forest);
  font-size: 14px;
}
.ok {
  color: var(--forest);
  font-weight: 600;
}
.muted {
  color: var(--ink-3);
}
</style>
