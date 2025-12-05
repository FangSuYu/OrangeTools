package cn.orangetools.system.mapper;

import cn.orangetools.system.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author YuHeng
 * @project backend
 * @file UserMapper
 * @date 2025/12/5 19:59
 * @github https://github.com/YOUR_GITHUB_NAME/OrangeTools
 * @license GPL-3.0 License
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {}
