package cn.orangetools.modules.course.model;

/**
 * @author YuHeng
 * @project backend
 * @file RawCourseItem
 * @date 2025/12/7 10:05
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 原始课程项
 * 记录：周几、第几节、哪些周有课
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RawCourseItem {
    private Integer day; // 1-7 (周一到周日)
    private Integer slot; // 1-10 (节次)
    private List<Integer> busyWeeks; // 这节课忙碌的周次列表，如 [1, 2, 3, ... 16]
    // 【新增】存储该时间段内所有课程的详细信息
    private List<CourseDetail> courseDetails = new ArrayList<>();
}
