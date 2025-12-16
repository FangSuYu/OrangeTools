package cn.orangetools.modules.scheduler.service;

import cn.orangetools.modules.scheduler.dto.ScheduleRequirement;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.StudentCandidate;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file ScheduleStrategy
 * @date 2025/12/16 22:04
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public interface ScheduleStrategy {
    /**
     * 执行排班算法
     * @param students 所有人选
     * @param requirements 所有需求坑位
     * @param maxPerWeek 每人最大限制
     * @return 排班结果对象
     */
    ScheduleResultDTO execute(List<StudentCandidate> students, List<ScheduleRequirement> requirements, int maxPerWeek);
}
