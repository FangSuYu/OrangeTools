package cn.orangetools.modules.scheduler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file SchedulerRawItemDTO
 * @date 2025/12/15 17:54
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
/**
 * 排班助手 - 原始课程单元 DTO
 * <p>
 * 对应 Excel 中解析出的每一个“课程块”。
 * 一个单元格如果包含多门课（用省略号分隔），会被拆分成多个此对象。
 * </p>
 *
 * @author YuHeng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerRawItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 星期几 (1=周一, 7=周日)
     */
    private Integer day;

    /**
     * 节次 (1-10)
     */
    private Integer section;

    /**
     * 课程名称
     * (新增字段：用于前端手动排班时，提示具体的冲突原因，如"高等数学")
     */
    private String courseName;

    /**
     * 上课地点
     * (可选字段：辅助决策，如"明华楼301")
     */
    private String location;

    /**
     * 忙碌的周次列表
     * (例如：[1, 2, 3, 4, 8, 9])
     * 前端根据此列表 + 用户选择的"当前周"，判断该时段是否冲突。
     */
    private List<Integer> busyWeeks;
}
