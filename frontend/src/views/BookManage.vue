<template>
  <div>
    <!-- 工具栏 -->
    <el-card shadow="never" class="search-bar">
      <el-form inline @submit.prevent>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="书名 / 作者 / ISBN"
            clearable
            style="width: 220px"
            @keyup.enter="load"
            @clear="load"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select
            v-model="query.categoryId"
            placeholder="全部分类"
            clearable
            style="width: 160px"
            @change="load"
          >
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openAdd">新增图书</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="isbn" label="ISBN" width="140" />
        <el-table-column prop="name" label="书名" min-width="150" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="110" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="90" />
        <el-table-column prop="price" label="定价(¥)" width="90" />
        <el-table-column label="在馆/已借" width="90">
          <template #default="{ row }">{{ row.stock }} / {{ row.borrowed }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text @click="toggleStatus(row)">
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑图书' : '新增图书'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="ISBN" required>
          <el-input v-model="form.isbn" placeholder="图书 ISBN" />
        </el-form-item>
        <el-form-item label="书名" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="作者" required>
          <el-input v-model="form.author" />
        </el-form-item>
        <el-form-item label="出版社">
          <el-input v-model="form.publisher" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="定价">
          <el-input-number
            v-model="form.price"
            :min="0"
            :precision="2"
            :step="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="库存" v-if="!form.id">
          <el-input-number v-model="form.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBookPage, addBook, updateBook, changeBookStatus, deleteBook } from '../api/book'
import { listCategories } from '../api/category'

const query = reactive({ pageNum: 1, pageSize: 10, keyword: '', categoryId: null })
const list = ref([])
const total = ref(0)
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const emptyForm = {
  id: null,
  isbn: '',
  name: '',
  author: '',
  publisher: '',
  categoryId: null,
  price: 0,
  stock: 1,
  description: ''
}
const form = reactive({ ...emptyForm })

async function load() {
  loading.value = true
  try {
    const res = await getBookPage(query)
    list.value = res.data.records
    total.value = Number(res.data.total)
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
  if (!form.isbn || !form.name || !form.author) {
    ElMessage.warning('ISBN、书名、作者为必填项')
    return
  }
  if (form.id) {
    await updateBook(form)
    ElMessage.success('修改成功')
  } else {
    await addBook(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row) {
  const next = row.status === 1 ? 0 : 1
  await changeBookStatus(row.id, next)
  ElMessage.success(next === 1 ? '已上架' : '已下架')
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(
    `确定删除《${row.name}》吗？删除为软删除，借阅历史不受影响。`,
    '删除确认',
    { type: 'warning' }
  )
  await deleteBook(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(async () => {
  const res = await listCategories()
  categories.value = res.data
  load()
})
</script>

<style scoped>
.search-bar {
  margin-bottom: 16px;
}
</style>
