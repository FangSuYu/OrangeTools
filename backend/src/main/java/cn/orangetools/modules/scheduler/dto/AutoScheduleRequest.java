package cn.orangetools.modules.scheduler.dto;

import lombok.Data;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file AutoScheduleRequest
 * @date 2025/12/16 21:49
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class AutoScheduleRequest {
    // 策略名称: "GREEDY_BALANCE", "CONSECUTIVE", "RANDOM"
    private String strategy;

    // 全局约束
    private Integer maxPerWeek;   // 每人每周最大班次
    private Integer defaultCount; // 默认人数 (备用)

    // 需求明细列表
    private List<ScheduleRequirement> requirements;

    // 学生候选人池
    private List<StudentCandidate> students;
}
