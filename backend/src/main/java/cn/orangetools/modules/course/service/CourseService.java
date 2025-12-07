package cn.orangetools.modules.course.service;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.modules.course.model.*;
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
        // 临时存储：StudentInfo -> StudentSchedule
        // 用 Map 方便我们在解析过程中随时把课程塞给对应的学生
        Map<StudentInfo, StudentSchedule> scheduleMap = new HashMap<>();

        Set<String> allColleges = new HashSet<>();
        Set<String> allMajors = new HashSet<>();
        Set<String> allGrades = new HashSet<>();

        // 全局最大周次，默认为 20，解析过程中动态更新
        final int[] maxWeekRef = {20};

        for (MultipartFile file : files) {
            try {
                // 解析器
                new ExcelParser(file, scheduleMap, maxWeekRef).parse();
            } catch (Exception e) {
                log.error("解析异常: " + file.getOriginalFilename(), e);
            }
        }

        // 组装结果
        result.setTotalPeople(scheduleMap.size());
        result.setMaxWeek(maxWeekRef[0]);
        result.setData(new ArrayList<>(scheduleMap.values())); // 转为 List

        // 提取元数据
        for (StudentInfo s : scheduleMap.keySet()) {
            if (s.getCollege() != null) allColleges.add(s.getCollege());
            if (s.getMajor() != null) allMajors.add(s.getMajor());
            if (s.getGrade() != null) allGrades.add(s.getGrade());
        }
        result.setAllColleges(allColleges);
        result.setAllMajors(allMajors);
        result.setAllGrades(allGrades);

        return result;
    }

    /**
     * 解析器内部类
     */
    private class ExcelParser extends AnalysisEventListener<Map<Integer, String>> {
        private final MultipartFile file;
        private final Map<StudentInfo, StudentSchedule> scheduleMap;
        private final int[] maxWeekRef;

        private StudentInfo currentStudent = null;
        private Integer mondayColIndex = null;
        private Integer dataStartRowIndex = null;

        public ExcelParser(MultipartFile file, Map<StudentInfo, StudentSchedule> scheduleMap, int[] maxWeekRef) {
            this.file = file;
            this.scheduleMap = scheduleMap;
            this.maxWeekRef = maxWeekRef;
        }

        public void parse() throws IOException {
            EasyExcel.read(file.getInputStream(), this).sheet().doRead();
        }

        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            int rowIndex = context.readRowHolder().getRowIndex();

            // 1. 提取个人信息 (第3行)
            if (rowIndex == 2) {
                StringBuilder rawTextBuilder = new StringBuilder();
                for (String val : rowData.values()) {
                    if (val != null) rawTextBuilder.append(val).append(" ");
                }
                this.currentStudent = extractStudentInfo(rawTextBuilder.toString());
                // 如果 Map 里还没有这个学生，初始化一个
                scheduleMap.putIfAbsent(this.currentStudent, new StudentSchedule(this.currentStudent, new ArrayList<>()));
                return;
            }

            // 2. 寻找锚点 "星期一"
            if (mondayColIndex == null) {
                for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                    if (entry.getValue() != null && entry.getValue().contains("星期一")) {
                        mondayColIndex = entry.getKey();
                        dataStartRowIndex = rowIndex + 1;
                        break;
                    }
                }
                return;
            }

            // 3. 提取数据
            if (mondayColIndex != null && dataStartRowIndex != null) {
                if (rowIndex >= dataStartRowIndex && rowIndex < dataStartRowIndex + 10) {
                    int slotIndex = rowIndex - dataStartRowIndex + 1; // 第几节 (1-10)

                    for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                        int actualColIndex = mondayColIndex + dayOffset;
                        String cellContent = rowData.get(actualColIndex);

                        // 解析出这节课所有的忙碌周次
                        List<Integer> busyWeeks = parseWeeksFromCell(cellContent);

                        // 如果有忙碌周次，就记录下来
                        if (!busyWeeks.isEmpty()) {
                            // 更新全局最大周次
                            for (Integer w : busyWeeks) {
                                if (w > maxWeekRef[0]) maxWeekRef[0] = w;
                            }

                            // 存入当前学生的课表
                            if (this.currentStudent != null) {
                                StudentSchedule schedule = scheduleMap.get(this.currentStudent);
                                // Day 是 1-7
                                schedule.addCourse(new RawCourseItem(dayOffset + 1, slotIndex, busyWeeks));
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        /**
         * 【核心算法升级】从单元格内容中解析出所有忙碌的周次列表
         * 返回: [1, 2, 3, 4, 8, 9] 这种列表
         */
        private List<Integer> parseWeeksFromCell(String content) {
            List<Integer> result = new ArrayList<>();
            if (content == null || content.trim().length() <= 1) return result;
            if (content.contains("无课") || content.contains("时间段空闲")) return result;

            // 1. 切割课程 (省略号分隔)
            String[] courses = content.split("[…。\\.]+");

            for (String course : courses) {
                if (course.trim().isEmpty()) continue;

                // 2. 提取周次字符串
                Pattern pattern = Pattern.compile("[【\\[](.*?)[周\\]】]");
                Matcher matcher = pattern.matcher(course);

                if (matcher.find()) {
                    String weekStr = matcher.group(1); // "2-6,8"
                    List<Integer> parsed = expandWeeks(weekStr);
                    result.addAll(parsed);
                } else {
                    // 没写周次，默认 1-20 周都忙 (兜底策略)
                    for (int i = 1; i <= 20; i++) result.add(i);
                }
            }
            return result;
        }

        /**
         * 展开周次字符串
         * "2-4,6" -> [2, 3, 4, 6]
         */
        private List<Integer> expandWeeks(String weekStr) {
            List<Integer> list = new ArrayList<>();
            try {
                String[] parts = weekStr.split("[,，]");
                for (String part : parts) {
                    if (part.contains("-")) {
                        String[] range = part.split("-");
                        int start = Integer.parseInt(range[0]);
                        int end = Integer.parseInt(range[1]);
                        for (int i = start; i <= end; i++) list.add(i);
                    } else {
                        list.add(Integer.parseInt(part));
                    }
                }
            } catch (Exception e) {
                // 解析失败忽略
            }
            return list;
        }

        // ... (extractStudentInfo 和 getValueByRegex 保持不变，请直接保留你上一次的代码) ...
        // 为了篇幅我不重复贴 extractStudentInfo 了，它不需要变
        private StudentInfo extractStudentInfo(String text) {
            StudentInfo info = new StudentInfo();
            String cleanText = text.replaceAll("：", ":").replaceAll("\\s+", " ");
            info.setName(getValueByRegex(cleanText, "姓名:(\\S+)"));
            info.setGrade(getValueByRegex(cleanText, "年级:(\\S+)"));
            info.setCollege(getValueByRegex(cleanText, "院系:(\\S+)"));
            info.setMajor(getValueByRegex(cleanText, "专业:(\\S+)"));
            String explicitCode = getValueByRegex(cleanText, "学号[:：](\\w+)");
            info.setCode(explicitCode != null ? explicitCode : "");
            info.setClassName("");
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