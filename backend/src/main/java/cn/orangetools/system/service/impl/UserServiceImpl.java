package cn.orangetools.system.service.impl;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import cn.orangetools.system.model.dto.UserPasswordDTO;
import cn.orangetools.system.model.dto.UserProfileDTO;
import cn.orangetools.system.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author YuHeng
 * @project backend
 * @file UserServiceImpl
 * @date 2025/12/5 20:09
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        // 1. 唯一性校验：如果你填了学号，且这个学号被别人绑定了，报错
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            Long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, dto.getStudentId())
                    .ne(User::getId, userId)); // 排除自己
            if (count > 0) {
                throw new ServiceException("该学号已被其他账号绑定！");
            }
        }
        User user = new User();
        user.setId(userId);
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        this.updateById(user);
    }

    @Override
    public void updatePassword(Long userId, UserPasswordDTO dto) {
        User user = this.getById(userId);
        // 1. 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new ServiceException("旧密码错误");
        }
        // 2. 加密新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
    }

}
