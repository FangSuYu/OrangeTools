import request from '@/utils/request'

/**
 * 上传并解析课表
 * @param {FormData} data - 包含文件的 FormData 对象
 */
export function uploadAndParse(data) {
  return request({
    url: '/api/tools/scheduler/parse',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 智能排班计算接口
export function autoSchedule(data) {
  return request({
    url: '/api/tools/scheduler/auto-generate',
    method: 'post',
    data
  })
}
