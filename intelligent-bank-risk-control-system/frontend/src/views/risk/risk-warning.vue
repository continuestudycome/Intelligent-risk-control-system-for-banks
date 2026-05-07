<template>
  <div class="risk-warning-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">风险预警</span>
          <div class="actions">
            <el-button type="primary" plain :loading="loading" @click="load">刷新</el-button>
            <router-link class="el-link el-link--primary" to="/risk/fraud-alert">欺诈告警处理 →</router-link>
          </div>
        </div>
      </template>

      <p class="hint">
        汇总<strong>欺诈预警（模型+规则）</strong>与<strong>近 {{ lookbackDays }} 天内非低风险交易</strong>；上方指标基于流水时间与预警表实时统计。预警复核请在「欺诈告警处理」中闭环。
      </p>

      <el-row :gutter="12" class="stat-row" v-loading="loading">
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-card shadow="never" class="stat-card">
            <el-statistic title="待处理欺诈预警" :value="stats.pendingFraudAlerts">
              <template #suffix><span class="unit">笔</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-card shadow="never" class="stat-card warn">
            <el-statistic title="24h 中风险交易" :value="stats.mediumRiskTransactions24h">
              <template #suffix><span class="unit">笔</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-card shadow="never" class="stat-card danger">
            <el-statistic title="24h 高危/拦截/欺诈" :value="stats.criticalRiskTransactions24h">
              <template #suffix><span class="unit">笔</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="5">
          <el-card shadow="never" class="stat-card">
            <el-statistic title="7日拦截累计" :value="stats.interceptedTransactions7d">
              <template #suffix><span class="unit">笔</span></template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="12" :md="8" :lg="4">
          <el-card shadow="never" class="stat-card dark">
            <el-statistic title="黑名单客户" :value="stats.blacklistCustomers">
              <template #suffix><span class="unit">人</span></template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <h3 class="sec-title">欺诈预警动态</h3>
      <el-table :data="alerts" border size="small" empty-text="暂无预警记录">
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="transactionNo" label="流水号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户ID" width="96" />
        <el-table-column label="等级" width="96">
          <template #default="{ row }">
            <el-tag :type="row.alertLevel === 'HIGH' ? 'danger' : 'warning'" size="small">{{
              row.alertLevel
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitRules" label="命中规则" min-width="160" show-overflow-tooltip />
        <el-table-column label="模型分" width="96">
          <template #default="{ row }">{{
            row.mlScore != null ? Number(row.mlScore).toFixed(4) : '—'
          }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="alertStatusType(row.status)" size="small">{{ alertStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <h3 class="sec-title">近期风险交易（{{ lookbackDays }} 天内非低风险）</h3>
      <el-table :data="txns" border size="small" empty-text="暂无符合条件的交易">
        <el-table-column prop="transactionTime" label="交易时间" width="170" />
        <el-table-column prop="transactionNo" label="流水号" min-width="180" show-overflow-tooltip />
        <el-table-column prop="customerId" label="客户ID" width="88" />
        <el-table-column prop="customerName" label="客户" width="100" show-overflow-tooltip />
        <el-table-column prop="transactionTypeName" label="类型" width="80" />
        <el-table-column prop="amount" label="金额" width="110" />
        <el-table-column label="风险" width="110">
          <template #default="{ row }">
            <el-tag :type="riskTag(row.riskStatus)" size="small">{{ row.riskStatusName }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="handleResultName" label="处置" width="100" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openTxn(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="交易详情" width="720px" destroy-on-close>
      <el-skeleton v-if="detailLoading" :rows="8" animated />
      <el-descriptions v-else-if="detail" :column="2" border>
        <el-descriptions-item label="流水号">{{ detail.transactionNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.transactionTypeName }}</el-descriptions-item>
        <el-descriptions-item label="金额">{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ detail.transactionTime }}</el-descriptions-item>
        <el-descriptions-item label="客户ID">{{ detail.customerId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ detail.customerName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="付款账户">{{ detail.fromAccount }}</el-descriptions-item>
        <el-descriptions-item label="收款账户">{{ detail.toAccount }}</el-descriptions-item>
        <el-descriptions-item label="地点" :span="2"
          >{{ detail.transactionProvince }} {{ detail.transactionCity }}</el-descriptions-item
        >
        <el-descriptions-item label="风险">{{ detail.riskStatusName }}</el-descriptions-item>
        <el-descriptions-item label="处置">{{ detail.handleResultName }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.riskMessage" label="说明" :span="2">{{
          detail.riskMessage
        }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { riskWarningApi } from '@/api/riskWarning'
import { riskTransactionApi } from '@/api/riskTransaction'
import type { FraudAlertRow, RiskWarningStats } from '@/api/types/riskWarning'
import type { TransactionRecord } from '@/api/types/transaction'

const lookbackDays = 7

const loading = ref(false)
const stats = reactive<RiskWarningStats>({
  pendingFraudAlerts: 0,
  mediumRiskTransactions24h: 0,
  criticalRiskTransactions24h: 0,
  interceptedTransactions7d: 0,
  blacklistCustomers: 0
})
const alerts = ref<FraudAlertRow[]>([])
const txns = ref<TransactionRecord[]>([])

const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<TransactionRecord | null>(null)

async function load() {
  loading.value = true
  try {
    const data = await riskWarningApi.getOverview()
    Object.assign(stats, data.stats)
    alerts.value = data.fraudAlerts
    txns.value = data.riskTransactions
  } finally {
    loading.value = false
  }
}

function alertStatusType(s: string) {
  if (s === 'PENDING') return 'warning'
  if (s === 'REVIEWING') return 'primary'
  if (s === 'CONFIRMED') return 'danger'
  if (s === 'IGNORED') return 'info'
  return ''
}

function alertStatusLabel(s: string) {
  const m: Record<string, string> = {
    PENDING: '待处理',
    REVIEWING: '复核中',
    CONFIRMED: '已确认',
    IGNORED: '已关闭'
  }
  return m[s] || s
}

function riskTag(rs: string) {
  if (rs === 'HIGH' || rs === 'CONFIRMED_FRAUD') return 'danger'
  if (rs === 'MEDIUM') return 'warning'
  if (rs === 'INTERCEPTED') return 'info'
  return ''
}

async function openTxn(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await riskTransactionApi.getDetail(id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.risk-warning-page {
  max-width: 1280px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 16px;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin-bottom: 16px;
}
.stat-row {
  margin-bottom: 8px;
}
.stat-card {
  margin-bottom: 12px;
}
.stat-card.warn :deep(.el-statistic__number) {
  color: var(--el-color-warning);
}
.stat-card.danger :deep(.el-statistic__number) {
  color: var(--el-color-danger);
}
.stat-card.dark :deep(.el-statistic__number) {
  color: var(--el-text-color-primary);
}
.unit {
  font-size: 14px;
  margin-left: 4px;
  color: var(--el-text-color-secondary);
}
.sec-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}
</style>
