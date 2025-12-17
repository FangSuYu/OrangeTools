package cn.orangetools.modules.course.model;

/**
 * @author YuHeng
 * @project backend
 * @file CourseDetail
 * @date 2025/12/17 10:17
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单门课程的详细信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseDetail {
    private String name;      // 课程名
    private String teacher;   // 教师
    private String location;  // 地点
    private String rawWeekStr; // 原始周次字符串 (用于前端展示，如 "2-6,8周")
    private List<Integer> weeks; // 解析后的周次列表 (用于计算是否在当前周)
}
