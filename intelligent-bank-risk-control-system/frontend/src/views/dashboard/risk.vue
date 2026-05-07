<template>
  <div class="risk-home">
    <section class="hero">
      <div class="hero-text">
        <p class="eyebrow">风险运营</p>
        <h1>风控人员运营台</h1>
        <p class="sub">集中处理审批、监控预警与欺诈案件，统一维护规则、知识库与数据查询能力。</p>
      </div>
    </section>

    <el-row :gutter="16" class="card-row">
      <el-col v-for="card in cards" :key="card.title" :span="8">
        <el-card class="card" shadow="hover">
          <div class="card-icon">
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
import {
  Check,
  WarningFilled,
  Bell,
  User,
  Setting,
  Collection,
  TrendCharts,
  EditPen,
  Money,
  DataAnalysis,
  Histogram
} from '@element-plus/icons-vue'

const cards = [
  { title: '审批工作台', desc: '贷款/信用卡申请多级审批与补件处理。', icon: Check },
  { title: '风险 / 欺诈监控', desc: '风险预警盯盘与欺诈告警闭环处置。', icon: WarningFilled },
  { title: '欺诈专项', desc: '欺诈案件核实、降级与工单流转。', icon: Bell },
  { title: '客户风险画像', desc: '多头、行为与评分标签的一站式视图。', icon: User },
  { title: '规则引擎', desc: '策略参数化调整与仿真验证。', icon: Setting },
  { title: '客服知识库', desc: '知识条目上传、编辑与分类维护。', icon: Collection },
  { title: '会话与满意度', desc: '智能客服会话质检与满意度统计。', icon: TrendCharts },
  { title: '客户信息录入', desc: '客户主数据建档与变更留痕。', icon: EditPen },
  { title: '交易与评分', desc: '全量交易查询与评分穿透分析。', icon: Money },
  { title: '评分查询', desc: '单笔/批量信用评分复核。', icon: DataAnalysis },
  { title: '审批进度', desc: '机构维度的审批 SLA 与积压监控。', icon: Histogram }
]
</script>

<style scoped>
.risk-home {
  max-width: 1200px;
  margin: 0 auto;
}

.hero {
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 20px;
  color: #f8fafc;
  background: linear-gradient(135deg, #0f172a 0%, #334155 55%, #78350f 120%);
  box-shadow: 0 12px 36px rgba(15, 23, 42, 0.35);
  border: 1px solid rgba(230, 162, 60, 0.35);
}

.eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #fbbf24;
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
}

.sub {
  margin: 0;
  font-size: 14px;
  line-height: 1.65;
  color: #cbd5e1;
  max-width: 640px;
}

.card-row {
  margin-top: 4px;
}

.card {
  margin-bottom: 16px;
  min-height: 142px;
  border-radius: 10px;
  border: 1px solid #dcdfe6;
  background: #fafbfc;
}

.card h3 {
  margin: 0 0 8px;
  font-size: 16px;
  color: #1f2937;
}

.card p {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: #4b5563;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  color: #d97706;
  background: rgba(230, 162, 60, 0.15);
}
</style>
