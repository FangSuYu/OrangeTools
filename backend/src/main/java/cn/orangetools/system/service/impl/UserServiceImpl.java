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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;

    @Override
    public void updateProfile(Long userId, UserProfileDTO dto) {
        log.info("开始更新用户资料，用户ID：{}", userId);
        // 1. 唯一性校验：如果你填了学号，且这个学号被别人绑定了，报错
        if (dto.getStudentId() != null && !dto.getStudentId().isEmpty()) {
            Long count = this.count(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, dto.getStudentId())
                    .ne(User::getId, userId)); // 排除自己
            if (count > 0) {
                log.warn("更新资料失败，学号已被占用，用户ID：{}，学号：{}", userId, dto.getStudentId());
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
        log.info("用户资料更新成功，用户ID：{}", userId);
    }

    @Override
    public void updatePassword(Long userId, UserPasswordDTO dto) {
        log.info("开始修改密码，用户ID：{}", userId);
        User user = this.getById(userId);
        // 1. 校验旧密码
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            log.warn("修改密码失败，旧密码错误，用户ID：{}", userId);
            throw new ServiceException("旧密码错误");
        }
        // 2. 加密新密码
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        this.updateById(user);
        log.info("密码修改成功，用户ID：{}", userId);
    }

}
