import request from './request'
import type { LimitAdjustPending } from './types/riskLimit'

export const riskLimitApi = {
  pendingList() {
    return request.get<any, LimitAdjustPending[]>('/risk/limit/pending-requests')
  },
  approve(id: number, comment?: string) {
    return request.post<any, void>(`/risk/limit/pending-requests/${id}/approve`, { comment })
  },
  reject(id: number, comment?: string) {
    return request.post<any, void>(`/risk/limit/pending-requests/${id}/reject`, { comment })
  },
  runAdjustmentBatch() {
    return request.post<any, void>('/risk/limit/run-adjustment-batch')
  }
}
