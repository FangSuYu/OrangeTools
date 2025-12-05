package cn.orangetools.system.model.dto;

import lombok.Data;

/**
 * @author YuHeng
 * @project backend
 * @file RegisterDto
 * @date 2025/12/5 21:58
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class RegisterDto {
    // 这里的字段要和前端注册页面传过来的 JSON 字段一一对应
    private String username;
    private String password;

    // 学号是选填的，可以在注册时填，也可以不填
    private String studentId;

}
