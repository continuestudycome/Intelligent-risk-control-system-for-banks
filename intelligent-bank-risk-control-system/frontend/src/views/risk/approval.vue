<template>
  <el-card>
    <template #header>
      <div class="toolbar">
        <span class="title">审批贷款 / 信用卡申请</span>
        <el-button type="primary" plain @click="reloadNow">刷新</el-button>
      </div>
    </template>

    <el-form :inline="true" :model="query" class="query-form" @submit.prevent="() => {}">
      <el-form-item label="状态">
        <el-select v-model="query.currentStatus" clearable placeholder="全部" style="width: 140px">
          <el-option label="待审批" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已拒绝" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.applyType" clearable placeholder="全部" style="width: 140px">
          <el-option label="信用贷款" :value="1" />
          <el-option label="抵押贷款" :value="2" />
          <el-option label="信用卡" :value="3" />
        </el-select>
      </el-form-item>
    </el-form>

    <el-table :data="list" v-loading="loading" border>
      <el-table-column prop="applicationNo" label="申请编号" min-width="190" />
      <el-table-column prop="applyTypeName" label="类型" width="100" />
      <el-table-column prop="applyAmount" label="申请金额" width="110" />
      <el-table-column label="期限" width="88">
        <template #default="{ row }">
          {{ row.applyType === 3 && row.applyTerm === 0 ? '—' : row.applyTerm + '月' }}
        </template>
      </el-table-column>
      <el-table-column prop="applyPurpose" label="用途" min-width="140" show-overflow-tooltip />
      <el-table-column prop="currentStatusName" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.currentStatus)">{{ row.currentStatusName }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" min-width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <el-button v-if="row.currentStatus === 'PENDING'" link type="success" @click="openReview(row)">审批</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="detailVisible" title="申请详情" width="720px" destroy-on-close>
    <el-skeleton v-if="detailLoading" :rows="8" animated />
    <template v-else-if="detailCurrent">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请编号">{{ detailCurrent.applicationNo }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detailCurrent.applyTypeName }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">{{ detailCurrent.applyAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="期限">
          {{ detailCurrent.applyType === 3 && detailCurrent.applyTerm === 0 ? '—' : detailCurrent.applyTerm + ' 月' }}
        </el-descriptions-item>
        <el-descriptions-item label="用途" :span="2">{{ detailCurrent.applyPurpose }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detailCurrent.currentStatusName }}</el-descriptions-item>
        <el-descriptions-item label="终审">{{ detailCurrent.finalResultName }}</el-descriptions-item>
      </el-descriptions>
      <div class="sub-title">审批记录</div>
      <el-timeline v-if="detailCurrent.approvalRecords?.length">
        <el-timeline-item v-for="rec in detailCurrent.approvalRecords" :key="rec.id" :timestamp="rec.createTime">
          {{ rec.reviewerName }} —
          <el-tag size="small" :type="rec.action === 'PASS' ? 'success' : 'danger'">{{ rec.action }}</el-tag>
          <span v-if="rec.comment">：{{ rec.comment }}</span>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审批记录" />
    </template>
  </el-dialog>

  <el-dialog v-model="reviewVisible" title="审批" width="520px" destroy-on-close @closed="resetReview">
    <el-form ref="reviewFormRef" :model="reviewForm" :rules="reviewRules" label-width="100px">
      <el-form-item label="结论" prop="action">
        <el-radio-group v-model="reviewForm.action">
          <el-radio label="PASS">通过</el-radio>
          <el-radio label="REJECT">拒绝</el-radio>
        </el-radio-group>
      </el-form-item>
      <template v-if="reviewForm.action === 'PASS'">
        <el-form-item label="核定金额" prop="finalAmount">
          <el-input-number v-model="reviewForm.finalAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="reviewTarget && reviewTarget.applyType !== 3" label="核定期限" prop="finalTerm">
          <el-input-number v-model="reviewForm.finalTerm" :min="1" :max="360" :step="1" style="width: 100%" />
        </el-form-item>
      </template>
      <el-form-item label="意见" prop="comment">
        <el-input v-model="reviewForm.comment" type="textarea" :rows="3" maxlength="2000" placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewVisible = false">取消</el-button>
      <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useDebouncedLoader } from '@/composables/useDebouncedReload'
