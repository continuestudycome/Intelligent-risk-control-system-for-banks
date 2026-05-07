<template>
  <div class="credit-query">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">信用评分查询</span>
          <el-button plain :loading="listLoading" @click="reloadNow">刷新列表</el-button>
        </div>
      </template>
      <p class="hint">
        按 <strong>客户姓名、手机号</strong> 模糊搜索，或输入 <strong>客户主键 ID</strong> 精确匹配；留空则展示最近登记的客户。修改关键词后列表会<strong>自动刷新</strong>（约 350ms
        防抖）。点击下方某一行的「查看详情」加载该客户的评分概况与历史记录。
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
        @row-click="onRowClick"
      >
        <el-table-column prop="customerId" label="客户ID" width="100" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="customerNo" label="客户号" min-width="130" show-overflow-tooltip />
        <el-table-column prop="creditLevel" label="档案等级" width="100" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="selected" class="detail-card" v-loading="detailLoading">
      <template #header>
        <div class="detail-head">
          <span>评分详情 — ID {{ selected.customerId }} · {{ selected.realName || '—' }}</span>
          <el-button text type="primary" @click="closeDetail">关闭</el-button>
        </div>
      </template>

      <el-empty v-if="!detail?.evaluated" description="该客户尚未产生信用评分记录" />

      <template v-else>
        <el-row :gutter="20" class="stat-row">
          <el-col :span="8">
            <div class="score-block">
              <div class="label">当前评分</div>
              <div class="score-num">{{ detail?.score }}</div>
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
                <el-tag :type="riskTagType" size="large" effect="dark"
                  >{{ detail?.riskLevel }} · {{ detail?.riskLevelName }}</el-tag
                >
              </div>
              <div class="meta" v-if="detail?.modelVersion">模型版本：{{ detail.modelVersion }}</div>
              <div class="meta" v-if="detail?.evaluatedAt">最近评估：{{ detail.evaluatedAt }}</div>
              <div class="meta" v-if="detail?.calcDurationMs != null">计算耗时：{{ detail.calcDurationMs }} ms</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="score-block">
              <div class="label">相对违约风险参考</div>
              <div class="hint-num">{{ badPct }}</div>
            </div>
          </el-col>
        </el-row>

        <h4 class="sec-title">模型指标</h4>
        <el-descriptions v-if="metricEntries.length" :column="2" border>
          <el-descriptions-item v-for="[k, v] in metricEntries" :key="k" :label="k">
            {{ formatMetric(v) }}
          </el-descriptions-item>
        </el-descriptions>
        <p v-else class="muted">暂无指标明细</p>

        <h4 class="sec-title">特征快照</h4>
        <el-collapse>
          <el-collapse-item title="展开 JSON" name="1">
            <pre class="json-pre">{{ prettySnap }}</pre>
          </el-collapse-item>
        </el-collapse>

        <h4 class="sec-title">评分历史</h4>
        <el-table v-if="trendRows.length" :data="trendRows" border size="small">
          <el-table-column prop="createTime" label="评估时间" min-width="170" />
          <el-table-column prop="score" label="分数" width="90" />
          <el-table-column prop="riskLevel" label="等级" width="80" />
          <el-table-column prop="modelVersion" label="模型版本" min-width="200" show-overflow-tooltip />
        </el-table>
        <p v-else class="muted">无历史记录</p>
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useDebouncedLoader } from '@/composables/useDebouncedReload'
import { riskCreditApi } from '@/api/riskCredit'
import type { CreditCustomerBrief } from '@/api/types/riskCredit'
import type { CreditScoreOverview } from '@/api/types/credit'

const keyword = ref('')
const listLoading = ref(false)
const customerList = ref<CreditCustomerBrief[]>([])

const selected = ref<CreditCustomerBrief | null>(null)
const detailLoading = ref(false)
const detail = ref<CreditScoreOverview | null>(null)

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

const openDetail = async (row: CreditCustomerBrief) => {
  selected.value = row
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await riskCreditApi.getCustomerOverview(row.customerId)
  } catch {
    ElMessage.error('加载评分详情失败')
  } finally {
    detailLoading.value = false
  }
}

const onRowClick = (row: CreditCustomerBrief) => {
  openDetail(row)
}

const closeDetail = () => {
  selected.value = null
  detail.value = null
}

onMounted(() => reloadNow())

const scorePercent = computed(() => {
  const s = detail.value?.score
  if (s == null) return 0
  return Math.min(100, Math.max(0, ((s - 300) / 600) * 100))
})

const progressColor = computed(() => {
  const s = detail.value?.score ?? 0
  if (s >= 720) return '#67c23a'
  if (s >= 620) return '#409eff'
  if (s >= 520) return '#e6a23c'
  return '#f56c6c'
})

const riskTagType = computed(() => {
  const r = detail.value?.riskLevel
  if (r === 'A') return 'success'
  if (r === 'B') return 'primary'
  if (r === 'C') return 'warning'
  return 'danger'
})

const badPct = computed(() => {
  const h = detail.value?.badProbabilityHint
  if (h != null && h !== '') {
    const n = typeof h === 'number' ? h : Number(h)
    if (!Number.isNaN(n)) return `${(n * 100).toFixed(2)}%`
  }
  const snap = detail.value?.featureSnapshot as Record<string, unknown> | undefined
  if (snap?.badProbabilityHint != null) {
    const n = Number(snap.badProbabilityHint)
    if (!Number.isNaN(n)) return `${(n * 100).toFixed(2)}%`
  }
  return '—'
})

const metricEntries = computed(() => {
  const m = detail.value?.metrics
  if (!m) return [] as [string, unknown][]
  return Object.entries(m)
})

const formatMetric = (v: unknown) => {
  if (typeof v === 'number') return v.toFixed(4)
  return String(v)
}

const prettySnap = computed(() => {
  try {
    return JSON.stringify(detail.value?.featureSnapshot ?? {}, null, 2)
  } catch {
    return '{}'
  }
})

const trendRows = computed(() => detail.value?.recentTrend ?? [])
</script>

<style scoped>
.credit-query {
  max-width: 1100px;
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
.detail-card {
  margin-top: 16px;
}
.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.stat-row {
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
  font-size: 42px;
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
  font-size: 28px;
  font-weight: 600;
  margin: 8px 0;
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
</style>
