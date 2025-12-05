package cn.orangetools.common.config;

import cn.orangetools.common.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * @author YuHeng
 * @project backend
 * @file JwtAuthenticationFilter
 * @date 2025/12/5 20:59
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    // 构造器注入
    public JwtAuthenticationFilter(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. 拿 Token：从请求头 Authorization 中取出
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 如果头为空，或者不是以 Bearer 开头，说明没带票，直接放行（让 Security 后面的过滤器去拦截它）
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 剪切 Token (去掉 "Bearer " 前缀)
        jwt = authHeader.substring(7);

        // 3. 解析 Token 里的用户名
        // 如果这里解析失败（比如 Token 是伪造的），jwtUtils 会抛出异常，这里没做捕获，全局异常处理会接住
        username = jwtUtils.extractUsername(jwt);

        // 4. 如果用户名存在，且当前上下文中还没认证过（防止重复认证）
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // 去数据库查这个人的详情
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 5. 验证 Token 是否合法
            if (jwtUtils.isTokenValid(jwt, userDetails.getUsername())) {
                // 生成“通过认证”的票据
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 【关键】把票据存入 Security 上下文，后续的 Controller 就能知道是谁登录了
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 继续下一个过滤器
        filterChain.doFilter(request, response);
    }

}
