package cn.orangetools.common.result;

import lombok.Data;

/**
 * @author YuHeng
 * @project backend
 * @file Result
 * @date 2025/12/5 21:56
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Data
public class Result<T> {
    private Integer code; // 状态码
    private String msg;   // 提示信息
    private T data;       // 数据载荷 (泛型)

    // 成功静态方法 (无数据)
    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "操作成功";
        return result;
    }

    // 成功静态方法 (带数据)
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 200;
        result.msg = "操作成功";
        result.data = data;
        return result;
    }

    // 失败静态方法
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 500; // 默认错误码
        result.msg = msg;
        return result;
    }
}
