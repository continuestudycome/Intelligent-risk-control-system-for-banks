export interface LoanApprovalRecordItem {
  id: number
  reviewLevel: number
  reviewerName: string
  action: string
  comment: string | null
  createTime: string
}

export interface LoanApplication {
  id: number
  applicationNo: string
  applyType: number
  applyTypeName: string
  applyAmount: number
  applyTerm: number
  applyPurpose: string
  currentStatus: string
  currentStatusName: string
  finalResult: number | null
  finalResultName: string
  finalAmount: number | null
  finalTerm: number | null
  remark: string | null
  createTime: string
  updateTime: string | null
  approvalRecords: LoanApprovalRecordItem[]
}

export interface LoanApplicationCreateRequest {
  applyType: number
  applyAmount: number
  applyTerm?: number
  applyPurpose: string
}

export interface LoanApplicationReviewRequest {
  action: 'PASS' | 'REJECT'
  comment?: string
  finalAmount?: number
  finalTerm?: number
}
