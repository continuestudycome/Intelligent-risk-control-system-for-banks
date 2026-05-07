export interface LoginRequest {
  username: string
  password: string
  captcha?: string
  rememberMe?: boolean
}

export interface RegisterRequest {
  username: string
  password: string
  confirmPassword: string
  realName: string
  phone: string
  email?: string
}

export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  email?: string
  userType: 1 | 2
  status: number
  riskLevel: number
  profileCompleted: boolean
}

export interface TokenResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  expireTime: string
  userInfo: UserInfo
}

export interface CaptchaResponse {
  uuid: string
  image: string
}
