package cn.orangetools.system.controller;

import cn.orangetools.common.result.Result;
import cn.orangetools.system.entity.User;
import cn.orangetools.system.model.dto.UserPasswordDTO;
import cn.orangetools.system.model.dto.UserProfileDTO;
import cn.orangetools.system.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YuHeng
 * @project backend
 * @file UserController
 * @date 2025/12/7 19:21
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // 辅助方法：获取当前用户ID
    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        return user.getId();
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody UserProfileDTO dto) {
        Long userId = getCurrentUserId();
        log.info("user-请求更新个人资料，用户ID：【{}】，昵称：【{}】", userId, dto.getNickname());
        userService.updateProfile(userId, dto);
        return Result.success("资料更新成功");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody UserPasswordDTO dto) {
        Long userId = getCurrentUserId();
        log.info("user-请求修改密码，用户ID：【{}】", userId);
        userService.updatePassword(userId, dto);
        return Result.success("密码修改成功");
    }
}
