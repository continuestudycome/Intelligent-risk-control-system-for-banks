<template>
  <div class="svc-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">智能客服</span>
          <div class="tools">
            <el-select v-model="scopeCategory" placeholder="全部分类检索" clearable style="width: 160px">
              <el-option label="全部分类" value="" />
              <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-button plain :loading="sessionLoading" @click="restartSession">新会话</el-button>
          </div>
        </div>
      </template>

      <el-alert type="info" :closable="false" show-icon class="mb-3">
        回答由<strong>知识库 + Ollama</strong>生成；可先选择左侧业务分类缩小检索范围。追问时请保持在同一会话内，便于理解「额度」「拦截」等指代。
      </el-alert>

      <div class="chat-wrap" v-loading="bootLoading">
        <el-scrollbar ref="scrollRef" height="420px" class="chat-scroll">
          <div v-if="!messages.length" class="empty-tip">您好，请问有什么可以帮您？</div>
          <div
            v-for="m in messages"
            :key="m.id"
            :class="['bubble', m.messageType === 1 ? 'user' : m.messageType === 2 ? 'bot' : 'sys']"
          >
            <div class="meta">{{ roleName(m.messageType) }} · {{ m.createTime }}</div>
            <div class="text">{{ m.content }}</div>
            <div v-if="m.messageType === 2 && m.isHelpful == null" class="fb">
              <span class="fb-label">有帮助吗？</span>
              <el-button link type="success" size="small" @click="feedback(m.id, true)">有用</el-button>
              <el-button link type="danger" size="small" @click="feedback(m.id, false)">无用</el-button>
            </div>
          </div>
        </el-scrollbar>

        <div class="composer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="3"
            placeholder="请输入您的问题…"
            @keydown.enter.exact.prevent="send"
          />
          <div class="composer-actions">
            <el-button @click="doTransfer" plain>转人工</el-button>
            <el-button type="primary" :loading="sending" :disabled="!sessionId" @click="send">发送</el-button>
            <el-button @click="satDlg = true">结束并评价</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="satDlg" title="会话满意度" width="400px" @closed="satDlg = false">
      <p class="mb-2">请为本次智能客服体验打分：</p>
      <el-radio-group v-model="sat">
        <el-radio :label="3">满意</el-radio>
        <el-radio :label="2">一般</el-radio>
        <el-radio :label="1">不满意</el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="satDlg = false">取消</el-button>
        <el-button type="primary" @click="submitSat">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { customerBotApi } from '@/api/customerBot'
import type { BotMessageItem } from '@/api/types/bot'

const bootLoading = ref(true)
const sessionLoading = ref(false)
const sending = ref(false)
const sessionId = ref('')
const messages = ref<BotMessageItem[]>([])
const input = ref('')
const scopeCategory = ref('')
const scrollRef = ref()
const satDlg = ref(false)
const sat = ref(3)

const categoryOptions = [
  { value: 'LOAN', label: '信贷/贷款' },
  { value: 'CREDIT_CARD', label: '信用卡' },
  { value: 'CREDIT', label: '信用额度' },
  { value: 'TRANSACTION', label: '交易' },
  { value: 'ACCOUNT', label: '账户安全' },
  { value: 'FRAUD', label: '风控/反欺诈' },
  { value: 'LIMIT', label: '授信' },
  { value: 'GENERAL', label: '通用' }
]

function roleName(t: number) {
  if (t === 1) return '您'
  if (t === 2) return '智能客服'
  return '系统'
}

async function boot() {
  bootLoading.value = true
  try {
    sessionId.value = await customerBotApi.createSession()
    messages.value = []
  } finally {
    bootLoading.value = false
  }
}

async function loadMessages() {
  if (!sessionId.value) return
  messages.value = await customerBotApi.listMessages(sessionId.value)
  await nextTick()
  scrollRef.value?.setScrollTop?.(99999)
}

async function restartSession() {
  sessionLoading.value = true
  try {
    sessionId.value = await customerBotApi.createSession()
    messages.value = []
    ElMessage.success('已开始新会话')
  } finally {
    sessionLoading.value = false
  }
}

async function send() {
  const q = input.value.trim()
  if (!q || !sessionId.value) return
  sending.value = true
  try {
    await customerBotApi.chat(sessionId.value, {
      content: q,
      scopeCategory: scopeCategory.value || undefined
    })
    input.value = ''
    await loadMessages()
  } catch {
    /* 错误提示由 request 拦截器统一处理 */
  } finally {
    sending.value = false
  }
}

async function feedback(messageId: number, helpful: boolean) {
  await customerBotApi.feedback(messageId, helpful)
  ElMessage.success('感谢您的反馈')
  await loadMessages()
}

async function doTransfer() {
  if (!sessionId.value) return
  await customerBotApi.transfer(sessionId.value, '用户点击转人工')
  ElMessage.success('已登记转人工')
  await loadMessages()
}

async function submitSat() {
  if (!sessionId.value) return
  await customerBotApi.satisfaction(sessionId.value, sat.value)
  ElMessage.success('感谢评价')
  satDlg.value = false
}

onMounted(async () => {
  await boot()
  await loadMessages()
})
</script>

<style scoped>
.svc-page {
  max-width: 880px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.tools {
  display: flex;
  gap: 8px;
  align-items: center;
}
.title {
  font-weight: 600;
}
.mb-3 {
  margin-bottom: 12px;
}
.chat-wrap {
  margin-top: 8px;
}
.empty-tip {
  color: var(--el-text-color-secondary);
  text-align: center;
  padding: 40px;
}
.bubble {
  margin-bottom: 14px;
  max-width: 92%;
}
.bubble.user {
  margin-left: auto;
  text-align: right;
}
.bubble.bot {
  margin-right: auto;
}
.bubble.sys {
  margin: 0 auto;
  text-align: center;
  max-width: 100%;
}
.meta {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 4px;
}
.text {
  white-space: pre-wrap;
  line-height: 1.6;
  padding: 10px 12px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  text-align: left;
  display: inline-block;
}
.user .text {
  background: var(--el-color-primary-light-9);
}
.sys .text {
  background: var(--el-color-info-light-9);
  font-size: 13px;
}
.fb {
  margin-top: 6px;
  font-size: 12px;
}
.fb-label {
  color: var(--el-text-color-secondary);
  margin-right: 8px;
}
.composer {
  margin-top: 12px;
}
.composer-actions {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}
.mb-2 {
  margin-bottom: 8px;
}
</style>
