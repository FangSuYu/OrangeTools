package cn.orangetools.modules.community.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.modules.community.model.entity.Feedback;
import cn.orangetools.modules.community.model.entity.Tool;
import cn.orangetools.modules.community.service.FeedbackService;
import cn.orangetools.modules.community.service.ToolService;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file CommunityController
 * @date 2025/12/7 14:05
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Slf4j
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final ToolService toolService;
    private final FeedbackService feedbackService;

    // ================== 工具统计 (保持不变) ==================

    @GetMapping("/tools/stats")
    public Result<List<Tool>> getToolStats() {
        return Result.success(toolService.list());
    }

    @PostMapping("/tools/report/{code}")
    public Result<String> reportUsage(@PathVariable String code) {
        toolService.reportUsage(code);
        return Result.success("ok");
    }

    // ================== 需求墙 (极简版) ==================

    @PostMapping("/feedback/submit")
    public Result<String> submitFeedback(@RequestBody Feedback feedback) {
        feedbackService.submit(feedback);
        return Result.success("提交成功！您的建议已上墙。");
    }

    @GetMapping("/feedback/list")
    public Result<List<Feedback>> getFeedbacks() {
        return Result.success(feedbackService.getList());
    }
}