import request from '@/utils/request'

// ... 工具统计接口保持不变 ...
export function getToolStats() {
  return request({ url: '/api/community/tools/stats', method: 'get' })
}
export function reportToolUsage(code) {
  return request({ url: `/api/community/tools/report/${code}`, method: 'post' })
}

// ... 需求墙接口 (只留这两个) ...
export function getPublicFeedbacks() {
  return request({
    url: '/api/community/feedback/list',
    method: 'get'
  })
}

export function submitFeedback(data) {
  return request({
    url: '/api/community/feedback/submit',
    method: 'post',
    data
  })
}
