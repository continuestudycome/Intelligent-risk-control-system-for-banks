import request from './request'
import type { FraudRuleItem, FraudRuleValidateResult } from './types/fraudRule'

export const fraudRuleApi = {
  list() {
    return request.get<any, FraudRuleItem[]>('/risk/fraud-rules')
  },
  update(
    id: number,
    body: {
      ruleCondition: string
      riskLevel?: string
      priority?: number
      status?: number
      remark?: string
    }
  ) {
    return request.put<any, FraudRuleItem>(`/risk/fraud-rules/${id}`, body)
  },
  validateAi(rules: { ruleCode: string; ruleCondition: string }[]) {
    return request.post<any, FraudRuleValidateResult>('/risk/fraud-rules/validate-ai', { rules })
  },
  simulateDefault() {
    return request.post<any, Record<string, unknown>[]>('/risk/fraud-rules/simulate-default')
  }
}
