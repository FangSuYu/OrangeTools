package cn.orangetools.system.service.impl;

import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import cn.orangetools.system.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * @author YuHeng
 * @project backend
 * @file UserServiceImpl
 * @date 2025/12/5 20:09
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {}
