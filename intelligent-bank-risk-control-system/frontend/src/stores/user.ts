import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import {
    setToken,
    getToken,
    removeToken,
    setRefreshToken,
    getRefreshToken,
    removeRefreshToken,
    setUserInfo,
    getUserInfo,
    removeUserInfo
} from '@/utils/auth'
import type { LoginRequest, RegisterRequest, TokenResponse, UserInfo } from '@/api/types/auth'

export const useUserStore = defineStore('user', () => {
    // State
    const token = ref(getToken() || '')
    const refreshToken = ref(getRefreshToken() || '')
    const userInfo = ref<UserInfo | null>(getUserInfo())
    const isLoggedIn = computed(() => !!token.value)
    const isRiskOfficer = computed(() => userInfo.value?.userType === 2)

    // Actions
    const login = async (loginData: LoginRequest) => {
        const res: TokenResponse = await authApi.login(loginData)
        token.value = res.accessToken
        refreshToken.value = res.refreshToken
        userInfo.value = res.userInfo

        setToken(res.accessToken)
        setRefreshToken(res.refreshToken)
        setUserInfo(res.userInfo)

        return res
    }

    const register = async (registerData: RegisterRequest) => {
        await authApi.register(registerData)
    }

    const logout = async () => {
        try {
            await authApi.logout()
        } catch (e) {
            // 忽略错误
        }

        token.value = ''
        refreshToken.value = ''
        userInfo.value = null

        removeToken()
        removeRefreshToken()
        removeUserInfo()
    }

    const refreshAccessToken = async () => {
        try {
            const res: TokenResponse = await authApi.refreshToken(refreshToken.value)
            token.value = res.accessToken
            refreshToken.value = res.refreshToken
            userInfo.value = res.userInfo

            setToken(res.accessToken)
            setRefreshToken(res.refreshToken)
            setUserInfo(res.userInfo)

            return res.accessToken
        } catch (error) {
            logout()
            throw error
        }
    }

    const updateProfileCompleted = (completed: boolean) => {
        if (!userInfo.value) return
        userInfo.value = { ...userInfo.value, profileCompleted: completed }
        setUserInfo(userInfo.value)
    }

    return {
        token,
        refreshToken,
        userInfo,
        isLoggedIn,
        isRiskOfficer,
        login,
        register,
        logout,
        refreshAccessToken,
        updateProfileCompleted
    }
})
