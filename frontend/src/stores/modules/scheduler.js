import { defineStore } from 'pinia'
import { ref } from 'vue'
import { uploadAndParse as uploadAndParseApi } from '@/api/tools/scheduler'
import { ElMessage } from 'element-plus'

export const useSchedulerStore = defineStore(
  'scheduler',
  () => {
    // --- State ---
    const studentPool = ref([])
    const scheduleSolution = ref({})
    const currentWeek = ref(1)

    // --- Actions ---

    const uploadAndParse = async (fileList) => {
      if (!fileList || fileList.length === 0) {
        ElMessage.warning('请先选择文件')
        return false
      }

      const formData = new FormData()
      fileList.forEach((f) => {
        formData.append('files', f.raw || f)
      })

      try {
        const res = await uploadAndParseApi(formData)

        let dataList = []
        if (Array.isArray(res)) {
          dataList = res
        } else if (res?.data && Array.isArray(res.data)) {
          dataList = res.data
        } else if (res?.data?.data && Array.isArray(res.data.data)) {
          dataList = res.data.data
        }

        if (dataList.length > 0) {
          studentPool.value = dataList
          scheduleSolution.value = {}
          ElMessage.success(`成功解析 ${dataList.length} 位同学的课表`)
          return true
        } else {
          ElMessage.warning('解析成功，但没有获取到学生数据')
          return false
        }
      } catch (error) {
        ElMessage.error('解析失败: ' + (error.message || '网络错误'))
        return false
      }
    }

    /**
     * 【核心修复】检查冲突
     * 修复了 "多门课只检查第一门" 的 Bug
     */
    const checkConflict = (student, day, section, week) => {
      if (!student || !student.scheduleRaw) return { conflict: false }

      const tDay = Number(day)
      const tSection = Number(section)
      const tWeek = Number(week)

      // 1. 找出该天该节的所有课程 (可能有多门，比如单双周不同课)
      const courses = student.scheduleRaw.filter(
        (item) => Number(item.day) === tDay && Number(item.section) === tSection
      )

      // 2. 遍历所有课程，只要有一门课在当前周忙碌，就是冲突
      for (const course of courses) {
        if (course.busyWeeks && course.busyWeeks.map(w => Number(w)).includes(tWeek)) {
          return { conflict: true, reason: course.courseName || '有课' }
        }
      }

      return { conflict: false }
    }

    /**
     * 添加学生到格子 (修复：强制去重)
     */
    const addStudentToSlot = (day, section, student) => {
      const key = `${day}_${section}`
      if (!scheduleSolution.value[key]) scheduleSolution.value[key] = []

      // 检查是否已存在
      const exists = scheduleSolution.value[key].some((s) => s.id === student.id)

      if (exists) {
        ElMessage.warning(`${student.name} 已经在该时间段了`)
        return
      }

      scheduleSolution.value[key].push(student)
      ElMessage.success(`已添加 ${student.name}`)
    }

    const removeStudentFromSlot = (day, section, studentId) => {
      const key = `${day}_${section}`
      if (scheduleSolution.value[key]) {
        scheduleSolution.value[key] = scheduleSolution.value[key].filter((s) => s.id !== studentId)
      }
    }

    const clearAll = () => {
      studentPool.value = []
      scheduleSolution.value = {}
      currentWeek.value = 1
    }

    return {
      studentPool,
      scheduleSolution,
      currentWeek,
      uploadAndParse,
      checkConflict,
      addStudentToSlot,
      removeStudentFromSlot,
      clearAll
    }
  },
  {
    persist: true
  }
)
