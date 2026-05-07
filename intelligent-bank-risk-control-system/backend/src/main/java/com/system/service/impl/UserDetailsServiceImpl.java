package com.system.service.impl;

import com.system.domain.SysUser;
import com.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }

        List<String> codes = userMapper.selectRoleCodes(user.getId());
        boolean risk =
                codes.stream()
                        .anyMatch(
                                c ->
                                        "RISK_OFFICER".equalsIgnoreCase(c)
                                                || "RISK_MANAGER".equalsIgnoreCase(c));
        String grantedRole = risk ? "ROLE_RISK_OFFICER" : "ROLE_CUSTOMER";

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(grantedRole)))
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }
}
