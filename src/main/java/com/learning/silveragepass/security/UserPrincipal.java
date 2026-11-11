package com.learning.silveragepass.security;

import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.security.services.UserDetailsImpl;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * UserPrincipal 是 UserDetailsImpl 的别名，用于控制器中获取当前登录用户
 */
public class UserPrincipal extends UserDetailsImpl {

    private static final long serialVersionUID = 1L;

    public UserPrincipal(Long id, String username, String phone, String realName, String password,
            Boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        super(id, username, phone, realName, password, enabled, authorities);
    }

    public static UserPrincipal build(User user) {
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);
        return new UserPrincipal(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getPhone(),
                userDetails.getRealName(),
                userDetails.getPassword(),
                userDetails.isEnabled(),
                userDetails.getAuthorities());
    }
}