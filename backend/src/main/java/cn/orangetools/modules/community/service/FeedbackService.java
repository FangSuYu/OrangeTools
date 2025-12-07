package cn.orangetools.modules.community.service;

import cn.orangetools.modules.community.model.entity.Feedback;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author YuHeng
 * @project backend
 * @file FeedbackService
 * @date 2025/12/7 14:24
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public interface FeedbackService extends IService<Feedback> {
    void submit(Feedback feedback);
    // 去掉了参数，因为不需要排序和管理员判断了
    List<Feedback> getList();
}