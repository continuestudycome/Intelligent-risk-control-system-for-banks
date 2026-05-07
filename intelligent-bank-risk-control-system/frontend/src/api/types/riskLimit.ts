export interface LimitAdjustPending {
  id: number
  customerId: number
  customerName: string
  currentTotalLimit: number
  proposedTotalLimit: number
  triggerScore: number | null
  triggerRiskLevel: string | null
  reason: string | null
  status: string
  createTime: string
}
