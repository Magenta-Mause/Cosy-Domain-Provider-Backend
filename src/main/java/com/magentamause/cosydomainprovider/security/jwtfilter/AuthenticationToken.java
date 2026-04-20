package com.magentamause.cosydomainprovider.security.jwtfilter;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Getter
@Setter
public class AuthenticationToken extends UsernamePasswordAuthenticationToken {
    private String userId;
    private UserEntity user;

    public AuthenticationToken(String userId, UserEntity user) {
        super(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.userId = userId;
        this.user = user;
    }
}
