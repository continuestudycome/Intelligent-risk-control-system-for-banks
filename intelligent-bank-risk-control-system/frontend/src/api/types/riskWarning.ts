import type { TransactionRecord } from './transaction'

export interface RiskWarningStats {
  pendingFraudAlerts: number
  mediumRiskTransactions24h: number
  criticalRiskTransactions24h: number
  interceptedTransactions7d: number
  blacklistCustomers: number
}

export interface FraudAlertRow {
  id: number
  transactionId: number
  transactionNo: string
  customerId: number
  alertLevel: string
  hitRules?: string
  mlScore?: number
  mlModelVersion?: string
  status: string
  createTime: string
}

export interface RiskWarningOverview {
  stats: RiskWarningStats
  fraudAlerts: FraudAlertRow[]
  riskTransactions: TransactionRecord[]
}
