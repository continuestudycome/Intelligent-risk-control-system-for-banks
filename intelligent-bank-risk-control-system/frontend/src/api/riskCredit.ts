import request from './request'
import type { CreditCustomerBrief, CreditScoreOverview } from './types/riskCredit'

export const riskCreditApi = {
  searchCustomers(keyword?: string) {
    return request.get<any, CreditCustomerBrief[]>('/risk/credit/customers/search', {
      params: { keyword: keyword || undefined }
    })
  },
  getCustomerOverview(customerId: number) {
    return request.get<any, CreditScoreOverview>(`/risk/credit/customers/${customerId}/overview`)
  }
}
