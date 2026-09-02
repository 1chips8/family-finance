package com.family.finance.auth.service;

import com.family.finance.auth.domain.AppUser;
import com.family.finance.auth.mapper.AppUserMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final AppUserMapper userMapper;

    public UserDetailsServiceImpl(AppUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser user = userMapper.selectByUsername(username);
        if (user == null) throw new UsernameNotFoundException("用户名或密码错误");
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(user.getStatus() != com.family.finance.auth.domain.UserStatus.ACTIVE)
                .authorities("ROLE_USER")
                .build();
    }
}
