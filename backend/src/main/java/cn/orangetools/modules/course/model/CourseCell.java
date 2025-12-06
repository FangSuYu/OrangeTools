package cn.orangetools.modules.course.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file CourseCell
 * @date 2025/12/6 21:58
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class CourseCell {
    // 空闲人数
    private Integer freeCount = 0;

    // 存 StudentInfo 对象
    private List<StudentInfo> freeStudents = new ArrayList<>();

    // 忙碌的学生 (做热力图用)
    private List<StudentInfo> busyStudents = new ArrayList<>();

    public void addFree(StudentInfo student) {
        this.freeStudents.add(student);
        this.freeCount++;
    }

    public void addBusy(StudentInfo student) {
        this.busyStudents.add(student);
    }
}
