import request from './request'
import type { RiskWarningOverview } from './types/riskWarning'

export const riskWarningApi = {
  getOverview() {
    return request.get<any, RiskWarningOverview>('/risk/warning/overview')
  }
}
