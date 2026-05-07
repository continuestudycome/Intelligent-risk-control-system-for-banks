import request from './request'
import type { TransactionCreateRequest, TransactionQuery, TransactionRecord } from './types/transaction'

export const transactionApi = {
  create(data: TransactionCreateRequest) {
    return request.post<any, TransactionRecord>('/customer/transaction', data)
  },

  queryRecords(params: TransactionQuery) {
    return request.get<any, TransactionRecord[]>('/customer/transaction/records', { params })
  },

  getDetail(id: number) {
    return request.get<any, TransactionRecord>(`/customer/transaction/${id}`)
  }
}
