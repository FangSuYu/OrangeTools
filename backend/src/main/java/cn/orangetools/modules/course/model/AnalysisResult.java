package cn.orangetools.modules.course.model;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author YuHeng
 * @project backend
 * @file AnalysisResult
 * @date 2025/12/6 22:13
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class AnalysisResult {
    private Integer totalPeople;

    // 所有参与分析的学生列表 (用于前端做筛选下拉框的“数据源”)
    private List<StudentInfo> allStudents;

    // 提取出的所有筛选选项 (方便前端直接用)
    private Set<String> allColleges; // 涉及到的所有学院
    private Set<String> allMajors;   // 涉及到的所有专业
    private Set<String> allGrades;   // 涉及到的所有年级

    // 核心课表数据
    private Map<Integer, Map<Integer, CourseCell>> schedule;
}
