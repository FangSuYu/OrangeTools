package cn.orangetools.modules.course.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.modules.course.model.AnalysisResult;
import cn.orangetools.modules.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author YuHeng
 * @project backend
 * @file CourseController
 * @date 2025/12/6 22:32
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Slf4j
@RestController
@RequestMapping("/api/tools/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 批量上传课表进行分析
     * @param files 课表文件
     * @return 分析结果
     */
    @PostMapping("/analyze")
    public Result<AnalysisResult> analyze(@RequestParam("files") MultipartFile[] files) {
        log.info("开始分析课表...");
        // 核心逻辑交给 Service
        AnalysisResult result = courseService.analyze(files);
        log.info("课表分析完成，涉及总人数：{}", result.getTotalPeople());
        return Result.success(result);
    }
}
