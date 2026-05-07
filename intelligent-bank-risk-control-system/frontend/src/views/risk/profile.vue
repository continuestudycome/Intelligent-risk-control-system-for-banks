<template>
  <div class="risk-profile-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">客户风险画像</span>
          <el-button plain :loading="listLoading" @click="reloadNow">刷新列表</el-button>
        </div>
      </template>
      <p class="hint">
        检索客户后点击表格行或「加载画像」，将聚合展示<strong>基础信息、信用评估、近 {{ txnLookbackDays }} 日交易风险、欺诈告警与信贷申请</strong>。关键词变更后列表自动刷新（约
        350ms 防抖）。
      </p>

      <el-form :inline="true" class="search-form" @submit.prevent="() => {}">
        <el-form-item label="关键词">
          <el-input
            v-model="keyword"
            clearable
            placeholder="姓名 / 手机号 / 客户ID"
            style="width: 260px"
          />
        </el-form-item>
      </el-form>

      <el-table
        :data="customerList"
        v-loading="listLoading"
        border
        size="small"
        highlight-current-row
        :current-row-key="selectedBrief?.customerId"
        row-key="customerId"
        @row-click="onRowClick"
      >
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="customerNo" label="客户号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="creditLevel" label="档案等级" width="100" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openProfile(row)">加载画像</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selectedBrief" class="profile-card" v-loading="profileLoading">
      <template #header>
        <div class="detail-head">
          <span>
            风险画像 — ID {{ profile?.basic.customerId }} · {{ profile?.basic.realName || '—' }}
          </span>
          <div class="head-actions">
            <el-button text type="primary" :loading="profileLoading" @click="refreshProfile">重新加载</el-button>
            <el-button text type="primary" @click="closeProfile">关闭</el-button>
          </div>
        </div>
      </template>

      <template v-if="profile">
        <div v-if="profile.portraitTags?.length" class="tag-bar">
          <el-tag v-for="(t, i) in profile.portraitTags" :key="i" type="warning" effect="plain" class="tag-item">{{
            t
          }}</el-tag>
        </div>

        <h3 class="sec-title">基本信息</h3>
        <el-descriptions :column="3" border size="small">
          <el-descriptions-item label="客户号">{{ profile.basic.customerNo }}</el-descriptions-item>
          <el-descriptions-item label="姓名">{{ profile.basic.realName }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ profile.basic.phoneMasked }}</el-descriptions-item>
          <el-descriptions-item label="证件">{{ profile.basic.idCardMasked }}</el-descriptions-item>
          <el-descriptions-item label="地区">{{ profile.basic.province }} {{ profile.basic.city }}</el-descriptions-item>
          <el-descriptions-item label="档案等级">{{ profile.basic.creditLevel || '—' }}</el-descriptions-item>
          <el-descriptions-item label="黑名单">
            <el-tag v-if="profile.basic.isBlacklist === 1" type="danger" size="small">是</el-tag>
            <span v-else>否</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="profile.basic.isBlacklist === 1" label="黑名单原因" :span="2">{{
            profile.basic.blacklistReason || '—'
          }}</el-descriptions-item>
          <el-descriptions-item label="年收入">{{ fmtMoney(profile.basic.annualIncome) }}</el-descriptions-item>
          <el-descriptions-item label="资产">{{ fmtMoney(profile.basic.assetAmount) }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ profile.basic.registerTime || '—' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="sec-title">信用评估</h3>
        <el-empty v-if="!profile.credit.evaluated" description="尚未生成信用评分" />
        <el-row v-else :gutter="16" class="credit-row">
          <el-col :xs="24" :sm="8">
            <div class="score-block">
              <div class="label">当前评分</div>
              <div class="score-num">{{ profile.credit.score }}</div>
              <el-progress :percentage="scorePercent" :color="progressColor" :stroke-width="10" :show-text="false" />
              <div class="range-hint">区间 300 – 900</div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="8">
            <div class="score-block">
              <div class="label">模型风险等级</div>
              <div class="risk-line">
                <el-tag :type="riskTagType" size="large" effect="dark">
                  {{ profile.credit.riskLevel }} · {{ profile.credit.riskLevelName }}
                </el-tag>
              </div>
              <div class="meta" v-if="profile.credit.modelVersion">模型版本：{{ profile.credit.modelVersion }}</div>
              <div class="meta" v-if="profile.credit.evaluatedAt">评估时间：{{ profile.credit.evaluatedAt }}</div>
            </div>
          </el-col>
          <el-col :xs="24" :sm="8">
            <div class="score-block">
              <div class="label">相对违约风险参考</div>
              <div class="hint-num">{{ badPct }}</div>
            </div>
          </el-col>
        </el-row>

        <h3 class="sec-title">交易风险（近 {{ profile.transactions.lookbackDays }} 日）</h3>
        <el-row :gutter="12" class="stat-row">
          <el-col :xs="12" :sm="6">
            <el-statistic title="总笔数" :value="profile.transactions.totalCount" />
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-statistic title="总金额" :value="profile.transactions.totalAmount" :precision="2" prefix="¥" />
          </el-col>
          <el-col :xs="12" :sm="4">
            <el-statistic title="低风险" :value="profile.transactions.lowCount" />
          </el-col>
          <el-col :xs="12" :sm="4">
            <el-statistic title="中风险" :value="profile.transactions.mediumCount" />
          </el-col>
          <el-col :xs="12" :sm="4">
            <el-statistic title="高风险" :value="profile.transactions.highCount" />
          </el-col>
          <el-col :xs="12" :sm="4">
            <el-statistic title="已拦截" :value="profile.transactions.interceptedCount" />
          </el-col>
          <el-col :xs="12" :sm="4">
            <el-statistic title="确认欺诈" :value="profile.transactions.confirmedFraudCount" />
          </el-col>
        </el-row>
        <p v-if="!profile.transactions.recentRiskyTransactions?.length" class="muted">近期无中高风险/拦截类交易抽样</p>
        <el-table
          v-else
          :data="profile.transactions.recentRiskyTransactions"
          border
          size="small"
          class="sub-table"
        >
          <el-table-column prop="transactionTime" label="时间" min-width="170" />
          <el-table-column prop="transactionNo" label="流水号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="transactionTypeName" label="类型" width="80" />
          <el-table-column prop="amount" label="金额" width="110" />
          <el-table-column label="风险" width="100">
            <template #default="{ row }">
              <el-tag :type="txnRiskTag(row.riskStatus)" size="small">{{ row.riskStatusName }}</el-tag>
            </template>
          </el-table-column>
        </el-table>

        <h3 class="sec-title">欺诈告警</h3>
        <el-empty v-if="!profile.fraudAlerts?.length" description="暂无欺诈告警记录" />
        <el-table v-else :data="profile.fraudAlerts" border size="small" class="sub-table">
          <el-table-column prop="createTime" label="时间" width="170" />
          <el-table-column prop="transactionNo" label="流水号" min-width="170" show-overflow-tooltip />
          <el-table-column prop="alertLevel" label="级别" width="88" />
          <el-table-column label="模型分" width="100">
            <template #default="{ row }">
              {{ row.mlScore != null ? Number(row.mlScore).toFixed(4) : '—' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="fraudStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="hitRulesSummary" label="命中规则" min-width="200" show-overflow-tooltip />
        </el-table>

        <h3 class="sec-title">信贷申请</h3>
        <el-row :gutter="12" class="stat-row">
          <el-col :xs="12" :sm="6">
            <el-statistic title="待审批" :value="profile.loans.pendingCount" />
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-statistic title="已通过" :value="profile.loans.approvedCount" />
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-statistic title="已拒绝" :value="profile.loans.rejectedCount" />
          </el-col>
          <el-col :xs="12" :sm="6">
            <el-statistic title="其他状态" :value="profile.loans.otherStatusCount" />
          </el-col>
        </el-row>
        <el-empty v-if="!profile.loans.recentApplications?.length" description="暂无信贷申请记录" />
        <el-table v-else :data="profile.loans.recentApplications" border size="small" class="sub-table">
          <el-table-column prop="createTime" label="提交时间" width="170" />
          <el-table-column prop="applicationNo" label="申请编号" min-width="180" show-overflow-tooltip />
          <el-table-column prop="applyTypeName" label="类型" width="100" />
          <el-table-column prop="applyAmount" label="金额" width="110" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small">{{ row.currentStatusName }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useDebouncedLoader } from '@/composables/useDebouncedReload'
import { riskCreditApi } from '@/api/riskCredit'
import { riskProfileApi } from '@/api/riskProfile'
import type { CreditCustomerBrief } from '@/api/types/riskCredit'
import type { CustomerRiskProfile } from '@/api/types/riskProfile'

const keyword = ref('')
const listLoading = ref(false)
const customerList = ref<CreditCustomerBrief[]>([])

const selectedBrief = ref<CreditCustomerBrief | null>(null)
const profileLoading = ref(false)
const profile = ref<CustomerRiskProfile | null>(null)

const txnLookbackDays = computed(() => profile.value?.transactions.lookbackDays ?? 90)

const loadList = async () => {
  listLoading.value = true
  try {
    customerList.value = await riskCreditApi.searchCustomers(keyword.value.trim() || undefined)
  } finally {
    listLoading.value = false
  }
}

const { scheduleReload, reloadNow } = useDebouncedLoader(loadList)
watch(keyword, () => scheduleReload())

const loadProfile = async (customerId: number) => {
  profileLoading.value = true
  profile.value = null
  try {
    profile.value = await riskProfileApi.getProfile(customerId)
  } catch {
    ElMessage.error('加载风险画像失败')
  } finally {
    profileLoading.value = false
  }
}

const openProfile = (row: CreditCustomerBrief) => {
  selectedBrief.value = row
  void loadProfile(row.customerId)
}

const onRowClick = (row: CreditCustomerBrief) => {
  openProfile(row)
}

const refreshProfile = () => {
  if (selectedBrief.value) void loadProfile(selectedBrief.value.customerId)
}

const closeProfile = () => {
  selectedBrief.value = null
  profile.value = null
}

onMounted(() => reloadNow())

const scorePercent = computed(() => {
  const s = profile.value?.credit?.score
  if (s == null) return 0
  return Math.min(100, Math.max(0, ((s - 300) / 600) * 100))
})

const progressColor = computed(() => {
  const s = profile.value?.credit?.score ?? 0
  if (s >= 720) return '#67c23a'
  if (s >= 620) return '#409eff'
  if (s >= 520) return '#e6a23c'
  return '#f56c6c'
})

const riskTagType = computed(() => {
  const r = profile.value?.credit?.riskLevel
  if (r === 'A') return 'success'
  if (r === 'B') return 'primary'
  if (r === 'C') return 'warning'
  return 'danger'
})

const badPct = computed(() => {
  const d = profile.value?.credit
  if (!d) return '—'
  const h = d.badProbabilityHint
  if (h != null && h !== '') {
    const n = typeof h === 'number' ? h : Number(h)
    if (!Number.isNaN(n)) return `${(n * 100).toFixed(2)}%`
  }
  const snap = d.featureSnapshot as Record<string, unknown> | undefined
  if (snap?.badProbabilityHint != null) {
    const n = Number(snap.badProbabilityHint)
    if (!Number.isNaN(n)) return `${(n * 100).toFixed(2)}%`
  }
  return '—'
})

function fmtMoney(v: unknown) {
  if (v == null || v === '') return '—'
  const n = typeof v === 'number' ? v : Number(v)
  if (Number.isNaN(n)) return '—'
  return `¥ ${n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function txnRiskTag(status: string) {
  if (status === 'HIGH' || status === 'CONFIRMED_FRAUD') return 'danger'
  if (status === 'MEDIUM') return 'warning'
  if (status === 'INTERCEPTED') return 'info'
  return ''
}

function fraudStatusTag(s: string) {
  if (s === 'PENDING') return 'warning'
  if (s === 'CONFIRMED') return 'danger'
  if (s === 'IGNORED') return 'info'
  return ''
}
</script>

<style scoped>
.risk-profile-page {
  max-width: 1200px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  font-weight: 600;
}
.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
}
.search-form {
  margin-bottom: 12px;
}
.profile-card {
  margin-top: 16px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.head-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
.tag-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
.tag-item {
  margin-right: 0;
}
.sec-title {
  margin: 20px 0 12px;
  font-size: 15px;
  font-weight: 600;
}
.credit-row {
  margin-top: 4px;
}
.score-block {
  padding: 12px 8px;
}
.label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
}
.score-num {
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 10px;
}
.range-hint {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  margin-top: 8px;
}
.risk-line {
  margin-bottom: 10px;
}
.meta {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-top: 6px;
}
.hint-num {
  font-size: 24px;
  font-weight: 600;
  margin: 8px 0;
}
.stat-row {
  margin-bottom: 12px;
}
.sub-table {
  margin-top: 8px;
}
.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 8px 0;
}
</style>
