<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span class="title">交易反欺诈</span>
        <el-button type="primary" plain @click="refreshAll">刷新</el-button>
      </div>
    </template>

    <el-tabs v-model="activeTab" @tab-change="onTab">
      <el-tab-pane label="预警复核" name="alerts" />
      <el-tab-pane label="欺诈案例库" name="cases" />
    </el-tabs>

    <template v-if="activeTab === 'alerts'">
    <el-form :inline="true" class="query-form">
      <el-form-item label="状态">
        <el-select v-model="filterStatus" clearable placeholder="全部" style="width: 160px" @change="loadData">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已确认欺诈" value="CONFIRMED" />
          <el-option label="误报关闭" value="IGNORED" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="transactionNo" label="流水号" min-width="190" />
      <el-table-column prop="customerId" label="客户ID" width="100" />
      <el-table-column prop="alertLevel" label="预警等级" width="100">
        <template #default="{ row }">
          <el-tag :type="row.alertLevel === 'HIGH' ? 'danger' : 'warning'">{{ row.alertLevel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hitRules" label="命中规则" min-width="160" show-overflow-tooltip />
      <el-table-column prop="mlScore" label="异常分数" width="100" />
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="产生时间" min-width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="showSnapshot(row)">特征</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="success" @click="openReview(row, 'FALSE_POSITIVE')">误报</el-button>
          <el-button v-if="row.status === 'PENDING'" link type="danger" @click="openReview(row, 'CONFIRM_FRAUD')">确认欺诈</el-button>
        </template>
      </el-table-column>
    </el-table>
    </template>

    <template v-else>
      <p class="hint">以下记录由人工确认欺诈后写入，可用于后续模型迭代与规则优化。</p>
      <el-table :data="caseList" v-loading="caseLoading" border>
        <el-table-column prop="id" label="案例ID" width="90" />
        <el-table-column prop="transactionId" label="交易ID" width="100" />
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="fraudType" label="欺诈类型" width="120" />
        <el-table-column prop="labelSource" label="标注" width="90">
          <template #default="{ row }">{{ row.labelSource === 1 ? '人工' : '自动' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="入库时间" min-width="170" />
      </el-table>
    </template>
  </el-card>

  <el-dialog v-model="snapVisible" title="特征快照" width="640px">
    <pre class="json-pre">{{ prettySnap }}</pre>
  </el-dialog>

  <el-dialog v-model="reviewVisible" :title="reviewTitle" width="520px" @closed="reviewTarget = null">
    <el-form label-width="100px">
      <el-alert type="info" :closable="false" show-icon class="mb-3">
        {{ reviewTip }}
      </el-alert>
      <el-form-item label="欺诈类型" v-if="reviewDecision === 'CONFIRM_FRAUD'">
        <el-select v-model="fraudType" style="width: 100%">
          <el-option label="盗刷 THEFT" value="THEFT" />
          <el-option label="洗钱 LAUNDER" value="LAUNDER" />
          <el-option label="套现 CASHOUT" value="CASHOUT" />
          <el-option label="伪冒 IMPERSONATE" value="IMPERSONATE" />
        </el-select>
      </el-form-item>
      <el-form-item label="复核意见">
        <el-input v-model="reviewComment" type="textarea" :rows="3" maxlength="2000" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fraudApi } from '@/api/fraud'
import type { FraudAlert, FraudCase } from '@/api/types/fraud'

const activeTab = ref('alerts')
const loading = ref(false)
const list = ref<FraudAlert[]>([])
const filterStatus = ref<string | undefined>(undefined)
const caseLoading = ref(false)
const caseList = ref<FraudCase[]>([])

const snapVisible = ref(false)
const snapRaw = ref('')
const prettySnap = computed(() => {
  try {
    const o = JSON.parse(snapRaw.value || '{}')
    return JSON.stringify(o, null, 2)
  } catch {
    return snapRaw.value
  }
})

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTarget = ref<FraudAlert | null>(null)
const reviewDecision = ref<'CONFIRM_FRAUD' | 'FALSE_POSITIVE'>('FALSE_POSITIVE')
const reviewComment = ref('')
const fraudType = ref('THEFT')

const reviewTitle = computed(() =>
  reviewDecision.value === 'CONFIRM_FRAUD' ? '确认欺诈并入案例库' : '标记为误报'
)

const reviewTip = computed(() =>
  reviewDecision.value === 'CONFIRM_FRAUD'
    ? '确认后将写入欺诈案例库，并将关联交易标记为已确认欺诈。'
    : '标记误报后预警关闭，不写入案例库。'
)

const statusLabel = (s: string) => {
  const m: Record<string, string> = {
    PENDING: '待处理',
    CONFIRMED: '已确认',
    IGNORED: '误报'
  }
  return m[s] || s
}

const statusType = (s: string) => {
  if (s === 'CONFIRMED') return 'danger'
  if (s === 'IGNORED') return 'info'
  return 'warning'
}

const loadData = async () => {
  loading.value = true
  try {
    list.value = await fraudApi.listAlerts(filterStatus.value || undefined)
  } finally {
    loading.value = false
  }
}

const loadCases = async () => {
  caseLoading.value = true
  try {
    caseList.value = await fraudApi.listCases()
  } finally {
    caseLoading.value = false
  }
}

const refreshAll = async () => {
  await loadData()
  await loadCases()
}

const onTab = (name: string | number) => {
  if (name === 'cases') loadCases()
}

onMounted(() => {
  loadData()
  loadCases()
})

const showSnapshot = (row: FraudAlert) => {
  snapRaw.value = row.featureSnapshot || '{}'
  snapVisible.value = true
}

const openReview = (row: FraudAlert, decision: 'CONFIRM_FRAUD' | 'FALSE_POSITIVE') => {
  reviewTarget.value = row
  reviewDecision.value = decision
  reviewComment.value = ''
  fraudType.value = 'THEFT'
  reviewVisible.value = true
}

const submitReview = async () => {
  if (!reviewTarget.value) return
  reviewSubmitting.value = true
  try {
    await fraudApi.review(reviewTarget.value.id, {
      decision: reviewDecision.value,
      comment: reviewComment.value || undefined,
      fraudType: reviewDecision.value === 'CONFIRM_FRAUD' ? fraudType.value : undefined
    })
    ElMessage.success('复核已保存')
    reviewVisible.value = false
    await loadData()
    await loadCases()
  } finally {
    reviewSubmitting.value = false
  }
}

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
.query-form {
  margin-bottom: 12px;
}
.json-pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  max-height: 420px;
  overflow: auto;
}
.mb-3 {
  margin-bottom: 12px;
}
.hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
  margin: 0 0 12px;
}
</style>
