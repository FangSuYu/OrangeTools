package cn.orangetools.modules.community.service.impl;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.modules.community.mapper.FeedbackMapper;
import cn.orangetools.modules.community.model.entity.Feedback;
import cn.orangetools.modules.community.service.FeedbackService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file FeedbackServiceImpl
 * @date 2025/12/7 14:23
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
public class FeedbackServiceImpl extends ServiceImpl<FeedbackMapper, Feedback> implements FeedbackService {

    @Override
    public void submit(Feedback feedback) {
        if (feedback.getTitle() == null || feedback.getContent() == null) {
            throw new ServiceException("标题和内容不能为空");
        }

        // 【核心修改】状态逻辑简化
        // 如果用户勾选了公开 (isPublicCheck=true)，则状态直接设为 1 (已公开)
        // 否则设为 0 (私密/仅后台可见)
        boolean isPublic = feedback.getIsPublicCheck() != null && feedback.getIsPublicCheck();
        feedback.setStatus(isPublic ? 1 : 0);

        // 初始化其他字段
        feedback.setLikeCount(0);
        feedback.setDislikeCount(0);
        feedback.setCreateTime(LocalDateTime.now());

        this.save(feedback);
    }

    @Override
    public List<Feedback> getList() {
        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        // 只查询状态为 1 (已公开) 的数据
        wrapper.eq(Feedback::getStatus, 1);
        // 固定按时间倒序 (最新发布的在前面)
        wrapper.orderByDesc(Feedback::getCreateTime);
        return this.list(wrapper);
    }
}