import type { CreditScoreOverview } from './credit'

export interface CustomerRiskBasic {
  customerId: number
  customerNo: string
  realName: string
  phoneMasked: string
  idCardMasked: string
  province?: string
  city?: string
  creditLevel?: string
  isBlacklist?: number
  blacklistReason?: string
  annualIncome?: number
  assetAmount?: number
  profileCompleted?: number
  registerTime?: string
}

export interface ProfileTxnBrief {
  id: number
  transactionNo: string
  transactionType: number
  transactionTypeName: string
  amount: number
  riskStatus: string
  riskStatusName: string
  transactionTime: string
}

export interface TransactionRiskSummary {
  lookbackDays: number
  totalCount: number
  totalAmount: number
  lowCount: number
  mediumCount: number
  highCount: number
  interceptedCount: number
  confirmedFraudCount: number
  recentRiskyTransactions: ProfileTxnBrief[]
}

export interface FraudAlertBrief {
  id: number
  transactionNo: string
  alertLevel: string
  mlScore?: number
  status: string
  createTime: string
  hitRulesSummary?: string
}

export interface LoanAppProfileRow {
  id: number
  applicationNo: string
  applyTypeName: string
  currentStatus: string
  currentStatusName: string
  applyAmount: number
  createTime: string
}

export interface LoanApplicationProfileSummary {
  pendingCount: number
  approvedCount: number
  rejectedCount: number
  otherStatusCount: number
  recentApplications: LoanAppProfileRow[]
}

export interface CustomerRiskProfile {
  basic: CustomerRiskBasic
  portraitTags: string[]
  credit: CreditScoreOverview
  transactions: TransactionRiskSummary
  fraudAlerts: FraudAlertBrief[]
  loans: LoanApplicationProfileSummary
}
