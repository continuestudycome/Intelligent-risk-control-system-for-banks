export interface BotKnowledgeItem {
  id: number
  category: string
  docType: string
  question: string
  answer: string
  similarQuestions?: string
  keywords?: string
  hitCount?: number
  status: number
  updateTime?: string
  sourceType?: string
  sourceFilename?: string
  vectorIndexedAt?: string
}

export interface BotMessageItem {
  id: number
  messageType: number
  content: string
  matchedKnowledgeId?: number
  confidence?: number
  isHelpful?: number | null
  createTime: string
}

export interface CustomerBotReply {
  assistantMessageId: number
  answer: string
  citations: { id: number; score: number }[]
  mode: string
  model?: string
}

export interface BotSessionAdmin {
  id: number
  sessionId: string
  userId: number
  channel?: string
  sessionTopic?: string
  status: number
  satisfaction?: number
  startTime?: string
  endTime?: string
  createTime?: string
  messageCount?: number
}

export interface BotStatsSummary {
  totalSessions: number
  activeSessions: number
  transferredSessions: number
  satisfiedCount: number
  neutralCount: number
  dissatisfiedCount: number
}
