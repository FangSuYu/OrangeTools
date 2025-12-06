import request from '@/utils/request'

// 登录接口
export function login(data) {
  return request({
    url: '/api/auth/login',
    method: 'post',
    data
  })
}

// 注册接口
export function register(data) {
  return request({
    url: '/api/auth/register',
    method: 'post',
    data
  })
}
