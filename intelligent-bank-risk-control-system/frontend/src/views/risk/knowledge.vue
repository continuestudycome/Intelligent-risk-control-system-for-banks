<template>
  <div class="kb-page">
    <el-card>
      <template #header>
        <div class="toolbar">
          <span class="title">知识库管理</span>
          <el-button type="primary" @click="openUpload">上传文档</el-button>
        </div>
      </template>
      <el-form :inline="true" class="q-form" @submit.prevent>
        <el-form-item label="关键词">
          <el-input v-model="kw" clearable placeholder="题/答/关键词" style="width: 200px" @clear="load" />
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="fcat" clearable placeholder="全部" style="width: 150px" @change="load">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型">
          <el-select v-model="fdoc" clearable placeholder="全部" style="width: 130px" @change="load">
            <el-option label="FAQ" value="FAQ" />
            <el-option label="办理指南" value="GUIDE" />
            <el-option label="政策说明" value="POLICY" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" v-loading="loading" border size="small">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="category" label="业务类型" width="110" />
        <el-table-column prop="docType" label="文档类型" width="96" />
        <el-table-column prop="sourceType" label="来源" width="88">
          <template #default="{ row }">
            <span>{{ row.sourceType === 'FILE' ? '上传' : '手工' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="question" label="标题/标准问题" min-width="200" show-overflow-tooltip />
        <el-table-column prop="hitCount" label="命中" width="72" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{
              row.status === 1 ? '启用' : '停用'
            }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="uploadDlg" title="上传知识文档" width="640px" destroy-on-close @closed="resetUpload">
      <p class="upload-tip">
        文档由 <strong>Python AI 服务</strong> 解析（/kb/extract），入库再走 Java 接口。支持 PDF、Word（.docx）、.txt / .md；标题默认取文件名，可下方修改。
      </p>
      <el-upload
        class="upload-block"
        drag
        :auto-upload="false"
        :limit="1"
        accept=".pdf,.docx,.txt,.md,.markdown"
        :on-change="onFilePick"
        :on-remove="onFileRemove"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件到此处，或 <em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">单文件，不超过 20MB</div>
        </template>
      </el-upload>
      <el-form :model="uploadForm" label-width="100px" size="small" class="upload-fields">
        <el-form-item label="业务类型" required>
          <el-select v-model="uploadForm.category" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型" required>
          <el-radio-group v-model="uploadForm.docType">
            <el-radio-button label="GUIDE">办理指南</el-radio-button>
            <el-radio-button label="POLICY">政策说明</el-radio-button>
            <el-radio-button label="FAQ">FAQ</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="uploadForm.title" clearable placeholder="留空则使用文件名（不含后缀）" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="uploadForm.keywords" clearable placeholder="逗号分隔，辅助检索" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="uploadForm.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDlg = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">入库</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="dlg" title="编辑知识" width="720px" destroy-on-close>
      <el-form :model="form" label-width="100px" size="small">
        <el-form-item label="业务类型" required>
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="文档类型" required>
          <el-radio-group v-model="form.docType">
            <el-radio-button label="FAQ">FAQ</el-radio-button>
            <el-radio-button label="GUIDE">办理指南</el-radio-button>
            <el-radio-button label="POLICY">政策说明</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标准问题" required>
          <el-input v-model="form.question" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="标准答案" required>
          <el-input v-model="form.answer" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="相似问法">
          <el-input
            v-model="similarLines"
            type="textarea"
            :rows="3"
            placeholder="每行一条，将保存为 JSON 数组"
          />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="逗号分隔，辅助检索" />
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadFile } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { extractKbDocument } from '@/api/aiKbExtract'
import { riskBotAdminApi } from '@/api/riskBotAdmin'
import type { BotKnowledgeItem } from '@/api/types/bot'

const loading = ref(false)
const list = ref<BotKnowledgeItem[]>([])
const kw = ref('')
const fcat = ref('')
const fdoc = ref('')

const categoryOptions = [
  { value: 'LOAN', label: '信贷/贷款' },
  { value: 'CREDIT_CARD', label: '信用卡' },
  { value: 'CREDIT', label: '信用评分/额度' },
  { value: 'TRANSACTION', label: '交易' },
  { value: 'ACCOUNT', label: '账户安全' },
  { value: 'FRAUD', label: '反欺诈/风控' },
  { value: 'LIMIT', label: '授信额度' },
  { value: 'GENERAL', label: '通用' }
]

const uploadDlg = ref(false)
const uploading = ref(false)
const uploadFile = ref<File | null>(null)
const uploadForm = reactive({
  category: 'GENERAL',
  docType: 'GUIDE',
  title: '',
  keywords: '',
  status: 1
})

const dlg = ref(false)
const editId = ref<number | null>(null)
const saving = ref(false)
const similarLines = ref('')
const form = reactive({
  category: 'GENERAL',
  docType: 'FAQ',
  question: '',
  answer: '',
  keywords: '',
  status: 1
})

async function load() {
  loading.value = true
  try {
    list.value = await riskBotAdminApi.listKnowledge({
      keyword: kw.value.trim() || undefined,
      category: fcat.value || undefined,
      docType: fdoc.value || undefined
    })
  } finally {
    loading.value = false
  }
}

function openUpload() {
  resetUpload()
  uploadDlg.value = true
}

function resetUpload() {
  uploadFile.value = null
  uploadForm.category = 'GENERAL'
  uploadForm.docType = 'GUIDE'
  uploadForm.title = ''
  uploadForm.keywords = ''
  uploadForm.status = 1
}

function onFilePick(file: UploadFile) {
  uploadFile.value = file.raw ?? null
}

function onFileRemove() {
  uploadFile.value = null
}

async function submitUpload() {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const extracted = await extractKbDocument(uploadFile.value)
    const question = uploadForm.title.trim() || extracted.suggested_title
    await riskBotAdminApi.createKnowledge({
      category: uploadForm.category,
      docType: uploadForm.docType,
      question,
      answer: extracted.text,
      keywords: uploadForm.keywords.trim() || undefined,
      status: uploadForm.status,
      sourceType: 'FILE',
      sourceFilename: uploadFile.value.name
    })
    ElMessage.success('文档已入库')
    uploadDlg.value = false
    await load()
  } catch (e: unknown) {
    const ax = e as { response?: { data?: { detail?: unknown } }; message?: string }
    const d = ax.response?.data?.detail
    const msg =
      typeof d === 'string'
        ? d
        : Array.isArray(d) && d[0]?.msg
          ? String(d[0].msg)
          : ax.message || '文档抽取或入库失败（请确认 AI 服务已启动）'
    ElMessage.error(msg)
  } finally {
    uploading.value = false
  }
}

