package cn.orangetools.common.exception;

/**
 * @author YuHeng
 * @project backend
 * @file ServiceException
 * @date 2025/12/6 19:54
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */

import lombok.Getter;

/**
 * 自定义业务异常
 * 用于在 Service 层手动抛出，比如 "余额不足"、"学号已存在"
 */
@Getter
public class ServiceException extends RuntimeException {

    private Integer code;

    // 默认错误码 500
    public ServiceException(String message) {
        super(message);
        this.code = 500;
    }

    // 自定义错误码
    public ServiceException(String message, Integer code) {
        super(message);
        this.code = code;
    }
}
