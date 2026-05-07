export interface TransactionCreateRequest {
  transactionType: number
  fromAccount: string
  toAccount: string
  amount: number
  transactionProvince: string
  transactionCity: string
  purpose: string
}

export interface TransactionRecord {
  id: number
  transactionNo: string
  transactionType: number
  transactionTypeName: string
  fromAccount: string
  toAccount: string
  amount: number
  transactionProvince: string
  transactionCity: string
  purpose: string
  riskStatus: string
  riskStatusName: string
  handleResult: number
  handleResultName: string
  transactionTime: string
  /** 创建接口返回时的风险提示文案 */
  riskMessage?: string
  /** 风控端列表：客户信息 */
  customerId?: number
  customerName?: string
  customerPhone?: string
}

export interface TransactionQuery {
  transactionType?: number
  riskStatus?: string
  minAmount?: number
  maxAmount?: number
  startTime?: string
  endTime?: string
}

/** 风控端交易查询（在 TransactionQuery 基础上增加客户维度） */
export interface RiskTransactionQuery extends TransactionQuery {
  customerId?: number
  customerKeyword?: string
}
