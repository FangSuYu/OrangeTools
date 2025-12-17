package cn.orangetools.modules.scheduler.service;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.modules.scheduler.dto.AutoScheduleRequest;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.SchedulerRawItemDTO;
import cn.orangetools.modules.scheduler.dto.SchedulerStudentDTO;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YuHeng
 * @project backend
 * @file SchedulerService
 * @date 2025/12/15 18:01
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

/**
 * 排班助手核心业务逻辑
 * <p>
 * 负责解析上传的 Excel 文件，并转换为标准化的 DTO 列表返回给前端。
 * 不涉及数据库存储。
 * </p>
 *
 * @author YuHeng
 */
@Slf4j
@Service
public class SchedulerService {

    @Autowired
    private ScheduleStrategyFactory strategyFactory;

    public List<SchedulerStudentDTO> parseFiles(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new ServiceException("请至少上传一个 Excel 文件");
        }

        List<SchedulerStudentDTO> resultList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                SchedulerExcelParser parser = new SchedulerExcelParser();
                EasyExcel.read(file.getInputStream(), parser).sheet().doRead();

                if (parser.getStudentDTO() != null) {
                    resultList.add(parser.getStudentDTO());
                }
            } catch (IOException e) {
                log.error("文件流读取失败: {}", file.getOriginalFilename(), e);
                throw new ServiceException("文件读取失败：" + file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Excel解析异常: {}", file.getOriginalFilename(), e);
                throw new ServiceException("解析异常(" + file.getOriginalFilename() + "): " + e.getMessage());
            }
        }

        return resultList;
    }

    private static class SchedulerExcelParser extends AnalysisEventListener<Map<Integer, String>> {

        private SchedulerStudentDTO studentDTO = new SchedulerStudentDTO();

        private Integer mondayColIndex = null;
        private Integer dataStartRowIndex = null;

        // --- 元数据正则 (严格匹配 Key[:：]Value 格式) ---
        private static final Pattern NAME_PATTERN = Pattern.compile("姓名[:：](\\S+)");
        private static final Pattern GRADE_PATTERN = Pattern.compile("年级[:：](\\S+)");
        private static final Pattern COLLEGE_PATTERN = Pattern.compile("院系[:：](\\S+)");
        private static final Pattern MAJOR_PATTERN = Pattern.compile("专业[:：](\\S+)");

        public SchedulerExcelParser() {
            // 初始化时生成唯一 ID，确保前端 key 不为空且唯一
            studentDTO.setId(UUID.randomUUID().toString().replace("-", ""));
        }

        public SchedulerStudentDTO getStudentDTO() {
            return studentDTO.getName() != null ? studentDTO : null;
        }

        @Override
        public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
            int rowIndex = context.readRowHolder().getRowIndex();

            // 1. 解析表头信息 (前5行内寻找)
            if (rowIndex < 5) {
                parseHeaderInfo(rowData);
            }

            // 2. 寻找锚点 "星期一"
            if (mondayColIndex == null) {
                for (Map.Entry<Integer, String> entry : rowData.entrySet()) {
                    String val = entry.getValue();
                    if (val != null && val.contains("星期一")) {
                        mondayColIndex = entry.getKey();
                        dataStartRowIndex = rowIndex + 1;
                        return;
                    }
                }
            }

            // 3. 解析课程数据
            if (mondayColIndex != null && rowIndex >= dataStartRowIndex && rowIndex < dataStartRowIndex + 10) {
                parseScheduleRow(rowData, rowIndex);
            }
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        /**
         * 解析第三行的学生元数据
         */
        private void parseHeaderInfo(Map<Integer, String> rowData) {
            StringBuilder sb = new StringBuilder();
            for (String val : rowData.values()) {
                if (val != null) sb.append(val).append(" ");
            }
            String rawText = sb.toString();

            if (studentDTO.getName() == null) {
                Matcher nameM = NAME_PATTERN.matcher(rawText);
                if (nameM.find()) studentDTO.setName(nameM.group(1).trim());
            }
            if (studentDTO.getGrade() == null) {
                Matcher gradeM = GRADE_PATTERN.matcher(rawText);
                if (gradeM.find()) studentDTO.setGrade(gradeM.group(1).trim());
            }
            if (studentDTO.getCollege() == null) {
                Matcher collegeM = COLLEGE_PATTERN.matcher(rawText);
                if (collegeM.find()) studentDTO.setCollege(collegeM.group(1).trim());
            }
            if (studentDTO.getMajor() == null) {
                Matcher majorM = MAJOR_PATTERN.matcher(rawText);
                if (majorM.find()) studentDTO.setMajor(majorM.group(1).trim());
            }
        }

        private void parseScheduleRow(Map<Integer, String> rowData, int rowIndex) {
            int slot = rowIndex - dataStartRowIndex + 1;

            for (int dayOffset = 0; dayOffset < 7; dayOffset++) {
                int colIndex = mondayColIndex + dayOffset;
                String cellContent = rowData.get(colIndex);

                // 解析单元格内容
                List<SchedulerRawItemDTO> items = parseCellContent(cellContent, dayOffset + 1, slot);

                // 将解析出的课程项添加到 DTO
                for (SchedulerRawItemDTO item : items) {
                    studentDTO.addScheduleItem(item);
                }
            }
        }

        /**
         * 【核心解析逻辑更新】
         * 同步 CourseService 的解析规则：
         * 1. 支持用 "……" 分隔多门课
         * 2. 第一行：课程名
         * 3. 第二行：教师 + 周次【xx周】
         * 4. 第三行：地点
         */
        private List<SchedulerRawItemDTO> parseCellContent(String content, int day, int slot) {
            List<SchedulerRawItemDTO> result = new ArrayList<>();
            if (content == null || content.trim().length() <= 1) return result;
            if (content.contains("无课") || content.contains("时间段空闲")) return result;

            // 1. 切割课程 (兼容不同长度的省略号)
            String[] courseBlocks = content.split("…{2,}");

            for (String block : courseBlocks) {
                if (block.trim().isEmpty()) continue;

                // 2. 按行切割，过滤空行
                String[] rawLines = block.trim().split("\n");
                List<String> lines = new ArrayList<>();
                for (String l : rawLines) {
                    if (l.trim().length() > 0) lines.add(l.trim());
                }

                if (lines.isEmpty()) continue;

                SchedulerRawItemDTO item = new SchedulerRawItemDTO();
                item.setDay(day);
                item.setSection(slot);

                // Line 1: 课程名
                item.setCourseName(lines.get(0));

                // Line 2: 教师 + 周次
                if (lines.size() > 1) {
                    String line2 = lines.get(1);
                    // 正则：匹配 方括号内的内容
                    // 示例：杜洪霞【2-6,8周】
                    Pattern p = Pattern.compile("^(.*?)[【\\[](.*?)[】\\]]$");
                    Matcher m = p.matcher(line2);

                    if (m.find()) {
                        // m.group(1) 是教师名，SchedulerRawItemDTO 暂不需要存教师名，这里忽略
                        String weekStrRaw = m.group(2).trim();
                        // 移除 "周" 字，准备交给 expandWeeks 解析
                        String weekStrClean = weekStrRaw.replaceAll("周", "");
                        item.setBusyWeeks(expandWeeks(weekStrClean));
                    } else {
                        // 没匹配到，兜底策略 (设为空)
                        item.setBusyWeeks(new ArrayList<>());
                    }
                } else {
                    item.setBusyWeeks(new ArrayList<>());
                }

                // Line 3: 地点 (如有)
                if (lines.size() > 2) {
                    item.setLocation(lines.get(2));
                }

                // 只有当解析出有效的忙碌周次（或者至少有课程名）时才添加
                result.add(item);
            }
            return result;
        }

        /**
         * 【核心周次解析更新】
         * 支持：逗号分隔、连字符范围、(单)/(双)周标记
         */
        private List<Integer> expandWeeks(String weekStr) {
            List<Integer> list = new ArrayList<>();
            if (weekStr == null || weekStr.isEmpty()) return list;

            try {
                // 1. 先按逗号切割
                String[] parts = weekStr.split("[,，]");
                for (String part : parts) {
                    part = part.trim();
                    if (part.isEmpty()) continue;

                    // 【新增】检测单双周标记
                    boolean isOdd = part.contains("(单)") || part.contains("（单）");
                    boolean isEven = part.contains("(双)") || part.contains("（双）");

                    // 【新增】清理非数字和连字符字符 (移除 (单), (双) 等)
                    String cleanPart = part.replaceAll("[\\(（][单双][\\)）]", "").trim();

                    if (cleanPart.contains("-")) {
                        // 处理范围 "3-5"
                        String[] range = cleanPart.split("-");
                        if (range.length >= 2) {
                            int start = Integer.parseInt(range[0].trim());
                            int end = Integer.parseInt(range[1].trim());
                            for (int i = start; i <= end; i++) {
                                // 过滤单双周
                                if (isOdd) {
                                    if (i % 2 != 0) list.add(i);
                                } else if (isEven) {
                                    if (i % 2 == 0) list.add(i);
                                } else {
                                    list.add(i);
                                }
                            }
                        }
                    } else {
                        // 处理单个数字 "8"
                        int val = Integer.parseInt(cleanPart.trim());
                        if (isOdd) {
                            if (val % 2 != 0) list.add(val);
                        } else if (isEven) {
                            if (val % 2 == 0) list.add(val);
                        } else {
                            list.add(val);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("周次解析异常: " + weekStr);
            }
            return list;
        }
    }

    public ScheduleResultDTO generateSchedule(AutoScheduleRequest request) {
        // 1. 获取对应的算法策略
        ScheduleStrategy strategy = strategyFactory.getStrategy(request.getStrategy());

        // 2. 执行算法
        return strategy.execute(
                request.getStudents(),
                request.getRequirements(),
                request.getMaxPerWeek()
        );
    }
}