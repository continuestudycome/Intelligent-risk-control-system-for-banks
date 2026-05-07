import request from './request'
import type { RiskTransactionQuery, TransactionRecord } from './types/transaction'

export const riskTransactionApi = {
  queryRecords(params: RiskTransactionQuery) {
    return request.get<any, TransactionRecord[]>('/risk/transaction/records', { params })
  },
  getDetail(id: number) {
    return request.get<any, TransactionRecord>(`/risk/transaction/${id}`)
  }
}
