import request from './request'
import type { CreditScoreOverview, CustomerLimitSummary } from './types/credit'

export const creditApi = {
  getOverview() {
    return request.get<any, CreditScoreOverview>('/customer/credit/overview')
  },
  evaluate() {
    return request.post<any, CreditScoreOverview>('/customer/credit/evaluate')
  },
  getLimitSummary() {
    return request.get<any, CustomerLimitSummary>('/customer/credit/limit')
  }
}
