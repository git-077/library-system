<template>
  <div class="notice-page">
    <header class="page-head">
      <div>
        <p class="eyebrow">ANNOUNCEMENTS</p>
        <h1 class="page-title">系统公告</h1>
        <p class="page-sub">共 {{ total }} 条公告，了解闭馆、上新与借阅规则调整</p>
      </div>
      <button v-if="store.isAdmin" class="btn-gold head-btn" @click="openAdd">发布公告</button>
    </header>

    <div class="notice-list" v-loading="loading">
      <article v-for="item in list" :key="item.id" class="notice">
        <div class="date">
          <b>{{ dayOf(item.createTime) }}</b>
          <span>{{ monthOf(item.createTime) }}</span>
        </div>

        <div class="notice-main">
          <h2 class="notice-title">{{ item.title }}</h2>
          <p class="notice-content">{{ item.content }}</p>

          <div class="notice-foot">
            <span class="notice-time">{{ item.createTime }}</span>
            <div v-if="store.isAdmin" class="acts">
              <button class="act" @click="openEdit(item)">编辑</button>
              <button class="act danger" @click="handleDelete(item)">删除</button>
            </div>
          </div>
        </div>
      </article>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="暂无公告" />

    <div class="pager" v-if="total > query.pageSize">
      <el-pagination
        background
        layout="prev, pager, next"
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
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑公告' : '发布公告'" width="560px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" placeholder="例如：11 月闭馆检修通知" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="填写公告正文" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getNoticePage, addNotice, updateNotice, deleteNotice } from '../api/notice'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const query = reactive({ pageNum: 1, pageSize: 10 })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const emptyForm = { id: null, title: '', content: '' }
const form = reactive({ ...emptyForm })

// "2026-11-15 10:20:00" → 15 / 11月
const dayOf = t => (t ? String(t).slice(8, 10) : '--')
const monthOf = t => (t ? `${Number(String(t).slice(5, 7))}月` : '')

async function load() {
  loading.value = true
  try {
    const res = await getNoticePage(query)
    list.value = res.data.records || []
    total.value = Number(res.data.total || 0)
  } finally {
    loading.value = false
  }
}

function openAdd() {
  Object.assign(form, emptyForm)
  dialogVisible.value = true
}

function openEdit(row) {
  Object.assign(form, emptyForm, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title) {
    ElMessage.warning('请输入公告标题')
    return
  }
  if (form.id) {
    await updateNotice(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addNotice(form)
    ElMessage.success('发布成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.notice-page {
  max-width: 1120px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}
.eyebrow {
  margin: 0 0 8px;
  font-size: 10.5px;
  letter-spacing: 0.28em;
  color: var(--ink-3);
  font-weight: 600;
}
.page-title {
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
}
.page-sub {
  margin: 7px 0 0;
  font-size: 12.5px;
  color: var(--ink-3);
}
.head-btn {
  height: 38px;
  font-size: 13.5px;
  padding: 0 20px;
  letter-spacing: 0.06em;
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 80px;
}

.notice {
  display: flex;
  gap: 18px;
  padding: 20px 22px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 14px;
  transition:
    box-shadow 0.2s,
    border-color 0.2s;
}
.notice:hover {
  box-shadow: var(--shadow);
  border-color: #dbcdb4;
}

.date {
  flex: none;
  width: 54px;
  text-align: center;
  padding-top: 2px;
  border-right: 1px solid var(--line-soft);
  padding-right: 16px;
}
.date b {
  display: block;
  font-family: Georgia, 'Songti SC', serif;
  font-size: 24px;
  line-height: 1.1;
  color: var(--forest);
}
.date span {
  font-size: 11px;
  color: var(--ink-3);
  letter-spacing: 0.08em;
}

.notice-main {
  min-width: 0;
  flex: 1;
}
.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  letter-spacing: 0.01em;
}
.notice-content {
  margin: 8px 0 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--ink-2);
  white-space: pre-wrap;
}

.notice-foot {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.notice-time {
  font-size: 11.5px;
  color: var(--ink-3);
  letter-spacing: 0.04em;
}

.acts {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.18s;
}
.notice:hover .acts {
  opacity: 1;
}
.act {
  appearance: none;
  border: none;
  background: transparent;
  font-family: inherit;
  font-size: 12.5px;
  color: var(--ink-3);
  cursor: pointer;
  padding: 3px 9px;
  border-radius: 6px;
}
.act:hover {
  background: var(--paper-2);
  color: var(--forest);
}
.act.danger:hover {
  background: #f7e7e4;
  color: #a2544a;
}

.pager {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
