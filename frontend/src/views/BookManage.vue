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
          <el-button @click="openImport">批量导入</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 列表 -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column label="封面" width="72">
          <template #default="{ row }">
            <div class="tbl-cover">
              <img
                v-if="row.coverUrl"
                :src="row.coverUrl"
                :alt="`${row.name} 封面`"
                loading="lazy"
              />
              <span v-else class="tbl-cover-none">无图</span>
            </div>
          </template>
        </el-table-column>
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
        <el-form-item label="封面">
          <div class="cover-field">
            <!-- 预览始终按 3:4 书封比例裁切，避免上传不同尺寸图片把表单撑变形 -->
            <div class="cover-preview">
              <img v-if="form.coverUrl" :src="form.coverUrl" alt="封面预览" />
              <span v-else class="cover-preview-empty">暂无封面</span>
            </div>
            <div class="cover-side">
              <div class="cover-btns">
                <el-upload
                  :show-file-list="false"
                  :http-request="handleCoverUpload"
                  accept="image/jpeg,image/png,image/webp"
                >
                  <el-button size="small" :loading="coverUploading">上传图片</el-button>
                </el-upload>
                <el-button
                  v-if="form.coverUrl"
                  size="small"
                  text
                  type="danger"
                  @click="form.coverUrl = ''"
                >
                  移除
                </el-button>
              </div>
              <p class="cover-hint">支持 jpg / png / webp，不超过 2MB；上传后自动填入下方地址</p>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="封面地址">
          <el-input
            v-model="form.coverUrl"
            placeholder="如 /covers/9787508643324.jpg 或完整 URL，留空使用配色书封"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入 CSV -->
    <el-dialog v-model="importVisible" title="批量导入图书" width="680px">
      <div class="import-tip">
        <p>1. 下载模板后按列填写，<b>isbn 与书名必填</b>，作者留空会记为「佚名」。</p>
        <p>2. 分类按名称匹配，库里没有的分类会自动创建，不用先建分类。</p>
        <p>3. ISBN 已存在的行会被跳过（不覆盖已有馆藏与库存），坏行会给出具体原因。</p>
        <p>4. 单次最多 2000 行、文件不超过 2MB；Excel 请另存为「CSV UTF-8」。</p>
      </div>

      <div class="import-actions">
        <el-button @click="downloadTemplate">下载模板</el-button>
        <el-upload
          :show-file-list="false"
          :auto-upload="false"
          accept=".csv"
          :on-change="onFileChange"
        >
          <el-button>选择 CSV 文件</el-button>
        </el-upload>
        <span class="file-name">{{ importFile ? importFile.name : '未选择文件' }}</span>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="doImport">
          开始导入
        </el-button>
      </div>

      <div v-if="importResult" class="import-result">
        <div class="import-stat">
          <span
            >共 <b>{{ importResult.total }}</b> 行</span
          >
          <span class="ok">成功 {{ importResult.success }}</span>
          <span class="skip">跳过 {{ importResult.skipped }}</span>
          <span class="fail">失败 {{ importResult.failed }}</span>
        </div>
        <el-table
          v-if="importResult.issues && importResult.issues.length"
          :data="importResult.issues"
          size="small"
          max-height="240"
        >
          <el-table-column prop="line" label="行号" width="66" />
          <el-table-column prop="isbn" label="ISBN" width="140" />
          <el-table-column prop="name" label="书名" min-width="120" show-overflow-tooltip />
          <el-table-column label="结果" width="76">
            <template #default="{ row }">
              <el-tag size="small" :type="row.result === 'SKIPPED' ? 'info' : 'danger'">
                {{ row.result === 'SKIPPED' ? '跳过' : '失败' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reason" label="原因" min-width="190" show-overflow-tooltip />
        </el-table>
        <p v-else class="import-all-ok">全部导入成功，没有需要处理的异常行。</p>
      </div>

      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getBookPage,
  addBook,
  updateBook,
  changeBookStatus,
  deleteBook,
  importBooks,
  downloadImportTemplate
} from '../api/book'
import { uploadCover } from '../api/file'
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
  description: '',
  coverUrl: ''
}
const form = reactive({ ...emptyForm })

