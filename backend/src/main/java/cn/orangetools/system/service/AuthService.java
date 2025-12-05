package cn.orangetools.system.service;

import cn.orangetools.system.model.dto.LoginDto;
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
}
