import type { UserInfo } from '@/api/types/auth'

const TOKEN_KEY = 'risk_token'
const REFRESH_TOKEN_KEY = 'risk_refresh_token'
const USER_INFO_KEY = 'risk_user_info'

export function getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
    localStorage.removeItem(TOKEN_KEY)
}

export function getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
}

export function setRefreshToken(token: string): void {
    localStorage.setItem(REFRESH_TOKEN_KEY, token)
}

export function removeRefreshToken(): void {
    localStorage.removeItem(REFRESH_TOKEN_KEY)
}

export function getUserInfo(): UserInfo | null {
    const raw = localStorage.getItem(USER_INFO_KEY)
    if (!raw) return null
    try {
        return JSON.parse(raw) as UserInfo
    } catch {
        return null
    }
}

export function setUserInfo(info: UserInfo): void {
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(info))
}

export function removeUserInfo(): void {
    localStorage.removeItem(USER_INFO_KEY)
}
