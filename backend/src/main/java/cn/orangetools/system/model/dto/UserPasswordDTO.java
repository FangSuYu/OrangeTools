package cn.orangetools.system.model.dto;

import lombok.Data;

/**
 * @author YuHeng
 * @project backend
 * @file UserPasswordDTO
 * @date 2025/12/7 19:20
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class UserPasswordDTO {
    private String oldPassword;
    private String newPassword;
}
