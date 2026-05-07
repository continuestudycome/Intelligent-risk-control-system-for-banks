export interface FraudRuleItem {
  id: number
  ruleCode: string
  ruleName: string
  ruleType: number
  ruleCondition: string
  riskLevel: string
  priority: number
  hitThreshold: number
  status: number
  version: number
  effectiveTime?: string
  expireTime?: string
  remark?: string
  updateTime?: string
}

export interface FraudRuleValidateResult {
  ok: boolean
  errors: string[]
  hints: string[]
}
