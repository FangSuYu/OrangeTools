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

export function sendCode(data) {
  return request({
    url: '/api/auth/code',
    method: 'post',
    data
  })
}

export function loginEmail(data) {
  return request({
    url: '/api/auth/login/email',
    method: 'post',
    data
  })
}

export function resetPassword(data) {
  return request({
    url: '/api/auth/password/reset',
    method: 'post',
    data
  })
}

export function getUserInfo() {
  return request({
    url: '/api/auth/info',
    method: 'get'
  })
}
