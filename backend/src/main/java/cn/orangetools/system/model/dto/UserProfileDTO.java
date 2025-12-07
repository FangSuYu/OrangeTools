package cn.orangetools.system.model.dto;

import lombok.Data;

/**
 * @author YuHeng
 * @project backend
 * @file UserProfileDTO
 * @date 2025/12/7 19:19
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class UserProfileDTO {
    private String nickname;
    private String phone;
    private String email;
    private String avatar; // 存图片文件名，如 "avatar-1.png"
    private String studentId;
}
