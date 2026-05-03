<template>
  <el-config-provider :locale="zhCn">
    <main class="manage-page">
      <section class="toolbar">
        <div>
          <p class="eyebrow">ADMIN CONSOLE</p>
          <h1>用户管理</h1>
        </div>
        <el-input
          v-model="keyword"
          class="search-input"
          clearable
          placeholder="搜索用户名、手机号或邮箱"
          size="large"
          @keyup.enter="triggerSearch"
        >
          <template #prefix>
            <span class="search-icon">⌕</span>
          </template>
        </el-input>
      </section>

      <el-card class="table-card" shadow="never">
        <el-table v-loading="loading" :data="users" row-key="id" class="user-table">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="用户名" min-width="120" prop="username" show-overflow-tooltip />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" effect="light" round>
                {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="手机号" min-width="120" prop="phone">
            <template #default="{ row }">{{ row.phone || '-' }}</template>
          </el-table-column>
          <el-table-column label="邮箱" min-width="160" prop="email" show-overflow-tooltip>
            <template #default="{ row }">{{ row.email || '-' }}</template>
          </el-table-column>
          <el-table-column label="VIP等级" width="100">
            <template #default="{ row }">
              <el-badge :value="`V${row.vipLevel}`" :type="row.vipLevel > 0 ? 'warning' : 'info'" class="vip-badge" />
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'" effect="light" round>
                {{ row.status === 'ACTIVE' ? '正常' : '已禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="注册时间" min-width="160">
            <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.role !== 'ADMIN'"
                :type="row.status === 'ACTIVE' ? 'danger' : 'success'"
                link
                @click="confirmToggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            background
            layout="total, prev, pager, next"
            @current-change="fetchUsers"
          />
        </div>
      </el-card>
    </main>
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { getAdminUsers, updateUserStatus, type UserManageItem } from '../../api/users'

const users = ref<UserManageItem[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
let searchTimer: ReturnType<typeof window.setTimeout> | undefined

function formatDate(value: string) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

function scheduleSearch() {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  searchTimer = window.setTimeout(() => {
    page.value = 1
    fetchUsers()
  }, 400)
}

function triggerSearch() {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
  page.value = 1
  fetchUsers()
}

async function fetchUsers() {
  loading.value = true
  try {
    const response = await getAdminUsers({
      page: page.value,
      pageSize: pageSize.value,
      keyword: keyword.value.trim() || undefined
    })
    const result = response.data.data
    users.value = result.records || []
    total.value = result.total || 0
    page.value = result.page || page.value
    pageSize.value = result.pageSize || pageSize.value
  } catch {
    users.value = []
    total.value = 0
    ElMessage.error('用户列表加载失败')
  } finally {
    loading.value = false
  }
}

async function confirmToggleStatus(user: UserManageItem) {
  const isDisabling = user.status === 'ACTIVE'
  const actionText = isDisabling ? '禁用' : '启用'
  const newStatus = isDisabling ? 'DISABLED' : 'ACTIVE'

  try {
    await ElMessageBox.confirm(`确认${actionText}用户“${user.username}”？`, `${actionText}用户`, {
      confirmButtonText: actionText,
      cancelButtonText: '取消',
      type: isDisabling ? 'warning' : 'info'
    })
    await updateUserStatus(user.id, newStatus)
    ElMessage.success(`用户已${actionText}`)
    await fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(`${actionText}失败`)
    }
  }
}

watch(keyword, scheduleSearch)

onMounted(fetchUsers)

onUnmounted(() => {
  if (searchTimer) {
    window.clearTimeout(searchTimer)
  }
})
</script>

<style scoped>
.manage-page {
  min-height: 100vh;
  padding: 32px clamp(18px, 4vw, 56px);
  color: #1f2937;
  background: #f5f7fb;
}

.toolbar {
  display: flex;
  gap: 24px;
  align-items: center;
  justify-content: space-between;
  max-width: 1180px;
  margin: 0 auto 22px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #e94560;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.18em;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 32px;
}

.search-input {
  width: 320px;
  border-radius: 999px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.search-icon {
  color: #e94560;
  font-size: 22px;
  font-weight: 800;
}

.table-card {
  max-width: 1180px;
  margin: 0 auto;
  border: 0;
  border-radius: 18px;
}

.user-table {
  width: 100%;
}

.vip-badge {
  margin-top: 4px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

@media (max-width: 720px) {
  .toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .search-input {
    width: 100%;
  }

  .pagination-wrap {
    justify-content: center;
  }
}
</style>
