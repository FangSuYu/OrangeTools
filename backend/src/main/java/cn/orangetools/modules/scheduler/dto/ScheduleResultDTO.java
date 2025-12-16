package cn.orangetools.modules.scheduler.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author YuHeng
 * @project backend
 * @file ScheduleResultDTO
 * @date 2025/12/16 21:50
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class ScheduleResultDTO {
    // 核心方案: Key="1_0", Value=[StudentCandidate, ...]
    private Map<String, List<StudentCandidate>> solution;

    // 警告信息列表
    private List<String> warnings;

    // 总需求人数 (用于计算覆盖率)
    private Integer totalNeeds;
}
