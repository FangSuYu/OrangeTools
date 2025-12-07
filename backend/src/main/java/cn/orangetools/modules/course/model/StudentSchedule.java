package cn.orangetools.modules.course.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file StudentSchedule
 * @date 2025/12/7 10:05
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class StudentSchedule {
    private StudentInfo student;
    // 这个学生所有的课程列表
    private List<RawCourseItem> courses = new ArrayList<>();

    public void addCourse(RawCourseItem item) {
        this.courses.add(item);
    }
}
