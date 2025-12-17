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

                        // 【修改点 1】解析详细信息，而不是只解析数字
                        List<CourseDetail> details = parseDetailsFromCell(cellContent);

                        // 【修改点 2】计算忙碌周次并集 (兼容旧逻辑，用于前端快速计算热力图)
                        Set<Integer> busyWeekSet = new HashSet<>();
                        for (CourseDetail d : details) {
                            if (d.getWeeks() != null) {
                                busyWeekSet.addAll(d.getWeeks());
                            }
                        }
                        List<Integer> busyWeeks = new ArrayList<>(busyWeekSet);

                        // 如果有忙碌周次，就记录下来
                        if (!busyWeeks.isEmpty()) {
                            // 更新全局最大周次
                            for (Integer w : busyWeeks) {
                                if (w > maxWeekRef[0]) maxWeekRef[0] = w;
                            }

                            // 存入当前学生的课表
                            if (this.currentStudent != null) {
                                StudentSchedule schedule = scheduleMap.get(this.currentStudent);
                                // 【修改点 3】构造 RawCourseItem，同时存入 busyWeeks 和 courseDetails
                                RawCourseItem item = new RawCourseItem();
                                item.setDay(dayOffset + 1);
                                item.setSlot(slotIndex);
                                item.setBusyWeeks(busyWeeks);      // 旧字段 (必须保留)
                                item.setCourseDetails(details);    // 新字段 (本次新增)
                                schedule.addCourse(item);
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        /**
         * 【核心升级】从单元格解析完整的课程详情
         */
        private List<CourseDetail> parseDetailsFromCell(String content) {
            List<CourseDetail> details = new ArrayList<>();
            // 判空逻辑
            if (content == null || content.trim().length() <= 1) return details;
            if (content.contains("无课") || content.contains("时间段空闲")) return details;

            // 1. 切割课程：使用 "……" (至少两个) 作为分隔符
            // 你的示例中是 "………………"，这里用正则兼容不同长度的省略号
            String[] courseBlocks = content.split("…{2,}");

            for (String block : courseBlocks) {
                if (block.trim().isEmpty()) continue;

                // 2. 按行切割 (提取三行信息)
                String[] lines = block.trim().split("\n");
                List<String> validLines = new ArrayList<>();
                for (String line : lines) {
                    if (line.trim().length() > 0) validLines.add(line.trim());
                }

                if (validLines.isEmpty()) continue;

                CourseDetail detail = new CourseDetail();

                // 第一行：课程名
                detail.setName(validLines.get(0));

                // 第二行：教师 + 周次 (例如：杜洪霞【2-6,8周】)
                if (validLines.size() > 1) {
                    String line2 = validLines.get(1);
                    // 正则：任意字符(Group1:老师) + 【 + 内容(Group2:周次) + 周/】
                    // 示例：杜洪霞【2-6,8周】
                    // 【修复】正则改为严格匹配方括号内部的所有内容
                    // ^(.*?): 教师姓名
                    // [【\\[]: 左括号
                    // (.*?): 核心周次内容 (可能包含"周")
                    // [】\\]]$: 右括号结尾
                    Pattern p = Pattern.compile("^(.*?)[【\\[](.*?)[】\\]]$");
                    Matcher m = p.matcher(line2);
                    if (m.find()) {
                        detail.setTeacher(m.group(1).trim());
                        String weekStrRaw = m.group(2).trim();
                        String weekStrClean = weekStrRaw.replaceAll("周", "");
                        detail.setRawWeekStr(weekStrRaw.endsWith("周") ? weekStrRaw : weekStrRaw + "周"); // 修正展示文本，避免"周周"
                        detail.setWeeks(expandWeeks(weekStrClean)); // 传入干净的数字字符串
                    } else {
                        // 格式不匹配兜底
                        detail.setTeacher(line2);
                        detail.setWeeks(new ArrayList<>());
                    }
                } else {
                    detail.setWeeks(new ArrayList<>());
                }

                // 第三行：地点
                if (validLines.size() > 2) {
                    detail.setLocation(validLines.get(2));
                } else {
                    detail.setLocation("未知地点");
                }

                details.add(detail);
            }
            return details;
        }

        /**
         * 展开周次 (支持逗号、连字符、单双周)
         * 输入: "3-5(单),8-10,16-18(双)"
         * 输出: [3, 5, 8, 9, 10, 16, 18]
         */
        private List<Integer> expandWeeks(String weekStr) {
            List<Integer> list = new ArrayList<>();
            if (weekStr == null || weekStr.isEmpty()) return list;

            // 1. 先按逗号切割
            String[] parts = weekStr.split("[,，]");
            for (String part : parts) {
                try {
                    part = part.trim();
                    if (part.isEmpty()) continue;

                    // 【新增】检测单双周标记 (兼容中英文括号)
                    boolean isOdd = part.contains("(单)") || part.contains("（单）");
                    boolean isEven = part.contains("(双)") || part.contains("（双）");

                    // 【新增】清理非数字字符，保留数字和连字符 (移除 (单), (双) 等)
                    // 这一步会把 "3-5(单)" 变成 "3-5"
                    String cleanPart = part.replaceAll("[\\(（][单双][\\)）]", "").trim();

                    if (cleanPart.contains("-")) {
                        // 2. 处理连字符范围 "3-5"
                        String[] range = cleanPart.split("-");
                        if (range.length >= 2) {
                            int start = Integer.parseInt(range[0].trim());
                            int end = Integer.parseInt(range[1].trim());
                            for (int i = start; i <= end; i++) {
                                // 【新增】根据单双周标记过滤
                                if (isOdd) {
                                    if (i % 2 != 0) list.add(i);
                                } else if (isEven) {
                                    if (i % 2 == 0) list.add(i);
                                } else {
                                    list.add(i); // 无标记，全部添加
                                }
                            }
                        }
                    } else {
                        // 3. 处理单个数字 "8"
                        int val = Integer.parseInt(cleanPart.trim());
                        // 单个数字通常不带单双标记，但为了严谨也加上判断
                        if (isOdd) {
                            if (val % 2 != 0) list.add(val);
                        } else if (isEven) {
                            if (val % 2 == 0) list.add(val);
                        } else {
                            list.add(val);
                        }
                    }
                } catch (Exception e) {
                    log.warn("周次片段解析异常: " + part + " in " + weekStr);
                }
            }
            return list;
        }
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