<template>
  <div class="customer-home">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">个人金融</p>
        <h1>客户服务中心</h1>
        <p class="sub">办理交易、申请产品、查询进度，并享受 7×24 智能客服支持。</p>
      </div>
    </section>

    <el-row :gutter="16" class="card-row">
      <el-col v-for="card in cards" :key="card.title" :span="8">
        <el-card class="card" shadow="hover">
          <div class="card-icon" :class="card.tone">
            <el-icon :size="22">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <h3>{{ card.title }}</h3>
          <p>{{ card.desc }}</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { Money, Document, Clock, DataAnalysis, ChatDotRound } from '@element-plus/icons-vue'

const cards = [
  { title: '进行交易', desc: '支持转账、消费与还款，并展示实时风险状态。', icon: Money, tone: 'blue' },
  { title: '贷款/信用卡申请', desc: '在线提交额度与用信申请，并上传基础资料。', icon: Document, tone: 'blue' },
  { title: '申请进度', desc: '随时查询审批状态、补件要求与处理意见。', icon: Clock, tone: 'blue' },
  { title: '个人信用评分', desc: '查看当前评分、风险等级与历史变化。', icon: DataAnalysis, tone: 'blue' },
  { title: '智能客服', desc: '7×24 咨询贷款、信用卡与常见业务问题。', icon: ChatDotRound, tone: 'blue' }
]
</script>

<style scoped>
.customer-home {
  max-width: 1200px;
  margin: 0 auto;
}

.hero {
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
  color: #fff;
  background: linear-gradient(120deg, #1d4ed8 0%, #3b82f6 45%, #0ea5e9 100%);
  box-shadow: 0 10px 30px rgba(37, 99, 235, 0.25);
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.9;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.sub {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  opacity: 0.95;
  max-width: 520px;
}

.card-row {
  margin-top: 4px;
}

.card {
  margin-bottom: 16px;
  min-height: 150px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
}

.card h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #303133;
}

.card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  color: #2563eb;
  background: #eff6ff;
}

.card-icon.blue {
  color: #2563eb;
  background: #eff6ff;
}
</style>
