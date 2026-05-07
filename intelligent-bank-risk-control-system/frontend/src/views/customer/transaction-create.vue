<template>
  <el-card>
    <template #header>
      <div class="card-header">模拟交易发起</div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
      <el-form-item label="交易类型" prop="transactionType">
        <el-select v-model="form.transactionType" placeholder="请选择交易类型" style="width: 260px">
          <el-option label="转账" :value="1" />
          <el-option label="消费" :value="2" />
          <el-option label="取现" :value="3" />
          <el-option label="还款" :value="4" />
        </el-select>
      </el-form-item>

      <el-form-item label="付款账户" prop="fromAccount">
        <el-select v-model="form.fromAccount" placeholder="请选择付款账户" style="width: 360px">
          <el-option
            v-for="item in myAccounts"
            :key="item.id"
            :label="`${item.accountNo}（${item.accountTypeName}，余额 ${item.balance} ${item.currency}）`"
            :value="item.accountNo"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="收款账户" prop="toAccount">
        <el-input v-model="form.toAccount" placeholder="请输入已存在的收款账户号" maxlength="64" />
      </el-form-item>

      <el-form-item label="交易金额" prop="amount">
        <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" controls-position="right" />
      </el-form-item>

      <el-form-item label="交易省份" prop="transactionProvince">
        <el-input v-model="form.transactionProvince" placeholder="如：广东省" maxlength="64" />
      </el-form-item>

      <el-form-item label="交易城市" prop="transactionCity">
        <el-input v-model="form.transactionCity" placeholder="如：深圳市" maxlength="64" />
      </el-form-item>

      <el-form-item label="交易用途" prop="purpose">
        <el-input
          v-model="form.purpose"
          type="textarea"
          :rows="3"
          maxlength="512"
          show-word-limit
          placeholder="请输入交易用途"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交交易</el-button>
        <el-button @click="reset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-alert
      title="提交后将经过限额预检与实时反欺诈（规则引擎 + 孤立森林）；高风险拦截入账，中风险需二次验证。"
      type="info"
      :closable="false"
      show-icon
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { transactionApi } from '@/api/transaction'
import type { TransactionCreateRequest } from '@/api/types/transaction'
import { accountApi } from '@/api/account'
import type { CustomerAccount } from '@/api/types/account'

const formRef = ref<FormInstance>()
const submitting = ref(false)
const myAccounts = ref<CustomerAccount[]>([])

const initialForm: TransactionCreateRequest = {
  transactionType: 1,
  fromAccount: '',
  toAccount: '',
  amount: 100,
  transactionProvince: '',
  transactionCity: '',
  purpose: ''
}

const form = reactive<TransactionCreateRequest>({ ...initialForm })

const rules: FormRules = {
  transactionType: [{ required: true, message: '请选择交易类型', trigger: 'change' }],
  fromAccount: [{ required: true, message: '请输入付款账户', trigger: 'blur' }],
  toAccount: [{ required: true, message: '请输入收款账户', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入交易金额', trigger: 'change' }],
  transactionProvince: [{ required: true, message: '请输入交易省份', trigger: 'blur' }],
  transactionCity: [{ required: true, message: '请输入交易城市', trigger: 'blur' }],
  purpose: [{ required: true, message: '请输入交易用途', trigger: 'blur' }]
}

const submit = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const res = await transactionApi.create(form)
    if (res.handleResult === 1) {
      ElMessage.success(`交易成功，流水号：${res.transactionNo}`)
      reset()
    } else if (res.handleResult === 2) {
      ElMessage.warning(res.riskMessage || '中风险：需二次验证，资金未划出')
    } else {
      ElMessage.error(res.riskMessage || '高风险：交易已拦截，资金未划出')
    }
  } finally {
    submitting.value = false
  }
}

const reset = () => {
  Object.assign(form, initialForm)
  formRef.value?.clearValidate()
}

const loadMyAccounts = async () => {
  myAccounts.value = await accountApi.myAccounts()
  const first = myAccounts.value[0]
  if (!form.fromAccount && first) {
    form.fromAccount = first.accountNo
  }
}

onMounted(() => {
  loadMyAccounts()
})
</script>

<style scoped>
.card-header {
  font-size: 16px;
  font-weight: 600;
}
</style>
