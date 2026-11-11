package com.learning.silveragepass.security.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.learning.silveragepass.entity.AdminAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class AdminUserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String phone;
    private String realName;

    @JsonIgnore
    private String password;

    private Boolean enabled;
    private Collection<? extends GrantedAuthority> authorities;

    public AdminUserDetailsImpl(Long id, String username, String phone, String realName, String password,
            Boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.phone = phone;
        this.realName = realName;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
    }

    public static AdminUserDetailsImpl build(AdminAccount admin) {
        // 管理员角色：所有管理员都拥有ROLE_ADMIN，同时根据级别添加具体角色
        // 这样可以确保所有管理员都能访问需要ADMIN权限的接口
        // 同时可以通过具体级别（如SUPER_ADMIN）实现更细粒度的权限控制
        List<GrantedAuthority> authorities = new java.util.ArrayList<>();
        
        // 所有管理员都有基础的ADMIN角色
        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        // 如果是超级管理员，额外添加SUPER_ADMIN角色
        if (admin.getLevel() == AdminAccount.AdminLevel.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }

        return new AdminUserDetailsImpl(
                admin.getId(),
                admin.getUsername(),
                admin.getPhone(),
                admin.getRealName(),
                admin.getPassword(),
                admin.getEnabled() != null ? admin.getEnabled() : true,
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public Long getId() {
        return id;
    }

    public String getPhone() {
        return phone;
    }

    public String getRealName() {
        return realName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AdminUserDetailsImpl admin = (AdminUserDetailsImpl) o;
        return Objects.equals(id, admin.id);
    }
}
