<template>
  <div>
    <el-card shadow="never">
      <div style="margin-bottom: 16px">
        <el-button type="success" @click="openAdd">新增分类</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" width="200" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="分类名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
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
import { listCategories, addCategory, updateCategory, deleteCategory } from '../api/category'

const list = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const emptyForm = { id: null, name: '', description: '' }
const form = reactive({ ...emptyForm })

async function load() {
  loading.value = true
  try {
    const res = await listCategories()
    list.value = res.data
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
  if (!form.name) {
    ElMessage.warning('请输入分类名称')
    return
  }
  if (form.id) {
    await updateCategory(form.id, form)
    ElMessage.success('修改成功')
  } else {
    await addCategory(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
