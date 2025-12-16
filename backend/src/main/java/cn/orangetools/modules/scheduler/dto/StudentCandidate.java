package cn.orangetools.modules.scheduler.dto;

import lombok.Data;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file StudentCandidate
 * @date 2025/12/16 21:50
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class StudentCandidate {
    private String id;
    private String name;

    // 忙碌的时间段列表，格式如 ["1_1", "1_2", "2_5"]
    // 算法在排班时，必须避开这些时间点
    private List<String> busySlots;
}
