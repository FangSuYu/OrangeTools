package cn.orangetools.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author YuHeng
 * @project backend
 * @file User
 * @date 2025/12/5 19:57
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
@TableName("sys_user")
public class User {

    // 指定主键策略：IdType.AUTO 代表数据库自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String studentId;
    private String nickname;
    private String avatar;
    private String phone;
    private String email;
    private String qqOpenId;
    private String wxOpenId;
    private String role;
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
