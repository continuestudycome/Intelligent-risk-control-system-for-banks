import request from './request'
import type { CustomerRiskProfile } from './types/riskProfile'

export const riskProfileApi = {
  getProfile(customerId: number) {
    return request.get<any, CustomerRiskProfile>(`/risk/profile/customers/${customerId}`)
  }
}
