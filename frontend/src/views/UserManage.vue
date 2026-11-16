<template>
  <div>
    <el-card shadow="never">
      <el-form inline @submit.prevent>
        <el-form-item label="用户名">
          <el-input
            v-model="query.username"
            placeholder="按用户名模糊搜索"
            clearable
            style="width: 220px"
            @keyup.enter="load"
            @clear="load"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
          <el-button type="success" @click="openAdd">新增用户</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'admin' ? '管理员' : '读者' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="118">
          <template #default="{ row }">
            <span class="status-cell">
              <el-switch
                :model-value="row.status === 1"
                :disabled="row.username === 'admin'"
                @change="val => toggleStatus(row, val)"
              />
              <em :class="{ off: row.status !== 1 }">{{ row.status === 1 ? '启用' : '禁用' }}</em>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="216" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text @click="openEdit(row)">编辑</el-button>
            <el-button size="small" text type="warning" @click="handleReset(row)"
              >重置密码</el-button
            >
            <el-button
              size="small"
              text
              type="danger"
              :disabled="row.username === 'admin'"
              @click="handleDelete(row)"
              >删除</el-button
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

    <!-- 新增 / 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名">
          <el-input v-model="form.username" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="密码" v-if="!form.id">
          <el-input v-model="form.password" type="password" show-password placeholder="6~20 位" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.role" style="width: 100%" :disabled="form.username === 'admin'">
            <el-option label="读者" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
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
import {
  getUserPage,
  addUser,
  updateUser,
  changeUserStatus,
  resetUserPassword,
  deleteUser
} from '../api/user'

const query = reactive({ pageNum: 1, pageSize: 10, username: '' })
const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialogVisible = ref(false)
const emptyForm = { id: null, username: '', password: '', realName: '', role: 'user' }
const form = reactive({ ...emptyForm })

async function load() {
  loading.value = true
  try {
    const res = await getUserPage(query)
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
  Object.assign(form, {
    id: row.id,
    username: row.username,
    password: '',
    realName: row.realName,
    role: row.role
  })
  dialogVisible.value = true
}

async function handleSave() {
  if (form.id) {
    await updateUser(form.id, { realName: form.realName, role: form.role })
    ElMessage.success('修改成功')
  } else {
    if (!form.username || !form.password) {
      ElMessage.warning('请填写用户名和密码')
      return
    }
    await addUser(form)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

async function toggleStatus(row, enabled) {
  await changeUserStatus(row.id, enabled ? 1 : 0)
  ElMessage.success(enabled ? '已启用' : '已禁用')
  load()
}

async function handleReset(row) {
  await ElMessageBox.confirm(`确定将「${row.username}」的密码重置为 123456 吗？`, '重置密码', {
    type: 'warning'
  })
  await resetUserPassword(row.id)
  ElMessage.success('密码已重置为 123456')
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除用户「${row.username}」吗？删除为软删除。`, '删除确认', {
    type: 'warning'
  })
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.status-cell {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.status-cell em {
  font-style: normal;
  font-size: 12px;
  color: var(--forest);
}
.status-cell em.off {
  color: var(--ink-3);
}
</style>
