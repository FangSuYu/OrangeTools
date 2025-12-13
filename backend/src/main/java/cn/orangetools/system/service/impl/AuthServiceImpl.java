package cn.orangetools.system.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.orangetools.common.exception.ServiceException;
import cn.orangetools.common.utils.JwtUtils;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import cn.orangetools.system.model.dto.EmailLoginDto;
import cn.orangetools.system.model.dto.LoginDto;
import cn.orangetools.system.model.dto.PasswordResetDto;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

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

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public String register(RegisterDto registerDto) {
        // 1. 检查用户名是否已存在
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

        // 4. 处理邮箱验证与绑定 (New)
        if (registerDto.getEmail() != null && !registerDto.getEmail().isEmpty()) {
            String codeKey = "auth:code:register:" + registerDto.getEmail();
            String cacheCode = redisTemplate.opsForValue().get(codeKey);
            if (cacheCode == null || !cacheCode.equals(registerDto.getCode())) {
                throw new ServiceException("邮箱验证码错误或已过期");
            }
            redisTemplate.delete(codeKey);

            // 检查邮箱是否已存在
            Long emailCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getEmail, registerDto.getEmail()));
            if (emailCount > 0) {
                throw new ServiceException("该邮箱已被注册");
            }
            user.setEmail(registerDto.getEmail());
        }

        // 5. 【核心】密码加密
        String encodedPassword = passwordEncoder.encode(registerDto.getPassword());
        user.setPassword(encodedPassword);

        // 6. 存入数据库
        userMapper.insert(user);

        return user.getUsername();
    }

    @Override
    public String login(LoginDto loginDto) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginDto.getUsername(),
                        loginDto.getPassword()
                )
        );
        return jwtUtils.generateToken(loginDto.getUsername());
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));
    }

    @Override
    public void sendCode(String email, String type) {
        if (email == null || !email.contains("@")) {
            throw new ServiceException("邮箱格式不正确");
        }

        // 校验逻辑
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if ("REGISTER".equalsIgnoreCase(type)) {
            if (count > 0) throw new ServiceException("该邮箱已被注册");
        } else if ("LOGIN".equalsIgnoreCase(type) || "RESET".equalsIgnoreCase(type)) {
            if (count == 0) throw new ServiceException("该邮箱未注册");
        } else {
            throw new ServiceException("未知的验证码类型");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);
        String key = "auth:code:" + type.toLowerCase() + ":" + email;
        redisTemplate.opsForValue().set(key, code, 5, TimeUnit.MINUTES);

        // 发送邮件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("OrangeTools 验证码");
            message.setText("【OrangeTools】您的验证码是：" + code + "，有效期5分钟。请勿泄露给他人。");
            mailSender.send(message);
        } catch (Exception e) {
            redisTemplate.delete(key);
            throw new ServiceException("邮件发送失败，请稍后重试");
        }
    }

    @Override
    public String loginEmail(EmailLoginDto loginDto) {
        String codeKey = "auth:code:login:" + loginDto.getEmail();
        String cacheCode = redisTemplate.opsForValue().get(codeKey);
        if (cacheCode == null || !cacheCode.equals(loginDto.getCode())) {
            throw new ServiceException("验证码错误或已过期");
        }
        redisTemplate.delete(codeKey);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, loginDto.getEmail()));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        return jwtUtils.generateToken(user.getUsername());
    }

    @Override
    public void resetPassword(PasswordResetDto resetDto) {
        String codeKey = "auth:code:reset:" + resetDto.getEmail();
        String cacheCode = redisTemplate.opsForValue().get(codeKey);
        if (cacheCode == null || !cacheCode.equals(resetDto.getCode())) {
            throw new ServiceException("验证码错误或已过期");
        }
        redisTemplate.delete(codeKey);

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, resetDto.getEmail()));
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        user.setPassword(passwordEncoder.encode(resetDto.getNewPassword()));
        userMapper.updateById(user);
    }

    // ================= 预留扩展接口 =================

    // public String loginPhone(String phone, String code) {
    //     // TODO: 手机号登录逻辑
    //     return null;
    // }

    // public String loginQQ(String openId) {
    //     // TODO: QQ登录逻辑
    //     return null;
    // }

    // public String loginWeChat(String openId) {
    //     // TODO: 微信登录逻辑
    //     return null;
    // }
}