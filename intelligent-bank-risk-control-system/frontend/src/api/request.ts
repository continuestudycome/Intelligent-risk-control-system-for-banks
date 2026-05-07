import axios from 'axios'
import type { AxiosInstance, AxiosResponse, AxiosError, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建axios实例
const request: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: 30000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
request.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStore = useUserStore()
        const token = userStore.token

        if (token && config.headers) {
            config.headers['Authorization'] = `Bearer ${token}`
        }
        if (config.data instanceof FormData && config.headers) {
            delete (config.headers as Record<string, unknown>)['Content-Type']
        }

        return config
    },
    (error: AxiosError) => {
        return Promise.reject(error)
    }
)

// 响应拦截器
request.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data

        if (res.code !== 200) {
            ElMessage.error(res.message || '请求失败')

            // Token过期或无效
            if (res.code === 401) {
                const userStore = useUserStore()
                userStore.logout()
                router.push('/login')
            }

            return Promise.reject(new Error(res.message || '请求失败'))
        }

        return res.data
    },
    (error: AxiosError) => {
        const { response } = error

        if (response) {
            switch (response.status) {
                case 401:
                    ElMessage.error('登录已过期，请重新登录')
                    const userStore = useUserStore()
                    userStore.logout()
                    router.push('/login')
                    break
                case 403:
                    ElMessage.error('没有权限访问')
                    break
                case 404:
                    ElMessage.error('请求的资源不存在')
                    break
                case 500:
                    ElMessage.error(
                        (response.data as { message?: string })?.message || '服务器内部错误'
                    )
                    break
                default:
                    ElMessage.error((response.data as any)?.message || '网络错误')
            }
        } else {
            const msg = error.message || ''
            const isTimeout =
                error.code === 'ECONNABORTED' || /timeout/i.test(msg)
            if (isTimeout) {
                ElMessage.error(
                    '请求超时：智能客服需调用大模型，耗时较长；已放宽等待时间，若仍失败请检查后端与 Ollama 是否可用'
                )
            } else {
                ElMessage.error('网络连接失败')
            }
        }

        return Promise.reject(error)
    }
)

export default request
