package cn.orangetools.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author YuHeng
 * @project backend
 * @file SecurityConfig
 * @date 2025/12/5 20:15
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Configuration // 这是一个配置类
@EnableWebSecurity // 开启 Security 可以在这里自定义
public class SecurityConfig {

    // 注入过滤器
    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    // 1. 定义“密码加密器” Bean
    // 为什么要这个？因为登录时，Security 拿到用户输入的明文密码 "123456"，
    // 需要用这个加密器加密成 "$2a$10$..."，然后再去跟数据库里的密码比对。
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 2. 定义“认证管理器” Bean
    // 这个 Bean 以后在 LoginController 里要用，用来手动触发登录验证流程。
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    // 3. 定义“安全过滤器链” (SecurityFilterChain) —— 这是防火墙的核心规则
    // Spring Boot 3.x 推荐这种 Lambda 写法，非常清爽
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 3.1 关闭 CSRF (跨站请求伪造)
                // 因为我们使用 JWT，不使用 Cookie 存 Session，所以不需要防 CSRF。
                // 如果不关，POST 请求会被拦截。
                .csrf(csrf -> csrf.disable())

                // 3.2 设置 Session 管理策略为“无状态” (Stateless)
                // 这意味着服务器不存 Session，每次请求都要带 Token。
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3.3 请求权限配置
                .authorizeHttpRequests(auth -> auth
                        // 下面这些接口，允许匿名访问（不仅是登录，还有Swagger文档等）
                        .requestMatchers("/api/auth/**", "/error").permitAll()
                        // 【新增】允许匿名访问课表分析接口
                        .requestMatchers("/api/tools/course/analyze").permitAll()
                        // 禁止用户匿名提交需求和反馈
                        .requestMatchers("/api/community/feedback/submit").authenticated()
                        // 允许所有人查看统计、看墙、点赞
                        .requestMatchers("/api/community/**").permitAll()
                        // 其他所有请求，都必须登录后才能访问
                        .anyRequest().authenticated()
                ).addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
