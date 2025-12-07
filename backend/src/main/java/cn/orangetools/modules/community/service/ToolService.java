package cn.orangetools.modules.community.service;

import cn.orangetools.modules.community.model.entity.Tool;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author YuHeng
 * @project backend
 * @file ToolService
 * @date 2025/12/7 14:22
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public interface ToolService extends IService<Tool> {
    void reportUsage(String code);
}
