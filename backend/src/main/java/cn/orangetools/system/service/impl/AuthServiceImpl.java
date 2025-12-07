package cn.orangetools.system.service.impl;

import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.common.utils.JwtUtils;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import cn.orangetools.system.model.dto.LoginDto;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
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

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Override
    public String register(RegisterDto registerDto) {
        // 1. 检查用户名是否已存在
        // SELECT count(*) FROM sys_user WHERE username = 'xxx'
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, registerDto.getUsername()));

        if (count > 0) {
            throw new ServiceException("用户名已存在");
        }

        // 2. 如果填了学号，也要检查学号是否重复
        if (registerDto.getStudentId() != null && !registerDto.getStudentId().isEmpty()) {
            Long stuCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getStudentId, registerDto.getStudentId()));
            if (stuCount > 0) {
                throw new ServiceException("该学号已被绑定");
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

    @Override
    public String login(LoginDto loginDto) {
        // 1. 调用 Security 的认证管理器进行认证
        // 这行代码会自动调用 UserDetailsServiceImpl 查库，并比较密码
        // 如果密码错误，它会直接抛出 BadCredentialsException 异常
        authenticationManager.authenticate(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );

        // 2. 如果代码走到这里，说明认证成功了（没抛异常）
        // 生成 JWT 令牌
        String token = jwtUtils.generateToken(loginDto.getUsername());

        // 3. 返回 Token
        return token;
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }
}
