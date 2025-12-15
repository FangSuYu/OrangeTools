package cn.orangetools.modules.scheduler.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file SchedulerStudentDTO
 * @date 2025/12/15 17:55
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
/**
 * 排班助手 - 学生完整数据 DTO
 * <p>
 * 这是后端解析 Excel 后返回给前端的核心对象。
 * 前端收到后，应存入 Pinia 进行持久化。
 * </p>
 *
 * @author YuHeng
 */
@Data
public class SchedulerStudentDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 唯一标识 ID
     * (由于 Excel 中可能无学号，由后端自动生成 UUID，确保前端 v-for 循环不冲突)
     */
    private String id;

    /**
     * 姓名 (如 "方苏渝")
     */
    private String name;

    /**
     * 年级 (如 "2023")
     * 用于前端筛选
     */
    private String grade;

    /**
     * 院系 (如 "人工智能与大数据学院")
     * 用于前端筛选
     */
    private String college;

    /**
     * 专业 (如 "大数据技术")
     * 用于前端筛选
     */
    private String major;

    /**
     * 原始课表数据列表
     */
    private List<SchedulerRawItemDTO> scheduleRaw;

    public SchedulerStudentDTO() {
        this.scheduleRaw = new ArrayList<>();
    }

    public void addScheduleItem(SchedulerRawItemDTO item) {
        if (this.scheduleRaw == null) {
            this.scheduleRaw = new ArrayList<>();
        }
        this.scheduleRaw.add(item);
    }
}