function openEdit(row: BotKnowledgeItem) {
  editId.value = row.id
  form.category = row.category
  form.docType = row.docType || 'FAQ'
  form.question = row.question
  form.answer = row.answer
  form.keywords = row.keywords || ''
  form.status = row.status
  similarLines.value = ''
  if (row.similarQuestions) {
    try {
      const arr = JSON.parse(row.similarQuestions) as string[]
      if (Array.isArray(arr)) similarLines.value = arr.join('\n')
    } catch {
      similarLines.value = row.similarQuestions
    }
  }
  dlg.value = true
}

async function save() {
  if (!form.question.trim() || !form.answer.trim()) {
    ElMessage.warning('请填写问题与答案')
    return
  }
  let simJson: string | undefined
  if (similarLines.value.trim()) {
    const parts = similarLines.value
      .split(/\r?\n/)
      .map((s) => s.trim())
      .filter(Boolean)
    simJson = JSON.stringify(parts)
  }
  saving.value = true
  try {
    const body = {
      category: form.category,
      docType: form.docType,
      question: form.question.trim(),
      answer: form.answer.trim(),
      similarQuestions: simJson,
      keywords: form.keywords.trim() || undefined,
      status: form.status
    }
    if (!editId.value) {
      return
    }
    await riskBotAdminApi.updateKnowledge(editId.value, body)
    ElMessage.success('已更新')
    dlg.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function remove(row: BotKnowledgeItem) {
  await ElMessageBox.confirm(`确定删除知识 #${row.id}？`, '确认', { type: 'warning' })
  await riskBotAdminApi.deleteKnowledge(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.kb-page {
  max-width: 1200px;
  margin: 0 auto;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title {
  font-weight: 600;
}
.q-form {
  margin-bottom: 12px;
}
.upload-tip {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
}
.upload-block {
  width: 100%;
}
.upload-fields {
  margin-top: 16px;
}
</style>
