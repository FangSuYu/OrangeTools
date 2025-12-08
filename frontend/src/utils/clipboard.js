// import { ElMessage } from 'element-plus'

/**
 * 兼容性复制文本到剪贴板
 * 自动降级处理 HTTP 环境
 * @param {String} text 要复制的文本
 */
export function copyText(text) {
  return new Promise((resolve, reject) => {
    // 1. 尝试使用现代 API (仅 HTTPS 或 localhost 可用)
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text)
        .then(() => {
          // ElMessage.success('复制成功')
          console.log('复制成功')
          resolve()
        })
        .catch(err => {
          console.error('Clipboard API Error:', err)
          // 如果现代 API 失败，尝试降级
          fallbackCopy(text, resolve, reject)
        })
    } else {
      // 2. HTTP 环境下使用传统方法 (execCommand)
      fallbackCopy(text, resolve, reject)
    }
  })
}

// 传统复制方法实现
function fallbackCopy(text, resolve, reject) {
  try {
    const input = document.createElement('textarea')
    input.value = text
    // 移出屏幕外，避免用户看到
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    input.style.top = '0'
    document.body.appendChild(input)

    input.select()
    const result = document.execCommand('copy')
    document.body.removeChild(input)

    if (result) {
      // ElMessage.success('复制成功')
      console.log('复制成功')
      resolve()
    } else {
      // ElMessage.error('复制失败，请手动复制')
      console.log('复制失败，请手动复制')
      reject(new Error('execCommand failed'))
    }
  } catch (err) {
    // ElMessage.error('当前环境不支持自动复制')
    console.log('当前环境不支持自动复制')
    console.error('Fallback Copy Error:', err)
    reject(err)
  }
}
