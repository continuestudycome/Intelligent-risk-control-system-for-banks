<template>
  <div class="credit-page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-card v-else class="hero">
      <div class="hero-top">
        <div>
          <h2>个人信用评分</h2>
          <p class="sub">
            综合收入、资产、征信授权、交易行为等特征，由 <strong>ai_services 逻辑回归模型</strong> 计算 300–900
            分；服务不可用时自动使用本地兜底规则。
          </p>
        </div>
        <el-button type="primary" :loading="evalLoading" @click="doEvaluate">立即重新评估</el-button>
      </div>

      <template v-if="limitSummary">
        <h4 class="sec-title">授信额度（定期动态调整）</h4>
        <el-alert
          v-if="limitSummary.pendingIncreaseReview"
          type="warning"
          show-icon
          :closable="false"
          class="mb-3"
          title="有一笔授信上调建议正在风控复核中，通过后额度才会生效。"
        />
        <el-descriptions v-if="limitSummary.hasLimitAccount" :column="2" border class="mb-3">
          <el-descriptions-item label="授信总额(元)">{{ fmtMoney(limitSummary.totalLimit) }}</el-descriptions-item>
          <el-descriptions-item label="已用(元)">{{ fmtMoney(limitSummary.usedLimit) }}</el-descriptions-item>
          <el-descriptions-item label="可用(元)">{{ fmtMoney(limitSummary.availableLimit) }}</el-descriptions-item>
          <el-descriptions-item label="单笔限额(元)">{{ fmtMoney(limitSummary.singleLimit) }}</el-descriptions-item>
          <el-descriptions-item label="日累计限额(元)">{{ fmtMoney(limitSummary.dailyLimit) }}</el-descriptions-item>
          <el-descriptions-item v-if="limitSummary.pendingProposedTotal != null" label="待复核建议额度(元)">
            {{ fmtMoney(limitSummary.pendingProposedTotal) }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-else class="muted">{{ limitSummary.lastAdjustHint || '完成首次信用评估后将初始化授信账户。' }}</p>
        <p class="muted small-print">
          系统按评分与交易行为定期重算：<strong>下调自动执行</strong>；<strong>上调需风控在「授信上调复核」中通过</strong>。
        </p>
        <el-divider />
      </template>

      <el-empty v-if="!data?.evaluated" description="尚未生成信用评分，请点击「立即重新评估」" />

      <template v-else-if="data?.evaluated">
        <el-row :gutter="20" class="stat-row">
          <el-col :span="8">
            <div class="score-block">
              <div class="label">当前评分</div>
              <div class="score-num">{{ data.score }}</div>
              <el-progress
                :percentage="scorePercent"
                :color="progressColor"
                :stroke-width="10"
                :show-text="false"
              />
              <div class="range-hint">区间 300 – 900</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="score-block">
              <div class="label">风险等级</div>
              <div class="risk-line">
                <el-tag :type="riskTagType" size="large" effect="dark">{{ data.riskLevel }} ·
                  {{ data.riskLevelName }}</el-tag>
              </div>
              <div class="meta" v-if="data.modelVersion">模型版本：{{ data.modelVersion }}</div>
              <div class="meta" v-if="data.evaluatedAt">最近评估：{{ data.evaluatedAt }}</div>
              <div class="meta" v-if="data.calcDurationMs != null">计算耗时：{{ data.calcDurationMs }} ms</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="score-block">
              <div class="label">相对违约风险参考</div>
              <div class="hint-num">{{ badPct }}</div>
              <p class="hint-desc">
                由模型输出的「信用较好」概率推导的互补指标（演示用途，非征信报送）。
              </p>
            </div>
          </el-col>
        </el-row>

        <el-divider />

        <h4 class="sec-title">模型指标（来自智能服务 / 库表元数据）</h4>
        <el-descriptions v-if="metricEntries.length" :column="2" border>
          <el-descriptions-item v-for="[k, v] in metricEntries" :key="k" :label="k">
            {{ formatMetric(v) }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-else class="muted">暂无指标明细（请先完成一次评估或执行 sql/credit_model_seed.sql）</p>

        <h4 class="sec-title">最近一次特征快照</h4>
        <el-collapse>
          <el-collapse-item title="展开查看归一化特征与数据来源" name="1">
            <pre class="json-pre">{{ prettySnap }}</pre>
          </el-collapse-item>
        </el-collapse>

        <h4 class="sec-title">评分历史与趋势</h4>
        <el-table v-if="trendRows.length" :data="trendRows" border size="small">
          <el-table-column prop="createTime" label="评估时间" min-width="170" />
          <el-table-column prop="score" label="分数" width="90" />
          <el-table-column prop="riskLevel" label="等级" width="80" />
          <el-table-column prop="modelVersion" label="模型版本" min-width="200" show-overflow-tooltip />
        </el-table>
        <p v-else class="muted">多次评估后在此展示趋势。</p>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { creditApi } from '@/api/credit'
import type { CreditScoreOverview, CustomerLimitSummary } from '@/api/types/credit'

const loading = ref(true)
const evalLoading = ref(false)
const data = ref<CreditScoreOverview | null>(null)
const limitSummary = ref<CustomerLimitSummary | null>(null)

const fmtMoney = (n: number | undefined) => {
  if (n == null || Number.isNaN(n)) return '—'
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const load = async () => {
  loading.value = true
  try {
    const [ov, lim] = await Promise.all([creditApi.getOverview(), creditApi.getLimitSummary()])
    data.value = ov
    limitSummary.value = lim
  } finally {
    loading.value = false
  }
}

const doEvaluate = async () => {
  evalLoading.value = true
  try {
    data.value = await creditApi.evaluate()
    limitSummary.value = await creditApi.getLimitSummary()
    ElMessage.success('信用评估已完成')
  } finally {
    evalLoading.value = false
  }
}

onMounted(load)

const scorePercent = computed(() => {
  const s = data.value?.score
  if (s == null) return 0
  return Math.min(100, Math.max(0, ((s - 300) / 600) * 100))
})

const progressColor = computed(() => {
  const s = data.value?.score ?? 0
  if (s >= 720) return '#67c23a'
  if (s >= 620) return '#409eff'
  if (s >= 520) return '#e6a23c'
  return '#f56c6c'
})

const riskTagType = computed(() => {
  const r = data.value?.riskLevel
  if (r === 'A') return 'success'
  if (r === 'B') return 'primary'
  if (r === 'C') return 'warning'
  return 'danger'
})

const badPct = computed(() => {
  const h = data.value?.badProbabilityHint
  if (h != null && h !== '') return `${(Number(h) * 100).toFixed(2)}%`
  const snap = data.value?.featureSnapshot as Record<string, unknown> | undefined
  if (snap?.badProbabilityHint != null) {
    const n = Number(snap.badProbabilityHint)
    if (!Number.isNaN(n)) return `${(n * 100).toFixed(2)}%`
  }
  return '—'
})

const metricEntries = computed(() => {
  const m = data.value?.metrics
  if (!m) return [] as [string, unknown][]
  return Object.entries(m)
})

const formatMetric = (v: unknown) => {
  if (typeof v === 'number') return v.toFixed(4)
  return String(v)
}

const prettySnap = computed(() => {
  try {
    return JSON.stringify(data.value?.featureSnapshot ?? {}, null, 2)
  } catch {
    return '{}'
  }
})

const trendRows = computed(() => data.value?.recentTrend ?? [])
</script>

<style scoped>
.credit-page {
  max-width: 1100px;
  margin: 0 auto;
}
.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}
.sub {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-top: 8px;
  line-height: 1.6;
}
.stat-row {
  margin-top: 8px;
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
  font-size: 42px;
  font-weight: 700;
  letter-spacing: 1px;
  margin-bottom: 10px;
  color: var(--el-text-color-primary);
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
  font-size: 28px;
  font-weight: 600;
  margin: 8px 0;
}
.hint-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
.sec-title {
  margin: 20px 0 12px;
  font-size: 15px;
}
.json-pre {
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 12px;
  max-height: 360px;
  overflow: auto;
  background: var(--el-fill-color-light);
  padding: 12px;
  border-radius: 6px;
}
.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.small-print {
  font-size: 12px;
  margin-top: 8px;
}
.mb-3 {
  margin-bottom: 12px;
}
</style>
