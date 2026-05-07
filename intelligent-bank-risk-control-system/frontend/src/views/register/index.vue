<template>
  <div class="register-container">
    <div class="register-box">
      <div class="register-header">
        <h1 class="title">用户注册</h1>
        <p class="subtitle">创建您的风控系统账户</p>
      </div>

      <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          label-position="top"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input
                  v-model="registerForm.username"
                  placeholder="4-20位字母数字下划线"
                  :prefix-icon="User"
                  size="large"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input
                  v-model="registerForm.realName"
                  placeholder="请输入真实姓名"
                  :prefix-icon="UserFilled"
                  size="large"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input
                  v-model="registerForm.password"
                  type="password"
                  placeholder="6-20位密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                  v-model="registerForm.confirmPassword"
                  type="password"
                  placeholder="请再次输入密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="手机号" prop="phone">
          <el-input
              v-model="registerForm.phone"
              placeholder="请输入手机号"
              :prefix-icon="Phone"
              size="large"
          />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input
              v-model="registerForm.email"
              placeholder="请输入邮箱（选填）"
              :prefix-icon="Message"
              size="large"
          />
        </el-form-item>

        <el-form-item>
          <el-alert
            title="自助注册仅支持客户账号，风控人员账号由管理员后台开通。"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form-item>

        <el-form-item>
          <el-button
              type="primary"
              size="large"
              class="register-btn"
              :loading="loading"
              @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>

        <div class="login-link">
          已有账号？<el-link type="primary" @click="goLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock, Phone, Message } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import type { FormInstance, FormItemRule } from 'element-plus'
import type { RegisterRequest } from '@/api/types/auth'

const router = useRouter()
const userStore = useUserStore()
const registerFormRef = ref<FormInstance>()
const loading = ref(false)

const registerForm = reactive<RegisterRequest>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  phone: '',
  email: ''
})

// 自定义验证：确认密码
const validateConfirmPassword: FormItemRule['validator'] = (_rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules: Record<string, FormItemRule[]> = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '用户名只能包含4-20位字母、数字和下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const forceNavigate = (path: string) => {
  window.location.assign(path)
}

const goLogin = () => {
  forceNavigate('/login')
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register(registerForm)
    ElMessage.success('注册成功，请登录')
    forceNavigate('/login')
  } catch (error) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.register-box {
  width: 560px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 30px;
}

.title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.subtitle {
  font-size: 14px;
  color: #909399;
}

.register-form {
  margin-top: 20px;
}

.register-btn {
  width: 100%;
  font-size: 16px;
  margin-top: 10px;
}

.login-link {
  text-align: center;
  margin-top: 16px;
  color: #606266;
  font-size: 14px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  padding-bottom: 4px;
}
</style>
