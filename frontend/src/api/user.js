import request from '@/utils/request'

export function updateProfile(data) {
  return request({
    url: '/api/user/profile',
    method: 'put',
    data
  })
}

export function updatePassword(data) {
  return request({
    url: '/api/user/password',
    method: 'put',
    data
  })
}
