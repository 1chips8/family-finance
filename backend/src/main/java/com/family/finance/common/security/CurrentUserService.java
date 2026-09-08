package com.family.finance.common.security;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.domain.UserStatus;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.common.error.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** 将 Spring Security 身份转换为业务身份，并集中执行“已登录—已入户—家长”的分层校验。 */
@Service
public class CurrentUserService {
    private final AppUserMapper userMapper;

    public CurrentUserService(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public CurrentUser requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "请先登录");
        }
        AppUser user = userMapper.selectByUsername(authentication.getName());
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_INACTIVE", "账号已停用或不存在");
        }
        return new CurrentUser(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getHouseholdId(), user.getMemberNo(), user.getRole(), user.getStatus());
    }

    public CurrentUser requireHouseholdUser() {
        CurrentUser user = requireUser();
        if (!user.hasHousehold()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "HOUSEHOLD_REQUIRED", "请先创建或加入家庭");
        }
        return user;
    }

    public CurrentUser requireParent() {
        CurrentUser user = requireHouseholdUser();
        if (!user.isParent()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PARENT_REQUIRED", "仅家长可以执行此操作");
        }
        return user;
    }
}
