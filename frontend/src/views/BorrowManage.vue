<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="状态">
          <el-select
            v-model="query.status"
            placeholder="全部"
            clearable
            style="width: 160px"
            @change="load"
          >
            <el-option label="借出中" :value="0" />
            <el-option label="已归还" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
        <el-form-item>
          <el-button :loading="checking" @click="handleCheckOverdue">扫描逾期</el-button>
          <el-button :loading="reminding" @click="handleDueSoon">到期提醒</el-button>
          <el-button :loading="exporting" @click="handleExport">导出记录</el-button>
        </el-form-item>
        <el-form-item label="格式">
          <el-select v-model="exportFormat" style="width: 130px">
            <el-option label="Excel (xlsx)" value="xlsx" />
            <el-option label="CSV" value="csv" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="记录ID" width="80" />
        <el-table-column prop="username" label="借阅人" width="120" />
        <el-table-column prop="bookName" label="书名" min-width="160" show-overflow-tooltip />
        <el-table-column prop="borrowDate" label="借出时间" width="160" />
        <el-table-column prop="dueDate" label="应还时间" width="160" />
        <el-table-column prop="returnDate" label="归还时间" width="160" />
        <el-table-column label="续借次数" width="90">
          <template #default="{ row }">{{ row.renewCount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 0 && row.overdue === 1" type="danger" size="small"
              >逾期未还</el-tag
            >
            <el-tag v-else :type="statusType(row)" size="small">{{ statusText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 0"
              type="success"
              size="small"
              @click="handleReturn(row)"
              >归还</el-button
            >
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
import {
  checkOverdue,
  exportBorrows,
  getBorrowPage,
  returnBook,
  remindDueSoon
} from '../api/borrow'
import { borrowStatus, borrowStatusType } from '../utils'

const query = reactive({ pageNum: 1, pageSize: 10, status: null })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const checking = ref(false)
const reminding = ref(false)
const exporting = ref(false)
/** 导出文件格式：xlsx 用 EasyExcel 产出真 Excel（数字列可直接求和），csv 兼容脚本处理 */
const exportFormat = ref('xlsx')

const statusText = borrowStatus
const statusType = borrowStatusType

async function load() {
  loading.value = true
  try {
    const res = await getBorrowPage(query)
    list.value = res.data.records
    total.value = Number(res.data.total)
  } finally {
    loading.value = false
  }
}

async function handleReturn(row) {
  await ElMessageBox.confirm(`确定为「${row.username}」归还《${row.bookName}》吗？`, '归还确认')
  await returnBook(row.id)
  ElMessage.success('归还成功')
  load()
}

/** 手动触发逾期扫描：刷新逾期标记，若存在逾期会同时发布当日催还公告 */
async function handleCheckOverdue() {
  checking.value = true
  try {
    const res = await checkOverdue()
    const d = res.data || {}
    const extra = d.noticeCreated ? `，已发布公告《${d.noticeTitle}》` : ''
    ElMessage.success(
      `扫描完成：当前逾期 ${d.overdueCount} 条，新标记 ${d.markedCount} 条，恢复 ${d.clearedCount} 条${extra}`
    )
    load()
  } finally {
    checking.value = false
  }
}

/** 手动触发到期前提醒：只发给读者本人，不动借阅数据，重复点也不会重复发 */
async function handleDueSoon() {
  reminding.value = true
  try {
    const res = await remindDueSoon()
    const d = res.data || {}
    ElMessage.success(`已发送 ${d.sent} 条到期提醒（${d.aheadDays} 天内到期的在借记录）`)
  } finally {
    reminding.value = false
  }
}

/** 触发浏览器下载：CSV 已写 UTF-8 BOM，xlsx 为标准 Excel 文件，双击打开均不乱码 */
async function handleExport() {
  exporting.value = true
  try {
    const blob = await exportBorrows({
      status: query.status ?? undefined,
      format: exportFormat.value
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    const stamp = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    a.href = url
    a.download = `borrows-${stamp}.${exportFormat.value}`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } finally {
    exporting.value = false
  }
}

onMounted(load)
</script>
