package cn.orangetools.modules.scheduler.controller;
import cn.orangetools.common.result.Result;
import cn.orangetools.modules.scheduler.dto.AutoScheduleRequest;
import cn.orangetools.modules.scheduler.dto.ScheduleResultDTO;
import cn.orangetools.modules.scheduler.dto.SchedulerStudentDTO;
import cn.orangetools.modules.scheduler.service.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file SchedulerController
 * @date 2025/12/15 18:16
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

/**
 * 橙子智能排班助手 - 控制层
 * <p>
 * 提供文件解析和排班计算接口。
 * </p>
 *
 * @author OrangeTools
 */
@Slf4j
@RestController
@RequestMapping("/api/tools/scheduler")
//@Tag(name = "智能排班助手", description = "提供课表解析与自动排班功能")
public class SchedulerController {

    @Autowired
    private SchedulerService schedulerService;

    /**
     * 1. 解析 Excel 课表文件
     * <p>
     * 前端上传一个或多个 Excel 文件，后端解析出包含元数据（年级/专业）和课程详情的 JSON。
     * 前端拿到数据后，应存入 Pinia 进行本地持久化，不入库。
     * </p>
     *
     * @param files 上传的 Excel 文件数组
     * @return 学生数据列表
     */
//    @Operation(summary = "解析上传的课表文件", description = "接收 .xls/.xlsx 文件，返回标准化的学生课表数据")
    @PostMapping("/parse")
    public Result<List<SchedulerStudentDTO>> parseFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            // 调用 Service 进行解析
            List<SchedulerStudentDTO> data = schedulerService.parseFiles(files);

            // 返回成功数据
            return Result.success(data);
        } catch (Exception e) {
            log.error("课表解析失败", e);
            // 返回具体的错误信息给前端提示
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/auto-generate")
    public Result autoGenerate(@RequestBody AutoScheduleRequest request) {
        // 简单的参数校验
        if (request.getRequirements() == null || request.getRequirements().isEmpty()) {
            return Result.error("排班需求不能为空");
        }
        if (request.getStudents() == null || request.getStudents().isEmpty()) {
            return Result.error("参与人员不能为空");
        }

        ScheduleResultDTO result = schedulerService.generateSchedule(request);
        return Result.success(result);
    }
}
