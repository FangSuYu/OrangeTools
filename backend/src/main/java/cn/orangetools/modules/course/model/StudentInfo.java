package cn.orangetools.modules.course.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * @author YuHeng
 * @project backend
 * @file StudentInfo
 * @date 2025/12/6 22:11
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfo {
    private String name;    // 姓名 (核心)
    private String code;    // 学号 (已废弃，存空字符串)
    private String college; // 学院
    private String major;   // 专业
    private String grade;   // 年级
    private String className; // 班级

    /**
     * 【核心修改】重写 equals 和 hashCode
     * 既然那串数字无用，我们就只靠 "姓名 + 专业 + 年级" 来区分是否是同一个人。
     * 这样哪怕你复制了 Excel 没改数字，只要改了名字，就是不同的人。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StudentInfo that = (StudentInfo) o;
        // 只要 姓名、专业、年级 一样，就认为是同一个人
        return Objects.equals(name, that.name) &&
                Objects.equals(major, that.major) &&
                Objects.equals(grade, that.grade);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, major, grade);
    }
}
