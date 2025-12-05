package cn.orangetools.system.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.system.model.dto.LoginDto;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YuHeng
 * @project backend
 * @file AuthController
 * @date 2025/12/5 22:16
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 注册接口
     * @param registerDto
     * @return 注册成功的用户名
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDto registerDto) {
        try {
            String username = authService.register(registerDto);
            return Result.success(username);
        } catch (RuntimeException e) {
            // 如果 Service 层抛出了 "用户名已存在" 等异常，这里捕获并返回错误信息
            return Result.error(e.getMessage());
        }
    }

    /**
     * 登录接口
     * @param loginDto
     * @return 登录成功的token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDto loginDto) {
        try {
            // 尝试登录
            String token = authService.login(loginDto);
            // 登录成功
            return Result.success(token);
        } catch (BadCredentialsException e) {
            // 1. 捕获密码错误异常
            return Result.error("账号或密码错误");
        } catch (InternalAuthenticationServiceException e) {
            // 2. 捕获用户不存在异常 (有时候 Security 会抛这个)
            return Result.error("账号不存在");
        } catch (Exception e) {
            // 3. 捕获其他未知异常 (比如数据库挂了)
            e.printStackTrace(); // 打印到控制台方便调试
            return Result.error("登录失败，系统繁忙");
        }
    }

}