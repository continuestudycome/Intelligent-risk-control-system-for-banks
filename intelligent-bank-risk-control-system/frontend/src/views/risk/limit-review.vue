<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span class="title">授信上调复核</span>
        <div class="actions">
          <el-button type="primary" plain :loading="batchLoading" @click="runBatch">手动跑批调整</el-button>
          <el-button plain @click="load">刷新</el-button>
        </div>
      </div>
    </template>

    <p class="hint">
      系统按信用评分与交易行为定期重算建议额度：<strong>下调自动生效</strong>；<strong>上调仅生成工单</strong>，需在此通过/驳回。
    </p>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="id" label="工单号" width="90" />
      <el-table-column prop="customerId" label="客户ID" width="90" />
      <el-table-column prop="customerName" label="客户" width="120" />
      <el-table-column prop="currentTotalLimit" label="当前授信(元)" width="130" />
      <el-table-column prop="proposedTotalLimit" label="建议调至(元)" width="130" />
      <el-table-column prop="triggerScore" label="触发评分" width="100" />
      <el-table-column prop="triggerRiskLevel" label="等级" width="70" />
      <el-table-column prop="reason" label="说明" min-width="220" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" min-width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="doApprove(row)">通过</el-button>
          <el-button link type="danger" @click="doReject(row)">驳回</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { riskLimitApi } from '@/api/riskLimit'
import type { LimitAdjustPending } from '@/api/types/riskLimit'

const loading = ref(false)
const batchLoading = ref(false)
const list = ref<LimitAdjustPending[]>([])

const load = async () => {
  loading.value = true
  try {
    list.value = await riskLimitApi.pendingList()
  } finally {
    loading.value = false
  }
}

const runBatch = async () => {
  try {
    await ElMessageBox.confirm('将立即对所有客户执行一轮授信重算（下调自动、上调继续生成工单），是否继续？', '手动跑批', {
      type: 'warning'
    })
  } catch {
    return
  }
  batchLoading.value = true
  try {
    await riskLimitApi.runAdjustmentBatch()
    ElMessage.success('批跑已触发')
    await load()
  } finally {
    batchLoading.value = false
  }
}

const doApprove = async (row: LimitAdjustPending) => {
  try {
    const { value } = await ElMessageBox.prompt('复核意见（可空）', '通过上调', {
      confirmButtonText: '提交',
      cancelButtonText: '取消',
      inputPlaceholder: '选填'
    })
    await riskLimitApi.approve(row.id, value || undefined)
    ElMessage.success('已通过')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel') return
  }
}

const doReject = async (row: LimitAdjustPending) => {
  try {
    const { value } = await ElMessageBox.prompt('驳回原因（可空）', '驳回上调', {
      confirmButtonText: '提交',
      cancelButtonText: '取消'
    })
    await riskLimitApi.reject(row.id, value || undefined)
    ElMessage.success('已驳回')
    await load()
  } catch (e: unknown) {
    if (e === 'cancel') return
  }
}

onMounted(load)
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
.actions {
  display: flex;
  gap: 8px;
}
.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 14px;
  line-height: 1.6;
}
</style>
