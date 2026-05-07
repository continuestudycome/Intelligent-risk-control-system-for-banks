import type { CreditScoreOverview } from './credit'

export interface CreditCustomerBrief {
  customerId: number
  realName: string
  phone: string
  customerNo: string
  creditLevel?: string
}

export type { CreditScoreOverview }
