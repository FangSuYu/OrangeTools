package cn.orangetools.system.model.dto;

import lombok.Data;
/**
 * @author YuHeng
 * @project backend
 * @file AuthController
 * @date 2025/12/13 9:56
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class PasswordResetDto {
    private String email;
    private String code;
    private String newPassword;
}
