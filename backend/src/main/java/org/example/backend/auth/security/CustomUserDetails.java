package org.example.backend.auth.security;

import org.example.backend.auth.entity.AccountStatus;
import org.example.backend.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
//이게 로그인한 사용자가 누구인지 표현할때 쓰는 인터페이스
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser(){return user;}


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != AccountStatus.SUSPENDED;
    } //생각을 깊게해보니 그냥 true로 놨두면 계정이 잠겼는데 안잠겼다고 하는게 돼서
    // 이렇게 설정해야 될거같음

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == AccountStatus.ACTIVE;
        //이것도 활성화 상태니까 이렇게 설정이 맞는거같음
    }
}
