import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import { setToken, getToken, removeToken, setRefreshToken, getRefreshToken, removeRefreshToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
    // State
    const token = ref(getToken() || '')
    const refreshToken = ref(getRefreshToken() || '')
    const userInfo = ref(null)
    const isLoggedIn = computed(() => !!token.value)

    // Actions
    const login = async (loginData: any) => {
        const res = await authApi.login(loginData)
        token.value = res.accessToken
        refreshToken.value = res.refreshToken
        userInfo.value = res.userInfo

        setToken(res.accessToken)
        setRefreshToken(res.refreshToken)

        return res
    }

    const register = async (registerData: any) => {
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
    }

    const refreshAccessToken = async () => {
        try {
            const res = await authApi.refreshToken(refreshToken.value)
            token.value = res.accessToken
            refreshToken.value = res.refreshToken
            userInfo.value = res.userInfo

            setToken(res.accessToken)
            setRefreshToken(res.refreshToken)

            return res.accessToken
        } catch (error) {
            logout()
            throw error
        }
    }

    return {
        token,
        refreshToken,
        userInfo,
        isLoggedIn,
        login,
        register,
        logout,
        refreshAccessToken
    }
})
