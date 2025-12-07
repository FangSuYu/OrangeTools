package cn.orangetools.system.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.model.dto.LoginDto;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * @author YuHeng
 * @project backend
 * @file AuthController
 * @date 2025/12/5 22:16
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 注册接口
     * @param registerDto
     * @return 注册成功的用户名
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody RegisterDto registerDto) {
        log.info("auth-正在注册的用户：【{}】", registerDto.getUsername());
        String username = authService.register(registerDto);
        log.info("auth-注册成功，用户名：【{}】", username);
        return Result.success(username);
    }

    /**
     * 登录接口
     * @param loginDto
     * @return 登录成功的token
     */
    @PostMapping("/login")
    public Result<String> login(@RequestBody LoginDto loginDto) {
        log.info("auth-正在登录的用户：【{}】", loginDto.getUsername());
        String token = authService.login(loginDto);
        log.info("auth-登录成功，用户名：【{}】", loginDto.getUsername());
        return Result.success(token);
    }

    /**
     * 获取用户信息
     * @return 用户信息
     */
    @GetMapping("/info")
    public Result<User> getUserInfo() {
        String username = ((UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        User user = authService.getUserByUsername(username); // 需要你在 AuthService 加这个方法，或者直接调 Mapper
        user.setPassword(null); // 抹除密码
        return Result.success(user);
    }

}