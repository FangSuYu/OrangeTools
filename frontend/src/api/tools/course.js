import request from '@/utils/request'

/**
 * 上传课表文件进行分析
 * @param {FormData} data - 包含 'files' 的 FormData 对象
 */
export function analyzeCourse(data) {
  return request({
    url: '/api/tools/course/analyze',
    method: 'post',
    data,
    // 上传文件必须设置 headers，虽然 Axios 甚至会自动识别，但显式写出来更保险
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
