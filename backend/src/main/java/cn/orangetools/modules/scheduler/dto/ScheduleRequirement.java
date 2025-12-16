package cn.orangetools.modules.scheduler.dto;

import lombok.Data;

/**
 * @author YuHeng
 * @project backend
 * @file ScheduleRequirement
 * @date 2025/12/16 21:49
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class ScheduleRequirement {
    private Integer day;     // 1-7
    private Integer section; // 0, 1-13
    private Integer count;   // 需要几个人

    // 辅助方法：生成唯一Key
    public String getKey() {
        return day + "_" + section;
    }
}
