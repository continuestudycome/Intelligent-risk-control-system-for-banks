import request from './request'
import type { FraudAlert, FraudAlertReviewRequest, FraudCase } from './types/fraud'

export const fraudApi = {
  listAlerts(status?: string) {
    return request.get<any, FraudAlert[]>('/risk/fraud/alerts', {
      params: status ? { status } : undefined
    })
  },

  review(alertId: number, data: FraudAlertReviewRequest) {
    return request.post<any, FraudAlert>(`/risk/fraud/alerts/${alertId}/review`, data)
  },

  listCases() {
    return request.get<any, FraudCase[]>('/risk/fraud/cases')
  }
}
