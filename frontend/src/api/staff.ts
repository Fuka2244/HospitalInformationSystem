import { post } from './request'
import type { LoginResponseDto, StaffLoginDto } from '@/types'

export function staffLogin(data: StaffLoginDto) {
  return post<LoginResponseDto>('/staff/login/jwt', data)
}

export function staffLogout() {
  return post('/staff/loginout/jwt')
}
