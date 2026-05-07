<template>
  <div class="risk-txn-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">交易记录查看</span>
          <el-button type="primary" plain :loading="loading" @click="reloadNow">刷新</el-button>
        </div>
      </template>

      <p class="hint">
        支持按 <strong>客户ID</strong> 或 <strong>客户关键词</strong>（姓名/手机号/客户主键）缩小范围；与交易类型、风险状态、金额、时间组合筛选。单次最多返回
        <strong>500</strong> 条，请尽量缩窄条件。修改下方任一条件后列表会<strong>自动刷新</strong>（输入约 350ms 防抖）。
      </p>

      <el-form :inline="true" :model="query" class="query-form" @submit.prevent>
        <el-form-item label="客户ID">
          <el-input
            v-model="customerIdStr"
            clearable
            placeholder="可选，精确客户主键"
            style="width: 150px"
            @clear="onClearCustomerId"
          />
        </el-form-item>
        <el-form-item label="客户关键词">
          <el-input
            v-model="query.customerKeyword"
            clearable
            placeholder="姓名/手机/ID，与上项二选一优先ID"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="交易类型">
          <el-select v-model="query.transactionType" clearable placeholder="全部" style="width: 130px">
            <el-option label="转账" :value="1" />
            <el-option label="消费" :value="2" />
            <el-option label="取现" :value="3" />
            <el-option label="还款" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="风险状态">
          <el-select v-model="query.riskStatus" clearable placeholder="全部" style="width: 130px">
            <el-option label="低风险" value="LOW" />
            <el-option label="中风险" value="MEDIUM" />
            <el-option label="高风险" value="HIGH" />
            <el-option label="已拦截" value="INTERCEPTED" />
            <el-option label="已确认欺诈" value="CONFIRMED_FRAUD" />
          </el-select>
        </el-form-item>
        <el-form-item label="最小金额">
          <el-input-number v-model="query.minAmount" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="最大金额">
          <el-input-number v-model="query.maxAmount" :min="0" :precision="2" :controls="false" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="query.startTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选填"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker
            v-model="query.endTime"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            placeholder="选填"
            style="width: 190px"
          />
        </el-form-item>
        <el-form-item>
          <el-button @click="resetQuery">重置条件</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="records" v-loading="loading" border size="small">
        <el-table-column prop="customerId" label="客户ID" width="90" />
        <el-table-column prop="customerName" label="客户姓名" width="100" show-overflow-tooltip />
        <el-table-column prop="customerPhone" label="手机" min-width="120" show-overflow-tooltip />
        <el-table-column prop="transactionNo" label="流水号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="transactionTypeName" label="类型" width="80" />
        <el-table-column prop="amount" label="金额" width="110" />
        <el-table-column prop="fromAccount" label="付款账户" min-width="150" show-overflow-tooltip />
        <el-table-column prop="toAccount" label="收款账户" min-width="150" show-overflow-tooltip />
        <el-table-column prop="riskStatusName" label="风险" width="100" />
        <el-table-column prop="handleResultName" label="处置" width="100" />
        <el-table-column prop="transactionTime" label="交易时间" min-width="170" />
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="交易详情" width="720px" destroy-on-close>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-empty v-else-if="!current" description="暂无数据" />
      <el-descriptions v-else :column="2" border>
        <el-descriptions-item label="客户ID">{{ current.customerId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="客户姓名">{{ current.customerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机" :span="2">{{ current.customerPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="流水号">{{ current.transactionNo }}</el-descriptions-item>
        <el-descriptions-item label="交易类型">{{ current.transactionTypeName }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ current.amount }}</el-descriptions-item>
        <el-descriptions-item label="交易时间">{{ current.transactionTime }}</el-descriptions-item>
        <el-descriptions-item label="付款账户">{{ current.fromAccount }}</el-descriptions-item>
        <el-descriptions-item label="收款账户">{{ current.toAccount }}</el-descriptions-item>
        <el-descriptions-item label="交易地点" :span="2"
          >{{ current.transactionProvince }} {{ current.transactionCity }}</el-descriptions-item
        >
        <el-descriptions-item label="风险状态">{{ current.riskStatusName }}</el-descriptions-item>
        <el-descriptions-item label="处置状态">{{ current.handleResultName }}</el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ current.purpose }}</el-descriptions-item>
        <el-descriptions-item v-if="current.riskMessage" label="风险说明" :span="2">{{
          current.riskMessage
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useDebouncedLoader } from '@/composables/useDebouncedReload'
import { riskTransactionApi } from '@/api/riskTransaction'
import type { RiskTransactionQuery, TransactionRecord } from '@/api/types/transaction'

const loading = ref(false)
const records = ref<TransactionRecord[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const current = ref<TransactionRecord | null>(null)

const customerIdStr = ref('')

const query = reactive<RiskTransactionQuery>({
  customerId: undefined,
  customerKeyword: undefined,
  transactionType: undefined,
  riskStatus: '',
  minAmount: undefined,
  maxAmount: undefined,
  startTime: undefined,
  endTime: undefined
})

function syncCustomerIdFromStr() {
  const t = customerIdStr.value?.trim() ?? ''
  if (!t) {
    query.customerId = undefined
    return
  }
  const n = Number(t)
  query.customerId = Number.isFinite(n) && n > 0 ? n : undefined
}

watch(customerIdStr, () => {
  syncCustomerIdFromStr()
})

const onClearCustomerId = () => {
  query.customerId = undefined
}

const loadRecords = async () => {
  loading.value = true
  try {
    const params: RiskTransactionQuery = { ...query }
    if (params.riskStatus === '') params.riskStatus = undefined
    records.value = await riskTransactionApi.queryRecords(params)
  } catch {
    ElMessage.error('加载失败')
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
  customerIdStr.value = ''
  query.customerId = undefined
  query.customerKeyword = undefined
  query.transactionType = undefined
  query.riskStatus = ''
  query.minAmount = undefined
  query.maxAmount = undefined
  query.startTime = undefined
  query.endTime = undefined
  reloadNow()
}

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  current.value = null
  try {
    current.value = await riskTransactionApi.getDetail(id)
  } catch {
    detailVisible.value = false
    ElMessage.error('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  reloadNow()
})
</script>

<style scoped>
.risk-txn-page {
  max-width: 1400px;
  margin: 0 auto;
}
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
  margin-bottom: 12px;
}
.query-form {
  margin-bottom: 12px;
}
</style>
