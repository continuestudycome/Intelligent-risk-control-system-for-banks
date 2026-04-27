import request from './request'

export const authApi = {
    // 登录
    login(data: any) {
        return request.post('/auth/login', data)
    },

    // 注册
    register(data: any) {
        return request.post('/auth/register', data)
    },

    // 刷新令牌
    refreshToken(refreshToken: string) {
        return request.post('/auth/refresh', { refreshToken })
    },

    // 登出
    logout() {
        return request.post('/auth/logout')
    },

    // 获取验证码
    getCaptcha(uuid: string) {
        return request.get('/auth/captcha', { params: { uuid } })
    }
}
