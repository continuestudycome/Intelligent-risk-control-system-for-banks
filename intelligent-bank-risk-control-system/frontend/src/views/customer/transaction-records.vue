<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span class="title">交易记录</span>
        <el-button type="primary" plain @click="reloadNow">刷新</el-button>
      </div>
    </template>

    <p class="hint">
      修改下方筛选条件后列表会<strong>自动刷新</strong>（约 350ms 防抖）；也可点「刷新」立即拉取。
    </p>
    <el-form :inline="true" :model="query" class="query-form" @submit.prevent="() => {}">
      <el-form-item label="交易类型">
        <el-select v-model="query.transactionType" clearable placeholder="全部" style="width: 140px">
          <el-option label="转账" :value="1" />
          <el-option label="消费" :value="2" />
          <el-option label="取现" :value="3" />
          <el-option label="还款" :value="4" />
        </el-select>
      </el-form-item>
      <el-form-item label="风险状态">
        <el-select v-model="query.riskStatus" clearable placeholder="全部" style="width: 140px">
          <el-option label="低风险" value="LOW" />
          <el-option label="中风险" value="MEDIUM" />
          <el-option label="高风险" value="HIGH" />
          <el-option label="已拦截" value="INTERCEPTED" />
          <el-option label="已确认欺诈" value="CONFIRMED_FRAUD" />
        </el-select>
      </el-form-item>
      <el-form-item label="最小金额">
        <el-input-number v-model="query.minAmount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="最大金额">
        <el-input-number v-model="query.maxAmount" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border>
      <el-table-column prop="transactionNo" label="流水号" min-width="200" />
      <el-table-column prop="transactionTypeName" label="交易类型" width="100" />
      <el-table-column prop="amount" label="金额" width="110" />
      <el-table-column prop="fromAccount" label="付款账户" min-width="160" />
      <el-table-column prop="toAccount" label="收款账户" min-width="160" />
      <el-table-column prop="riskStatusName" label="风险状态" width="110" />
      <el-table-column prop="handleResultName" label="处置状态" width="110" />
      <el-table-column prop="transactionTime" label="交易时间" min-width="180" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="detailVisible" title="交易详情" width="700px">
    <el-skeleton v-if="detailLoading" :rows="6" animated />
    <el-empty v-else-if="!current" description="暂无详情数据" />
    <el-descriptions v-else :column="2" border>
      <el-descriptions-item label="流水号">{{ current.transactionNo }}</el-descriptions-item>
      <el-descriptions-item label="交易类型">{{ current.transactionTypeName }}</el-descriptions-item>
      <el-descriptions-item label="金额">{{ current.amount }}</el-descriptions-item>
      <el-descriptions-item label="交易时间">{{ current.transactionTime }}</el-descriptions-item>
      <el-descriptions-item label="付款账户">{{ current.fromAccount }}</el-descriptions-item>
      <el-descriptions-item label="收款账户">{{ current.toAccount }}</el-descriptions-item>
      <el-descriptions-item label="交易地点">{{ current.transactionProvince }} {{ current.transactionCity }}</el-descriptions-item>
      <el-descriptions-item label="风险状态">{{ current.riskStatusName }}</el-descriptions-item>
      <el-descriptions-item label="处置状态">{{ current.handleResultName }}</el-descriptions-item>
      <el-descriptions-item label="交易用途" :span="2">{{ current.purpose }}</el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useDebouncedLoader } from '@/composables/useDebouncedReload'
import { transactionApi } from '@/api/transaction'
import type { TransactionQuery, TransactionRecord } from '@/api/types/transaction'

const loading = ref(false)
const records = ref<TransactionRecord[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref<TransactionRecord | null>(null)

const query = reactive<TransactionQuery>({
  transactionType: undefined,
  riskStatus: '',
  minAmount: undefined,
  maxAmount: undefined
})

const loadRecords = async () => {
  loading.value = true
  try {
    const params = { ...query }
    if (params.riskStatus === '') params.riskStatus = undefined
    const data = await transactionApi.queryRecords(params)
    records.value = data
  } finally {
    loading.value = false
  }
}

const { scheduleReload, reloadNow } = useDebouncedLoader(loadRecords)

watch(
  () => query,
  () => scheduleReload(),
  { deep: true }
)

const resetQuery = () => {
  query.transactionType = undefined
  query.riskStatus = ''
  query.minAmount = undefined
  query.maxAmount = undefined
  reloadNow()
}

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  current.value = null
  try {
    const detail = await transactionApi.getDetail(id)
    current.value = detail
  } catch {
    detailVisible.value = false
    ElMessage.error('获取交易详情失败，请稍后重试')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  reloadNow()
})
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
.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin: 0 0 12px;
}
.query-form {
  margin-bottom: 12px;
}
</style>
