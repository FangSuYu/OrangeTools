package cn.orangetools.system.service.impl;

import cn.orangetools.system.entity.User;
import cn.orangetools.system.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * @author YuHeng
 * @project backend
 * @file UserDetailsServiceImpl
 * @date 2025/12/5 20:05
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    // 构造器注入 UserMapper（比 @Autowired 更推荐）
    public UserDetailsServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 1. 去数据库查人
        // SQL: select * from sys_user where username = 'xxx'
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username));

        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }

        // 2. 开始翻译：把数据库查到的 user 变成 Spring Security 需要的 UserDetails
        // 这里使用的是 Spring Security 自带的 org.springframework.security.core.userdetails.User
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // 这里必须是数据库里的加密密码
                // 3. 权限列表：暂时给一个空列表，或者根据 user.getRole() 生成
                // 比如：AuthorityUtils.createAuthorityList(user.getRole())
                Collections.emptyList()
        );
    }
}