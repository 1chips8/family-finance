package com.family.finance.common.security;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 每次已认证请求都重新确认账号仍处于启用状态。
 * 这样管理员停用成员后，成员已有的 Session 会立即失效，无需等到自然过期。
 */
@Component
public class InactiveSessionFilter extends OncePerRequestFilter {
    private final AppUserMapper userMapper;
    private final ObjectMapper objectMapper;

    public InactiveSessionFilter(AppUserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            AppUser user = userMapper.selectByUsername(authentication.getName());
            if (user == null || user.getStatus() != UserStatus.ACTIVE) {
                SecurityContextHolder.clearContext();
                if (request.getSession(false) != null) request.getSession(false).invalidate();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                objectMapper.writeValue(response.getWriter(), new ApiErrorResponse("ACCOUNT_INACTIVE", "账号已停用或不存在"));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
