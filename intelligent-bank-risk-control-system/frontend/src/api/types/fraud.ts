export interface FraudAlert {
  id: number
  transactionId: number
  transactionNo: string
  customerId: number
  alertLevel: string
  hitRules: string | null
  mlScore: number | null
  mlModelVersion: string | null
  featureSnapshot: string | null
  status: string
  reviewerId: number | null
  reviewComment: string | null
  reviewTime: string | null
  createTime: string
}

export interface FraudAlertReviewRequest {
  decision: 'CONFIRM_FRAUD' | 'FALSE_POSITIVE'
  comment?: string
  fraudType?: string
}

export interface FraudCase {
  id: number
  transactionId: number
  alertId: number | null
  customerId: number
  fraudType: string | null
  confirmedResult: number
  labelSource: number
  createTime: string
}
