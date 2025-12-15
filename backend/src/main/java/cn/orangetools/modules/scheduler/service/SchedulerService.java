package cn.orangetools.modules.scheduler.service;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.modules.scheduler.dto.SchedulerRawItemDTO;
import cn.orangetools.modules.scheduler.dto.SchedulerStudentDTO;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.extern.slf4j.Slf4j;
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

        // 正则：匹配连续2个及以上的中文省略号或点
        private static final Pattern SPLIT_PATTERN = Pattern.compile("[…。]{2,}");

        // --- 元数据正则 (严格匹配 Key[:：]Value 格式) ---
        // 匹配非空白字符，自动截断后续的空格
        private static final Pattern NAME_PATTERN = Pattern.compile("姓名[:：](\\S+)");
        private static final Pattern GRADE_PATTERN = Pattern.compile("年级[:：](\\S+)");
        private static final Pattern COLLEGE_PATTERN = Pattern.compile("院系[:：](\\S+)");
        private static final Pattern MAJOR_PATTERN = Pattern.compile("专业[:：](\\S+)");

        // 周次正则
        private static final Pattern WEEK_PATTERN = Pattern.compile("[【\\[](.*?)[周\\]】]");

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
                if (val != null) sb.append(val).append(" "); // 用空格连接所有单元格
            }
            String rawText = sb.toString();

            // 如果名字已经解析过了，就不重复解析了，避免覆盖
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

                if (cellContent == null || cellContent.trim().isEmpty()) {
                    continue;
                }

                List<SchedulerRawItemDTO> items = parseCellContent(cellContent, dayOffset + 1, slot);
                for (SchedulerRawItemDTO item : items) {
                    studentDTO.addScheduleItem(item);
                }
            }
        }

        private List<SchedulerRawItemDTO> parseCellContent(String content, int day, int slot) {
            List<SchedulerRawItemDTO> result = new ArrayList<>();
            String[] courseBlocks = SPLIT_PATTERN.split(content);

            for (String block : courseBlocks) {
                if (block.trim().isEmpty()) continue;

                SchedulerRawItemDTO item = new SchedulerRawItemDTO();
                item.setDay(day);
                item.setSection(slot);

                String[] lines = block.trim().split("\\r?\\n");

                if (lines.length > 0) {
                    item.setCourseName(lines[0].trim());
                }

                boolean weekFound = false;
                for (String line : lines) {
                    Matcher weekMatcher = WEEK_PATTERN.matcher(line);
                    if (weekMatcher.find()) {
                        String weekStr = weekMatcher.group(1);
                        item.setBusyWeeks(expandWeeks(weekStr));
                        weekFound = true;
                        break;
                    }
                }

                if (!weekFound) {
                    item.setBusyWeeks(expandWeeks("1-20"));
                }

                if (lines.length >= 3) {
                    item.setLocation(lines[lines.length - 1].trim());
                }

                result.add(item);
            }
            return result;
        }

        private List<Integer> expandWeeks(String weekStr) {
            List<Integer> list = new ArrayList<>();
            try {
                String[] parts = weekStr.replaceAll("，", ",").split(",");
                for (String part : parts) {
                    if (part.contains("-")) {
                        String[] range = part.split("-");
                        int start = Integer.parseInt(range[0]);
                        int end = Integer.parseInt(range[1]);
                        for (int i = start; i <= end; i++) list.add(i);
                    } else {
                        String num = part.replaceAll("\\D", "");
                        if(!num.isEmpty()) list.add(Integer.parseInt(num));
                    }
                }
            } catch (Exception e) {
                log.warn("周次解析警告: {}", weekStr);
            }
            return list;
        }
    }
}