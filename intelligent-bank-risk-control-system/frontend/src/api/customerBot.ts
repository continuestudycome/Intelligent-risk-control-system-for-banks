import request from './request'
import type { BotMessageItem, CustomerBotReply } from './types/bot'

export const customerBotApi = {
  createSession() {
    return request.post<any, string>('/customer/bot/sessions')
  },
  listMessages(sessionId: string) {
    return request.get<any, BotMessageItem[]>(`/customer/bot/sessions/${sessionId}/messages`)
  },
  chat(sessionId: string, body: { content: string; scopeCategory?: string }) {
    // RAG + 本地 Ollama 推理可能超过默认 30s
    return request.post<any, CustomerBotReply>(
      `/customer/bot/sessions/${sessionId}/chat`,
      body,
      { timeout: 180000 }
    )
  },
  feedback(messageId: number, helpful: boolean) {
    return request.post(`/customer/bot/messages/${messageId}/feedback`, { helpful })
  },
  satisfaction(sessionId: string, satisfaction: number) {
    return request.put(`/customer/bot/sessions/${sessionId}/satisfaction`, { satisfaction })
  },
  transfer(sessionId: string, reason?: string) {
    return request.post(`/customer/bot/sessions/${sessionId}/transfer`, { reason })
  }
}
