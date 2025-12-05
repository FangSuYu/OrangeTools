package cn.orangetools.system.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.system.model.dto.RegisterDto;
import cn.orangetools.system.service.AuthService;
import lombok.RequiredArgsConstructor;
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
}