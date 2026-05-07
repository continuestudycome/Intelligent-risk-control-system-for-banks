import request from './request'
import type {
  LoanApplication,
  LoanApplicationCreateRequest,
  LoanApplicationReviewRequest
} from './types/loanApplication'

export const customerLoanApi = {
  create(data: LoanApplicationCreateRequest) {
    return request.post<any, LoanApplication>('/customer/loan-application', data)
  },
  list() {
    return request.get<any, LoanApplication[]>('/customer/loan-application')
  },
  detail(id: number) {
    return request.get<any, LoanApplication>(`/customer/loan-application/${id}`)
  }
}

export const riskLoanApi = {
  list(params?: { currentStatus?: string; applyType?: number }) {
    return request.get<any, LoanApplication[]>('/risk/loan-application', { params })
  },
  detail(id: number) {
    return request.get<any, LoanApplication>(`/risk/loan-application/${id}`)
  },
  review(id: number, data: LoanApplicationReviewRequest) {
    return request.post<any, LoanApplication>(`/risk/loan-application/${id}/review`, data)
  }
}
