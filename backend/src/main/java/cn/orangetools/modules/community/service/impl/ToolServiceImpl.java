package cn.orangetools.modules.community.service.impl;

import cn.orangetools.modules.community.mapper.ToolMapper;
import cn.orangetools.modules.community.model.entity.Tool;
import cn.orangetools.modules.community.service.ToolService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * @author YuHeng
 * @project backend
 * @file ToolServiceImpl
 * @date 2025/12/7 14:22
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
public class ToolServiceImpl extends ServiceImpl<ToolMapper, Tool> implements ToolService {
    @Override
    public void reportUsage(String code) {
        this.update(new LambdaUpdateWrapper<Tool>()
                .eq(Tool::getCode, code)
                .setSql("usage_count = usage_count + 1"));
    }
}
