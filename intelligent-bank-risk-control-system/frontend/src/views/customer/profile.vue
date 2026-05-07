<template>
  <div class="profile-page">
    <el-alert
      v-if="!form.profileCompleted"
      type="warning"
      :closable="false"
      show-icon
      title="资料未完善：请先完成个人中心信息，未完善前无法使用交易、申请等业务功能。"
      class="mb16"
    />

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>客户信息录入 / 个人中心</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户类型" prop="customerType">
              <el-radio-group v-model="form.customerType">
                <el-radio :label="1">个人客户</el-radio>
                <el-radio :label="2">企业客户</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="征信授权" prop="creditAuthorized">
              <el-switch
                v-model="form.creditAuthorized"
                inline-prompt
                active-text="已授权"
                inactive-text="未授权"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="证件OCR识别">
          <el-space wrap>
            <el-select v-model="ocrDocumentType" style="width: 180px">
              <el-option label="身份证" value="ID_CARD" />
              <el-option label="营业执照" value="BUSINESS_LICENSE" />
            </el-select>
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleOcrFileChange"
              accept=".jpg,.jpeg,.png,.bmp,.webp"
            >
              <el-button :loading="ocrLoading">上传并识别</el-button>
            </el-upload>
            <span v-if="ocrHint" class="ocr-hint">{{ ocrHint }}</span>
          </el-space>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="form.customerType === 1 ? '姓名' : '企业名称'" prop="realName">
              <el-input v-model="form.realName" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.customerType === 1 ? '身份证号' : '统一社会信用代码'" prop="idCardNo">
              <el-input v-model="form.idCardNo" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="省份" prop="province">
              <el-input v-model="form.province" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="city">
              <el-input v-model="form.city" clearable />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" maxlength="512" show-word-limit />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item :label="form.customerType === 1 ? '年收入(万元)' : '年营业额(万元)'" prop="annualIncome">
              <el-input-number v-model="form.annualIncome" :min="0" :precision="2" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产总额(万元)" prop="assetAmount">
              <el-input-number v-model="form.assetAmount" :min="0" :precision="2" :step="1" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存并完成</el-button>
          <el-button @click="loadProfile">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { customerApi } from '@/api/customer'
import type { CustomerProfile, CustomerProfileSaveRequest } from '@/api/types/customer'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { UploadFile } from 'element-plus'
import type { DocumentType } from '@/api/types/ocr'

const userStore = useUserStore()
const formRef = ref<FormInstance>()
const saving = ref(false)
const ocrLoading = ref(false)
const ocrHint = ref('')
const ocrDocumentType = ref<DocumentType>('ID_CARD')

const form = reactive<CustomerProfile>({
  customerType: 1,
  realName: '',
  idCardNo: '',
  phone: '',
  email: '',
  province: '',
  city: '',
  address: '',
  annualIncome: undefined,
  assetAmount: undefined,
  creditAuthorized: false,
  profileCompleted: false
})

const rules: FormRules = {
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  realName: [{ required: true, message: '请输入姓名或企业名称', trigger: 'blur' }],
  idCardNo: [{ required: true, message: '请输入证件号', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  annualIncome: [{ required: true, message: '请输入年收入/年营业额', trigger: 'change' }],
  assetAmount: [{ required: true, message: '请输入资产总额', trigger: 'change' }],
  creditAuthorized: [
    {
      validator: (_rule, value, callback) => {
        if (value !== true) callback(new Error('请先完成征信授权'))
        else callback()
      },
      trigger: 'change'
    }
  ]
}

const applyProfile = (res: CustomerProfile) => {
  form.customerType = (res.customerType || 1) as 1 | 2
  form.realName = res.realName || ''
  form.idCardNo = res.idCardNo || ''
  form.phone = res.phone || ''
  form.email = res.email || ''
  form.province = res.province || ''
  form.city = res.city || ''
  form.address = res.address || ''
  form.annualIncome = res.annualIncome
  form.assetAmount = res.assetAmount
  form.creditAuthorized = !!res.creditAuthorized
  form.profileCompleted = !!res.profileCompleted
}

const loadProfile = async () => {
  const res = await customerApi.getMyProfile()
  applyProfile(res)
}

const handleSave = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const payload: CustomerProfileSaveRequest = {
      customerType: form.customerType,
      realName: form.realName,
      idCardNo: form.idCardNo,
      phone: form.phone,
      email: form.email,
      province: form.province,
      city: form.city,
      address: form.address,
      annualIncome: form.annualIncome,
      assetAmount: form.assetAmount,
      creditAuthorized: form.creditAuthorized
    }
    const saved = await customerApi.saveMyProfile(payload)
    applyProfile(saved)
    userStore.updateProfileCompleted(true)
    ElMessage.success('资料保存成功，已解锁交易相关功能')
  } finally {
    saving.value = false
  }
}

const handleOcrFileChange = async (uploadFile: UploadFile) => {
  if (!uploadFile.raw) return
  ocrLoading.value = true
  try {
    const res = await customerApi.extractByOcr(uploadFile.raw, ocrDocumentType.value)
    if (res.realName) form.realName = res.realName
    if (res.idCardNo) form.idCardNo = res.idCardNo
    if (res.address) form.address = res.address
    if (res.phone) form.phone = res.phone
    ocrHint.value = res.rawTextHint || `OCR识别完成（置信度：${res.confidenceHint || '中'}），请人工核对后保存`
    ElMessage.success('OCR识别成功，已自动回填可识别字段')
  } finally {
    ocrLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<style scoped>
.profile-page {
  max-width: 1080px;
  margin: 0 auto;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.mb16 {
  margin-bottom: 16px;
}

.ocr-hint {
  color: #909399;
  font-size: 12px;
}
</style>
