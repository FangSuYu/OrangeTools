package cn.orangetools.common.exception;

import cn.orangetools.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author YuHeng
 * @project backend
 * @file GlobalExceptionHandler
 * @date 2025/12/6 19:54
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. 捕获所有未知的系统异常 (兜底方案)
     * 场景：NullPointerException, SQLSyntaxErrorException 等
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        // 打印详细堆栈到控制台 (方便我们开发调试)
        log.error("系统未知异常", e);

        // 告诉前端的只有一句温柔的谎言
        return Result.error("系统繁忙，请稍后再试");
    }

    /**
     * 2. 捕获我们自己抛出的业务异常 (ServiceException)
     * 场景：我们在代码里 throw new ServiceException("学号已存在");
     */
    @ExceptionHandler(ServiceException.class)
    public Result<?> handleServiceException(ServiceException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getMessage());
    }

    /**
     * 3. 捕获 Security 登录失败异常
     * 场景：密码输错、账号不存在
     */
    @ExceptionHandler({BadCredentialsException.class, InternalAuthenticationServiceException.class})
    public Result<?> handleSecurityException(Exception e) {
        log.warn("认证失败: {}", e.getMessage());
        return Result.error("账号或密码错误"); // 统一提示，防止黑客枚举账号
    }

}
