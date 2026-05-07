export interface CustomerProfile {
  customerType: 1 | 2
  realName: string
  idCardNo: string
  phone: string
  email?: string
  province?: string
  city?: string
  address?: string
  annualIncome?: number
  assetAmount?: number
  creditAuthorized: boolean
  profileCompleted: boolean
}

export interface CustomerProfileSaveRequest {
  customerType: 1 | 2
  realName: string
  idCardNo: string
  phone: string
  email?: string
  province?: string
  city?: string
  address?: string
  annualIncome?: number
  assetAmount?: number
  creditAuthorized: boolean
}
