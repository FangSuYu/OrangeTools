package cn.orangetools.modules.course.service;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.modules.course.model.AnalysisResult;
import cn.orangetools.modules.course.model.CourseCell;
import cn.orangetools.modules.course.model.StudentInfo;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YuHeng
 * @project backend
 * @file CourseService
 * @date 2025/12/6 22:14
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

@Slf4j
@Service
public class CourseService {

    public AnalysisResult analyze(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new ServiceException("请至少上传一个 Excel 文件");
        }

        AnalysisResult result = new AnalysisResult();
        Set<StudentInfo> allStudents = new HashSet<>();

        Set<String> allColleges = new HashSet<>();
        Set<String> allMajors = new HashSet<>();
        Set<String> allGrades = new HashSet<>();

        // 初始化日程表 (10行 * 7列)
        Map<Integer, Map<Integer, CourseCell>> schedule = initEmptySchedule();

        for (MultipartFile file : files) {
            try {
                // 使用智能解析器
                new ExcelParser(file, allStudents, schedule).parse();
            } catch (Exception e) {
                log.error("解析异常: " + file.getOriginalFilename(), e);
            }
        }

        // 组装返回结果
        result.setTotalPeople(allStudents.size());
        result.setAllStudents(new ArrayList<>(allStudents));

        for (StudentInfo s : allStudents) {
            if (s.getCollege() != null) allColleges.add(s.getCollege());
            if (s.getMajor() != null) allMajors.add(s.getMajor());
            if (s.getGrade() != null) allGrades.add(s.getGrade());
        }
        result.setAllColleges(allColleges);
        result.setAllMajors(allMajors);
        result.setAllGrades(allGrades);
        result.setSchedule(schedule);

        return result;
    }

    private Map<Integer, Map<Integer, CourseCell>> initEmptySchedule() {
        Map<Integer, Map<Integer, CourseCell>> map = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            Map<Integer, CourseCell> weekMap = new HashMap<>();
            for (int j = 1; j <= 7; j++) {
                weekMap.put(j, new CourseCell()); // 初始化 1-7 (周一到周日)
            }
            map.put(i, weekMap);
        }
        return map;
    }

    /**
     * 智能 Excel 解析器
     * 特性：自动寻找 "星期一" 定位列，自动寻找数据起始行
     */
    private class ExcelParser extends AnalysisEventListener<Map<Integer, String>> {
        private final MultipartFile file;
        private final Set<StudentInfo> allStudents;
        private final Map<Integer, Map<Integer, CourseCell>> schedule;

        private StudentInfo currentStudent = null;

        // 智能定位坐标
        private Integer mondayColIndex = null; // "星期一" 在第几列
        private Integer dataStartRowIndex = null; // 数据从第几行开始 (表头下一行)

        public ExcelParser(MultipartFile file, Set<StudentInfo> allStudents, Map<Integer, Map<Integer, CourseCell>> schedule) {
            this.file = file;
            this.allStudents = allStudents;
            this.schedule = schedule;
        }

        public void parse() throws IOException {
            EasyExcel.read(file.getInputStream(), this).sheet().doRead();
        }

        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            int rowIndex = context.readRowHolder().getRowIndex();

            // --- 阶段 1: 提取个人信息 (第3行, Index 2) ---
            if (rowIndex == 2) {
                StringBuilder rawTextBuilder = new StringBuilder();
                for (String val : rowData.values()) {
                    if (val != null) rawTextBuilder.append(val).append(" ");
                }
                this.currentStudent = extractStudentInfo(rawTextBuilder.toString());
                allStudents.add(this.currentStudent);
                return;
            }

            // --- 阶段 2: 寻找表头锚点 (寻找 "星期一") ---
            if (mondayColIndex == null) {
                for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                    String val = entry.getValue();
                    if (val != null && val.contains("星期一")) {
                        // 找到了锚点！
                        mondayColIndex = entry.getKey();
                        dataStartRowIndex = rowIndex + 1; // 数据从下一行开始
                        log.info("文件[{}] 定位成功: 星期一在第 {} 列, 数据从第 {} 行开始",
                                file.getOriginalFilename(), mondayColIndex, dataStartRowIndex);
                        break;
                    }
                }
                return; // 这一行是表头，不用处理数据，直接返回
            }

            // --- 阶段 3: 提取课表数据 ---
            // 只有当锚点找到，且当前行在数据范围内 (10节课) 时才处理
            if (mondayColIndex != null && dataStartRowIndex != null) {
                if (rowIndex >= dataStartRowIndex && rowIndex < dataStartRowIndex + 10) {

                    int slotIndex = rowIndex - dataStartRowIndex + 1; // 算出当前是第几节 (1-10)

                    // 遍历周一(1) 到 周日(7)
                    for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                        // 实际列索引 = 星期一的索引 + 偏移量
                        int actualColIndex = mondayColIndex + dayOffset;
                        String cellContent = rowData.get(actualColIndex);

                        // 忙碌判定
                        boolean isBusy = isEffectiveCourse(cellContent);

                        // 更新日程表 (dayOfWeek: 1=周一, 7=周日)
                        int dayOfWeek = dayOffset + 1;
                        CourseCell cell = schedule.get(slotIndex).get(dayOfWeek);

                        if (this.currentStudent != null) {
                            if (isBusy) {
                                cell.addBusy(this.currentStudent);
                            } else {
                                cell.addFree(this.currentStudent);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        // 判定是否是有效课程
        private boolean isEffectiveCourse(String content) {
            if (content == null) return false;
            String text = content.trim();
            // 排除无意义字符
            if (text.length() <= 1) return false;
            // 排除特定的空闲标识
            if (text.contains("无课") || text.contains("时间段空闲")) return false;
            return true;
        }

        private StudentInfo extractStudentInfo(String text) {
            StudentInfo info = new StudentInfo();
            String cleanText = text.replaceAll("：", ":").replaceAll("\\s+", " ");

            info.setName(getValueByRegex(cleanText, "姓名:(\\S+)"));
            info.setGrade(getValueByRegex(cleanText, "年级:(\\S+)"));
            info.setCollege(getValueByRegex(cleanText, "院系:(\\S+)"));
            info.setMajor(getValueByRegex(cleanText, "专业:(\\S+)"));

            // 你的特定需求：不解析数字作为学号
            String explicitCode = getValueByRegex(cleanText, "学号[:：](\\w+)");
            info.setCode(explicitCode != null ? explicitCode : "");
            info.setClassName("");

            // 兜底姓名
            if (info.getName() == null) {
                String filename = file.getOriginalFilename();
                if (filename != null && filename.contains(".")) {
                    info.setName(filename.substring(0, filename.lastIndexOf(".")));
                } else {
                    info.setName("未知同学");
                }
            }
            return info;
        }

        private String getValueByRegex(String text, String regex) {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return null;
        }
    }
}