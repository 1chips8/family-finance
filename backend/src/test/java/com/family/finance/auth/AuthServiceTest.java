package com.family.finance.auth;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.mapper.AppUserMapper;
import com.family.finance.auth.dto.RegisterRequest;
import com.family.finance.auth.service.AuthService;
import com.family.finance.common.error.ApiException;
import com.family.finance.common.security.CurrentUserService;
import com.family.finance.household.mapper.HouseholdMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock AppUserMapper userMapper;
    @Mock HouseholdMapper householdMapper;
    @Mock CurrentUserService currentUserService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    void rejectsDuplicateUsernameBeforeCreatingUser() {
        AppUser existing = new AppUser(); existing.setUsername("alice");
        when(userMapper.selectByUsername("alice")).thenReturn(existing);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("alice", "Alice", "Pass1234")))
                .isInstanceOf(ApiException.class)
                .hasMessage("用户名已存在");
        verify(userMapper, never()).insert(any(AppUser.class));
    }
}
