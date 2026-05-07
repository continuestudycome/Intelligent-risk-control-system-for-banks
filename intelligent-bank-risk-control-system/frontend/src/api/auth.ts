import request from './request'
import type { CaptchaResponse, LoginRequest, RegisterRequest, TokenResponse } from './types/auth'

export const authApi = {
    // 登录
    login(data: LoginRequest) {
        return request.post<any, TokenResponse>('/auth/login', data)
    },

    // 注册
    register(data: RegisterRequest) {
        return request.post<any, null>('/auth/register', data)
    },

    // 刷新令牌
    refreshToken(refreshToken: string) {
        return request.post<any, TokenResponse>('/auth/refresh', { refreshToken })
    },

    // 登出
    logout() {
        return request.post('/auth/logout')
    },

    // 获取验证码
    getCaptcha(uuid: string) {
        return request.get<any, CaptchaResponse>('/auth/captcha', { params: { uuid } })
    }
}
