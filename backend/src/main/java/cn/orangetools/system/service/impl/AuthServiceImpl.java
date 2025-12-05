package cn.orangetools.system.service.impl;

import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @author YuHeng
 * @project backend
 * @file AuthServiceImpl
 * @date 2025/12/5 22:02
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public String register(RegisterDto registerDto) {
        // 1. 检查用户名是否已存在
        // SELECT count(*) FROM sys_user WHERE username = 'xxx'
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDto.getUsername()));

        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 如果填了学号，也要检查学号是否重复
        if (registerDto.getStudentId() != null && !registerDto.getStudentId().isEmpty()) {
            Long stuCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, registerDto.getStudentId()));
            if (stuCount > 0) {
                throw new RuntimeException("该学号已被绑定");
            }
        }

        // 3. 构建 User 对象
        User user = new User();
        user.setUsername(registerDto.getUsername());
        user.setStudentId(registerDto.getStudentId());
        user.setNickname(registerDto.getUsername()); // 默认昵称等于用户名
        user.setRole("USER"); // 默认角色是普通用户
        user.setStatus(1); // 默认状态正常

        // 4. 【核心】密码加密
        // 这里的 passwordEncoder 就是我们在 SecurityConfig 里配置的 BCryptPasswordEncoder
        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());
        user.setPassword(encodedPassword);

        // 5. 存入数据库
        userMapper.insert(user);

        return user.getUsername();
    }
}
