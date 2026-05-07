export interface CreditHistoryPoint {
  id: number
  score: number
  riskLevel: string
  modelVersion: string
  createTime: string
}

export interface CustomerLimitSummary {
  hasLimitAccount: boolean
  totalLimit?: number
  usedLimit?: number
  availableLimit?: number
  singleLimit?: number
  dailyLimit?: number
  pendingIncreaseReview: boolean
  pendingProposedTotal?: number
  lastAdjustHint?: string
}

export interface CreditScoreOverview {
  evaluated: boolean
  score?: number
  riskLevel?: string
  riskLevelName?: string
  modelVersion?: string
  evaluatedAt?: string
  calcDurationMs?: number
  metrics?: Record<string, unknown>
  featureSnapshot?: Record<string, unknown>
  badProbabilityHint?: string
  recentTrend?: CreditHistoryPoint[]
}