// ---- 封面（含上传）----
const coverUploading = ref(false)

async function handleCoverUpload({ file }) {
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('封面不能超过 2MB')
    return
  }
  coverUploading.value = true
  try {
    const res = await uploadCover(file)
    form.coverUrl = res.data.url
    ElMessage.success('封面上传成功')
  } finally {
    coverUploading.value = false
  }
}

// ---- 批量导入 ----
const importVisible = ref(false)
const importFile = ref(null)
const importing = ref(false)
const importResult = ref(null)

function openImport() {
  importFile.value = null
  importResult.value = null
  importVisible.value = true
}

function onFileChange(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  if (!raw.name.toLowerCase().endsWith('.csv')) {
    ElMessage.warning('请选择 .csv 文件（Excel 请另存为「CSV UTF-8」）')
    return
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.warning('文件不能超过 2MB，请拆分后分批导入')
    return
  }
  importFile.value = raw
  importResult.value = null
}

async function downloadTemplate() {
  const blob = await downloadImportTemplate()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '图书导入模板.csv'
  a.click()
  URL.revokeObjectURL(url)
}

async function doImport() {
  if (!importFile.value) return
  importing.value = true
  try {
    const res = await importBooks(importFile.value)
    importResult.value = res.data
    if (res.data.success > 0) {
      ElMessage.success(`成功导入 ${res.data.success} 本图书`)
      query.pageNum = 1
      load()
      // 分类可能被自动创建，刷新下拉避免新增的分类看不到
      const categoryRes = await listCategories()
      categories.value = categoryRes.data
    } else if (res.data.skipped === 0 && res.data.failed === 0) {
      ElMessage.warning('文件里没有有效数据行')
    }
  } finally {
    importing.value = false
  }
}

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
/* 列表封面缩略图：无图时显示浅色占位，避免表格高度跳动 */
.tbl-cover {
  width: 38px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  background: var(--line);
  display: grid;
  place-items: center;
}
.tbl-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center top;
  display: block;
}
.tbl-cover-none {
  font-size: 10px;
  color: var(--ink-3);
}

/* 表单里的封面预览 + 上传区 */
.cover-field {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.cover-preview {
  width: 78px;
  aspect-ratio: 3 / 4;
  flex: none;
  border-radius: 6px;
  overflow: hidden;
  background: var(--line);
  display: grid;
  place-items: center;
  box-shadow: 0 6px 14px -10px rgba(0, 0, 0, 0.5);
}
.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.cover-preview-empty {
  font-size: 11px;
  color: var(--ink-3);
}
.cover-side {
  padding-top: 2px;
}
.cover-btns {
  display: flex;
  align-items: center;
  gap: 4px;
}
.cover-hint {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.6;
  color: var(--ink-3);
}

/* 批量导入 */
.import-tip {
  padding: 12px 16px;
  border-radius: 8px;
  background: var(--paper-2, #f6f2e9);
  font-size: 13px;
  line-height: 1.9;
  color: var(--ink-2, #4a4a44);
}
.import-tip p {
  margin: 0;
}
.import-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 16px 0 4px;
  flex-wrap: wrap;
}
.file-name {
  font-size: 13px;
  color: var(--ink-3);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.import-result {
  margin-top: 16px;
}
.import-stat {
  display: flex;
  gap: 20px;
  font-size: 13px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 10px;
}
.import-stat .ok {
  color: #2e7d5b;
}
.import-stat .skip {
  color: var(--ink-3);
}
.import-stat .fail {
  color: #b4463c;
}
.import-all-ok {
  margin: 0;
  font-size: 13px;
  color: #2e7d5b;
}
</style>
