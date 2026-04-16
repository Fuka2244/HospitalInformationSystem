import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const BASE_URL = '/HIS'

const instance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  withCredentials: true,
})

// 请求拦截器
instance.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
)

// 响应拦截器
instance.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const { data } = response
    if (data.success === false) {
      ElMessage.error(data.errorMsg || '请求失败')
      return Promise.reject(new Error(data.errorMsg))
    }
    return response
  },
  (error) => {
    if (error.response?.status === 401) {
      ElMessage.warning('请先登录')
      window.location.href = '/login'
    } else {
      ElMessage.error(error.response?.data?.errorMsg || '网络异常')
    }
    return Promise.reject(error)
  }
)

/** 封装 GET 请求 */
export function get<T = any>(url: string, params?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return instance.get(url, { params, ...config }).then((res) => res.data)
}

/** 封装 POST 请求 */
export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return instance.post(url, data, config).then((res) => res.data)
}

/** 封装 PUT 请求 */
export function put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<Result<T>> {
  return instance.put(url, data, config).then((res) => res.data)
}

/** 封装 DELETE 请求 */
export function del<T = any>(url: string, config?: AxiosRequestConfig): Promise<Result<T>> {
  return instance.delete(url, config).then((res) => res.data)
}

export default instance
