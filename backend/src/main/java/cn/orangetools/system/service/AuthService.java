package cn.orangetools.system.service;

import cn.orangetools.system.entity.User;
import cn.orangetools.system.model.dto.EmailLoginDto;
import cn.orangetools.system.model.dto.LoginDto;
import cn.orangetools.system.model.dto.PasswordResetDto;
import cn.orangetools.system.model.dto.RegisterDto;

/**
 * @author YuHeng
 * @project backend
 * @file AuthService
 * @date 2025/12/5 22:02
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public interface AuthService {
    /**
     * 用户注册
     * @param registerDto 前端传来的注册信息
     * @return 注册成功的用户名
     */
    String register(RegisterDto registerDto);

    /**
     * 用户登录
     * @param loginDto 登录信息
     * @return JWT Token 字符串
     */
    String login(LoginDto loginDto);

    /**
     * 获取用户的信息
     * @return 用户的信息
     */
    User getUserByUsername(String username);

    /**
     * 发送验证码
     * @param email 邮箱
     * @param type 类型 (REGISTER, LOGIN, RESET)
     */
    void sendCode(String email, String type);

    /**
     * 邮箱验证码登录
     * @param loginDto 登录信息
     * @return JWT Token
     */
    String loginEmail(EmailLoginDto loginDto);

    /**
     * 重置密码
     * @param resetDto 重置信息
     */
    void resetPassword(PasswordResetDto resetDto);
}
