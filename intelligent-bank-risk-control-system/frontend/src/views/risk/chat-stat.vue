<template>
  <div class="stat-page">
    <el-card class="mb-card">
      <template #header><span class="title">客服会话与反馈统计</span></template>
      <el-row :gutter="12" v-loading="sumLoading">
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="总会话" :value="summary.totalSessions" />
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="进行中" :value="summary.activeSessions" />
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="转人工" :value="summary.transferredSessions" />
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="满意(3)" :value="summary.satisfiedCount" />
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="一般(2)" :value="summary.neutralCount" />
        </el-col>
        <el-col :xs="12" :sm="8" :md="4">
          <el-statistic title="不满意(1)" :value="summary.dissatisfiedCount" />
        </el-col>
      </el-row>
    </el-card>

    <el-card>
      <template #header>
        <div class="toolbar">
          <span>会话列表</span>
          <el-button plain @click="reloadAll">刷新</el-button>
        </div>
      </template>
      <el-form :inline="true" class="q-form">
        <el-form-item label="主题分类">
          <el-input v-model="topic" clearable placeholder="session_topic" style="width: 140px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="st" clearable placeholder="全部" style="width: 120px">
            <el-option label="进行中" :value="1" />
            <el-option label="已结束" :value="0" />
            <el-option label="转人工" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="满意度">
          <el-select v-model="sat" clearable placeholder="全部" style="width: 120px">
            <el-option label="满意" :value="3" />
            <el-option label="一般" :value="2" />
            <el-option label="不满意" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadSessions">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="sessions" v-loading="listLoading" border size="small">
        <el-table-column prop="sessionId" label="会话ID" min-width="200" show-overflow-tooltip />
        <el-table-column prop="userId" label="用户ID" width="88" />
        <el-table-column prop="sessionTopic" label="主题" width="120" />
        <el-table-column label="状态" width="96">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="满意度" width="88">
          <template #default="{ row }">{{ satLabel(row.satisfaction) }}</template>
        </el-table-column>
        <el-table-column prop="messageCount" label="消息数" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { riskBotAdminApi } from '@/api/riskBotAdmin'
import type { BotSessionAdmin, BotStatsSummary } from '@/api/types/bot'

const sumLoading = ref(false)
const summary = reactive<BotStatsSummary>({
  totalSessions: 0,
  activeSessions: 0,
  transferredSessions: 0,
  satisfiedCount: 0,
  neutralCount: 0,
  dissatisfiedCount: 0
})

const listLoading = ref(false)
const sessions = ref<BotSessionAdmin[]>([])
const topic = ref('')
const st = ref<number | undefined>(undefined)
const sat = ref<number | undefined>(undefined)

function statusLabel(s: number) {
  if (s === 1) return '进行中'
  if (s === 2) return '转人工'
  if (s === 0) return '已结束'
  return String(s)
}

function satLabel(x?: number) {
  if (x === 3) return '满意'
  if (x === 2) return '一般'
  if (x === 1) return '不满意'
  return '—'
}

async function loadSummary() {
  sumLoading.value = true
  try {
    const d = await riskBotAdminApi.statsSummary()
    Object.assign(summary, d)
  } finally {
    sumLoading.value = false
  }
}

async function loadSessions() {
  listLoading.value = true
  try {
    sessions.value = await riskBotAdminApi.listSessions({
      topic: topic.value.trim() || undefined,
      status: st.value,
      satisfaction: sat.value,
      limit: 120
    })
  } finally {
    listLoading.value = false
  }
}

async function reloadAll() {
  await loadSummary()
  await loadSessions()
}

onMounted(reloadAll)
</script>

<style scoped>
.stat-page {
  max-width: 1200px;
  margin: 0 auto;
}
.mb-card {
  margin-bottom: 16px;
}
.title {
  font-weight: 600;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.q-form {
  margin-bottom: 12px;
}
</style>
