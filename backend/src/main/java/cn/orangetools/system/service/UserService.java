package cn.orangetools.system.service;

import cn.orangetools.system.entity.User;
import cn.orangetools.system.model.dto.UserPasswordDTO;
import cn.orangetools.system.model.dto.UserProfileDTO;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author YuHeng
 * @project backend
 * @file UserService
 * @date 2025/12/5 20:08
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public interface UserService extends IService<User> {
    void updateProfile(Long userId, UserProfileDTO dto);
    void updatePassword(Long userId, UserPasswordDTO dto);
}
