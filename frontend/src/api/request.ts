import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const BASE_URL = '/HIS'

// 开发模式开关：设为 true 时后端不可用也不弹错误、不跳登录页，所有页面可正常浏览
export const DEV_MODE = true

const instance = axios.create({
  baseURL: BASE_URL,
  timeout: DEV_MODE ? 5000 : 30000,
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
      if (!DEV_MODE) {
        ElMessage.error(data.errorMsg || '请求失败')
      }
      return Promise.reject(new Error(data.errorMsg || '请求失败'))
    }
    return response
  },
  (error) => {
    if (DEV_MODE) {
      // 开发模式：静默失败，不弹错误提示，不跳转登录页
      console.warn('[DEV_MODE] API请求失败:', error.message)
      return Promise.reject(error)
    }
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

/** 下载文件 */
export function download(url: string, filename: string, params?: any) {
  return instance.get(url, {
    params,
    responseType: 'blob'
  }).then((response) => {
    // 检查是否是错误响应（404等）
    if (response.status !== 200) {
      throw new Error('下载失败')
    }

    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = window.URL.createObjectURL(blob)
    link.download = filename
    link.click()
    window.URL.revokeObjectURL(link.href)
  })
}

/** 上传文件 */
export function upload<T = any>(url: string, file: File, fieldName = 'file', config?: AxiosRequestConfig): Promise<Result<T>> {
  const formData = new FormData()
  formData.append(fieldName, file)
  return instance.post(url, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    ...config,
  }).then((res) => res.data)
}

export default instance
