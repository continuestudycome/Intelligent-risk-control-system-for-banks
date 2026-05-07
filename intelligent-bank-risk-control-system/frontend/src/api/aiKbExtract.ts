import axios from 'axios'
import { useUserStore } from '@/stores/user'

/** Python /kb/extract 返回 */
export interface KbExtractResponse {
  text: string
  suggested_title: string
  filename?: string
}

/**
 * AI 服务根路径：
 * - 默认走网关 `/api/ai`（与 vite 代理一致）
 * - 直连 Python 时设置环境变量 `VITE_AI_SERVICE_URL=http://localhost:8000`
 */
export function aiServiceBaseUrl(): string {
  const direct = import.meta.env.VITE_AI_SERVICE_URL
  if (direct && String(direct).trim()) {
    return String(direct).replace(/\/$/, '')
  }
  const api = import.meta.env.VITE_API_BASE_URL || '/api'
  return `${api.replace(/\/$/, '')}/ai`
}

/** 调用 Python 抽取文档正文（不经 Java） */
export async function extractKbDocument(file: File): Promise<KbExtractResponse> {
  const fd = new FormData()
  fd.append('file', file)
  const token = useUserStore().token
  const res = await axios.post<KbExtractResponse>(`${aiServiceBaseUrl()}/kb/extract`, fd, {
    timeout: 120000,
    headers: token ? { Authorization: `Bearer ${token}` } : undefined
  })
  return res.data
}
