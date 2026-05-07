<template>
  <el-card class="page-card">
    <template #header>
      <span class="card-title">贷款 / 信用卡申请</span>
    </template>

    <el-alert
      title="提交前请确保已在「个人中心」完善资料；黑名单客户无法提交。"
      type="info"
      :closable="false"
      show-icon
      class="mb-4"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px" style="max-width: 560px" @submit.prevent>
      <el-form-item label="申请类型" prop="applyType">
        <el-select v-model="form.applyType" placeholder="请选择" style="width: 100%">
          <el-option label="信用贷款" :value="1" />
          <el-option label="抵押贷款" :value="2" />
          <el-option label="信用卡" :value="3" />
        </el-select>
      </el-form-item>

      <el-form-item label="申请金额(元)" prop="applyAmount">
        <el-input-number
          v-model="form.applyAmount"
          :min="1"
          :precision="2"
          :step="10000"
          controls-position="right"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item v-if="form.applyType !== 3" label="申请期限(月)" prop="applyTerm">
        <el-input-number v-model="form.applyTerm" :min="1" :max="360" :step="1" controls-position="right" style="width: 100%" />
      </el-form-item>

      <el-form-item label="资金用途" prop="applyPurpose">
        <el-input v-model="form.applyPurpose" type="textarea" :rows="3" maxlength="256" show-word-limit placeholder="请简要说明用途" />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交申请</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { customerLoanApi } from '@/api/loanApplication'
import type { LoanApplicationCreateRequest } from '@/api/types/loanApplication'

const formRef = ref<FormInstance>()
const submitting = ref(false)

const initial: LoanApplicationCreateRequest = {
  applyType: 1,
  applyAmount: 100000,
  applyTerm: 12,
  applyPurpose: ''
}

const form = reactive<LoanApplicationCreateRequest>({ ...initial })

const rules = computed<FormRules>(() => ({
  applyType: [{ required: true, message: '请选择申请类型', trigger: 'change' }],
  applyAmount: [{ required: true, message: '请输入申请金额', trigger: 'change' }],
  applyTerm:
    form.applyType === 3
      ? []
      : [{ required: true, message: '请输入期限', trigger: 'change' }],
  applyPurpose: [{ required: true, message: '请填写资金用途', trigger: 'blur' }]
}))

watch(
  () => form.applyType,
  (t) => {
    if (t === 3) {
      form.applyTerm = undefined
    } else if (form.applyTerm == null) {
      form.applyTerm = 12
    }
  }
)

const submit = async () => {
  if (!formRef.value) return
  const ok = await formRef.value.validate().catch(() => false)
  if (!ok) return

  submitting.value = true
  try {
    const payload: LoanApplicationCreateRequest = {
      applyType: form.applyType,
      applyAmount: form.applyAmount,
      applyPurpose: form.applyPurpose
    }
    if (form.applyType !== 3) {
      payload.applyTerm = form.applyTerm
    }
    const res = await customerLoanApi.create(payload)
    ElMessage.success(`提交成功，申请编号：${res.applicationNo}`)
    reset()
  } finally {
    submitting.value = false
  }
}

const reset = () => {
  Object.assign(form, { ...initial })
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.page-card {
  max-width: 720px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
}
.mb-4 {
  margin-bottom: 16px;
}
</style>
