import request from './request'
import type {
  BotKnowledgeItem,
  BotSessionAdmin,
  BotStatsSummary
} from './types/bot'

export const riskBotAdminApi = {
  listKnowledge(params?: { keyword?: string; category?: string; docType?: string }) {
    return request.get<any, BotKnowledgeItem[]>('/risk/bot/knowledge', { params })
  },
  createKnowledge(body: {
    category: string
    docType: string
    question: string
    answer: string
    similarQuestions?: string
    keywords?: string
    status?: number
    sourceType?: string
    sourceFilename?: string
  }) {
    return request.post<any, BotKnowledgeItem>('/risk/bot/knowledge', body)
  },
  updateKnowledge(
    id: number,
    body: {
      category: string
      docType: string
      question: string
      answer: string
      similarQuestions?: string
      keywords?: string
      status?: number
    }
  ) {
    return request.put<any, BotKnowledgeItem>(`/risk/bot/knowledge/${id}`, body)
  },
  deleteKnowledge(id: number) {
    return request.delete(`/risk/bot/knowledge/${id}`)
  },
  listSessions(params?: {
    topic?: string
    status?: number
    satisfaction?: number
    limit?: number
  }) {
    return request.get<any, BotSessionAdmin[]>('/risk/bot/sessions', { params })
  },
  statsSummary() {
    return request.get<any, BotStatsSummary>('/risk/bot/stats/summary')
  }
}