import { riskLoanApi } from '@/api/loanApplication'
import type { LoanApplication } from '@/api/types/loanApplication'

const loading = ref(false)
const list = ref<LoanApplication[]>([])
const query = reactive<{ currentStatus?: string; applyType?: number }>({})

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailCurrent = ref<LoanApplication | null>(null)

const reviewVisible = ref(false)
const reviewSubmitting = ref(false)
const reviewTargetId = ref<number | null>(null)
const reviewTarget = ref<LoanApplication | null>(null)
const reviewFormRef = ref<FormInstance>()
const reviewForm = reactive({
  action: 'PASS' as 'PASS' | 'REJECT',
  comment: '',
  finalAmount: undefined as number | undefined,
  finalTerm: undefined as number | undefined
})

const reviewRules: FormRules = {
  action: [{ required: true, message: '请选择结论', trigger: 'change' }]
}

const statusTag = (s: string) => {
  if (s === 'APPROVED') return 'success'
  if (s === 'REJECTED') return 'danger'
  return 'warning'
}

const loadList = async () => {
  loading.value = true
  try {
    list.value = await riskLoanApi.list({
      currentStatus: query.currentStatus || undefined,
      applyType: query.applyType
    })
  } finally {
    loading.value = false
  }
}

const { scheduleReload, reloadNow } = useDebouncedLoader(loadList)

watch(
  () => query,
  () => scheduleReload(),
  { deep: true }
)

const openDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  detailCurrent.value = null
  try {
    detailCurrent.value = await riskLoanApi.detail(id)
  } catch {
    detailVisible.value = false
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

const openReview = (row: LoanApplication) => {
  reviewTargetId.value = row.id
  reviewTarget.value = row
  reviewForm.action = 'PASS'
  reviewForm.comment = ''
  reviewForm.finalAmount = row.applyAmount
  reviewForm.finalTerm = row.applyType === 3 ? 0 : row.applyTerm
  reviewVisible.value = true
}

watch(
  () => reviewForm.action,
  (a) => {
    if (a === 'PASS' && reviewTarget.value) {
      reviewForm.finalAmount = reviewTarget.value.applyAmount
      reviewForm.finalTerm = reviewTarget.value.applyType === 3 ? 0 : reviewTarget.value.applyTerm
    }
  }
)

const resetReview = () => {
  reviewTargetId.value = null
  reviewTarget.value = null
  reviewFormRef.value?.resetFields()
}

const submitReview = async () => {
  if (!reviewFormRef.value || reviewTargetId.value == null) return
  const ok = await reviewFormRef.value.validate().catch(() => false)
  if (!ok) return

  reviewSubmitting.value = true
  try {
    const payload: {
      action: 'PASS' | 'REJECT'
      comment?: string
      finalAmount?: number
      finalTerm?: number
    } = {
      action: reviewForm.action,
      comment: reviewForm.comment || undefined
    }
    if (reviewForm.action === 'PASS') {
      payload.finalAmount = reviewForm.finalAmount
      if (reviewTarget.value && reviewTarget.value.applyType !== 3) {
        payload.finalTerm = reviewForm.finalTerm
      }
    }
    await riskLoanApi.review(reviewTargetId.value, payload)
    ElMessage.success('审批已提交')
    reviewVisible.value = false
    await reloadNow()
  } finally {
    reviewSubmitting.value = false
  }
}

onMounted(() => reloadNow())
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
.sub-title {
  margin: 12px 0 8px;
  font-weight: 600;
}
</style>
