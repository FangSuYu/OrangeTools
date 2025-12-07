package cn.orangetools.modules.community.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author YuHeng
 * @project backend
 * @file Tool
 * @date 2025/12/7 14:02
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
@TableName("sys_tool")
public class Tool {
    private Long id;
    private String code;
    private String name;
    private Long usageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
