<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span class="title">申请进度</span>
        <el-button type="primary" plain @click="loadList">刷新</el-button>
      </div>
    </template>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="applicationNo" label="申请编号" min-width="190" />
      <el-table-column prop="applyTypeName" label="类型" width="100" />
      <el-table-column prop="applyAmount" label="申请金额" width="120" />
      <el-table-column label="期限" width="90">
        <template #default="{ row }">
          {{ row.applyType === 3 && row.applyTerm === 0 ? '—' : row.applyTerm + ' 月' }}
        </template>
      </el-table-column>
      <el-table-column prop="currentStatusName" label="当前状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.currentStatus)">{{ row.currentStatusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" min-width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="detailVisible" title="申请详情" width="720px" destroy-on-close>
    <el-skeleton v-if="detailLoading" :rows="8" animated />
    <template v-else-if="current">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ current.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ current.applyTypeName }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ current.applyAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="期限">
          {{ current.applyType === 3 && current.applyTerm === 0 ? '—' : current.applyTerm + ' 月' }}
        </el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ current.applyPurpose }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(current.currentStatus)">{{ current.currentStatusName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="终审结果">{{ current.finalResultName }}</el-descriptions-item>
        <el-descriptions-item v-if="current.finalAmount != null" label="核定金额">{{ current.finalAmount }} 元</el-descriptions-item>
        <el-descriptions-item v-if="current.finalTerm != null" label="核定期限">{{ current.finalTerm }} 月</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ current.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ current.updateTime || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="timeline-title">审批记录</div>
      <el-timeline v-if="current.approvalRecords?.length">
        <el-timeline-item
          v-for="rec in current.approvalRecords"
          :key="rec.id"
          :timestamp="rec.createTime"
          placement="top"
        >
          <p>
            <strong>{{ rec.reviewerName }}</strong>
            <el-tag size="small" class="ml-2" :type="rec.action === 'PASS' ? 'success' : 'danger'">
              {{ rec.action === 'PASS' ? '通过' : '拒绝' }}
            </el-tag>
          </p>
          <p v-if="rec.comment" class="comment">{{ rec.comment }}</p>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录（待风控处理）" />
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { customerLoanApi } from '@/api/loanApplication'
import type { LoanApplication } from '@/api/types/loanApplication'

const loading = ref(false)
const list = ref<LoanApplication[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref<LoanApplication | null>(null)

const statusTagType = (s: string) => {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'warning'
}

const loadList = async () => {
  loading.value = true
  try {
    list.value = await customerLoanApi.list()
  } finally {
    loading.value = false
  }
}

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await customerLoanApi.detail(id)
  } catch {
    detailVisible.value = false
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => loadList())
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.timeline-title {
  margin: 16px 0 8px;
  font-weight: 600;
}
.comment {
  color: #606266;
  margin-top: 4px;
}
.ml-2 {
  margin-left: 8px;
}
</style>
