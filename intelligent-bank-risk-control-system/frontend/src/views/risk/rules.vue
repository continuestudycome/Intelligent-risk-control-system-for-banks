<template>
  <div class="rules-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">风控规则调整</span>
          <div class="actions">
            <el-button plain :loading="loading" @click="loadList">刷新</el-button>
            <el-button type="primary" plain :loading="validateLoading" @click="validateAllAi">智能校验（Python）</el-button>
            <el-button type="success" plain :loading="simLoading" @click="openSimulate">默认样本试算</el-button>
          </div>
        </div>
      </template>

      <p class="hint">
        反欺诈引擎实时读取表 <code>frd_rule</code> 中的参数（单体超大额、异地大额、小额试探、孤立森林升高危阈值）。修改后下一笔交易即生效。橙色按钮将调用
        <strong>ai_services</strong> 做参数合理性校验；试算结果与 Java <code>FraudRiskAssessmentServiceImpl</code> 逻辑对齐。
      </p>

      <el-table :data="list" v-loading="loading" border size="small">
        <el-table-column prop="ruleCode" label="规则编码" min-width="160" show-overflow-tooltip />
        <el-table-column prop="ruleName" label="名称" min-width="120" />
        <el-table-column label="类型" width="88">
          <template #default="{ row }">{{ ruleTypeLabel(row.ruleType) }}</template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="触达等级" width="96" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column label="状态" width="88">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{
              row.status === 1 ? '启用' : '停用'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="72" />
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="editVisible" :title="`编辑规则 — ${current?.ruleCode}`" width="640px" destroy-on-close>
      <el-form v-if="form" label-width="120px" size="small">
        <el-form-item label="参数 JSON">
          <el-input v-model="form.ruleCondition" type="textarea" :rows="8" placeholder='例如 {"absoluteHighAmount":85000}' />
        </el-form-item>
        <el-form-item label="触达风险等级">
          <el-select v-model="form.riskLevel" style="width: 160px">
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button :loading="aiOneLoading" @click="validateOneAi">校验本条（AI）</el-button>
        <el-button type="primary" :loading="saveLoading" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="simVisible" title="默认样本风险试算（Python）" width="900px" destroy-on-close>
      <el-table :data="simResults" border size="small" v-loading="simLoading">
        <el-table-column prop="index" label="#" width="50" />
        <el-table-column label="样本输入" min-width="220">
          <template #default="{ row }">
            <span class="mono">{{ fmtSample(row.input) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="ruleLevel" label="规则档" width="96" />
        <el-table-column label="命中规则" min-width="160">
          <template #default="{ row }">{{ (row.hitRules || []).join(', ') || '—' }}</template>
        </el-table-column>
        <el-table-column prop="finalLevel" label="合并档" width="96" />
        <el-table-column label="模型参与" width="96">
          <template #default="{ row }">{{ row.mlUsed ? '是' : '否' }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fraudRuleApi } from '@/api/fraudRule'
import type { FraudRuleItem } from '@/api/types/fraudRule'

const loading = ref(false)
const list = ref<FraudRuleItem[]>([])

const validateLoading = ref(false)
const simLoading = ref(false)
const simVisible = ref(false)
const simResults = ref<Record<string, unknown>[]>([])

const editVisible = ref(false)
const current = ref<FraudRuleItem | null>(null)
const form = reactive({
  ruleCondition: '',
  riskLevel: 'HIGH',
  priority: 0,
  status: 1,
  remark: ''
})
const saveLoading = ref(false)
const aiOneLoading = ref(false)

function ruleTypeLabel(t: number) {
  const m: Record<number, string> = {
    1: '金额',
    2: '频次',
    3: '地域',
    4: '时间',
    5: '组合'
  }
  return m[t] ?? String(t)
}

async function loadList() {
  loading.value = true
  try {
    list.value = await fraudRuleApi.list()
  } finally {
    loading.value = false
  }
}

async function validateAllAi() {
  if (!list.value.length) {
    ElMessage.warning('暂无规则数据')
    return
  }
  validateLoading.value = true
  try {
    const rules = list.value.map((r) => ({
      ruleCode: r.ruleCode,
      ruleCondition: r.ruleCondition
    }))
    const res = await fraudRuleApi.validateAi(rules)
    const offlineHint = res.hints?.some((h) => h.includes('无法连接') || h.includes('跳过'))
    if (!res.ok) {
      ElMessage.error(res.errors?.join('；') || '校验未通过')
    } else if (offlineHint) {
      ElMessage.warning(res.hints?.join('；') || '智能服务不可用')
    } else {
      ElMessage.success('智能校验通过')
    }
    if (res.ok && res.hints?.length && !offlineHint) {
      ElMessage.info(res.hints.join('；'))
    }
  } finally {
    validateLoading.value = false
  }
}

async function openSimulate() {
  simVisible.value = true
  simLoading.value = true
  simResults.value = []
  try {
    simResults.value = await fraudRuleApi.simulateDefault()
  } catch {
    simResults.value = []
  } finally {
    simLoading.value = false
  }
}

function openEdit(row: FraudRuleItem) {
  current.value = row
  form.ruleCondition = row.ruleCondition
  form.riskLevel = row.riskLevel
  form.priority = row.priority
  form.status = row.status
  form.remark = row.remark || ''
  editVisible.value = true
}

async function validateOneAi() {
  if (!current.value) return
  aiOneLoading.value = true
  try {
    const res = await fraudRuleApi.validateAi([
      { ruleCode: current.value.ruleCode, ruleCondition: form.ruleCondition }
    ])
    if (!res.ok) {
      ElMessage.error(res.errors?.join('；') || '校验未通过')
    } else {
      ElMessage.success('本条规则校验通过')
    }
    if (res.hints?.length) {
      ElMessage.info(res.hints.join('；'))
    }
  } finally {
    aiOneLoading.value = false
  }
}

async function saveEdit() {
  if (!current.value) return
  saveLoading.value = true
  try {
    await fraudRuleApi.update(current.value.id, {
      ruleCondition: form.ruleCondition,
      riskLevel: form.riskLevel,
      priority: form.priority,
      status: form.status,
      remark: form.remark
    })
    ElMessage.success('已保存')
    editVisible.value = false
    await loadList()
  } finally {
    saveLoading.value = false
  }
}

function fmtSample(input: unknown) {
  if (!input || typeof input !== 'object') return '—'
  const o = input as Record<string, unknown>
  return `amt=${o.amount} 同城=${o.sameProvince} 试探=${o.probeCount} ml=${o.mlScore}`
}

onMounted(loadList)
</script>

<style scoped>
.rules-page {
  max-width: 1200px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.title {
  font-weight: 600;
}
.hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.7;
  margin-bottom: 12px;
}
.mono {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
</style>
