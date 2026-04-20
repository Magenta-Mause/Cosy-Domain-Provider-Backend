package com.magentamause.cosydomainprovider.services.auth;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.AuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SecurityContextService {

    public AuthenticationToken getAuthenticationToken() {
        Object auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AuthenticationToken)) {
            return null;
        }
        return (AuthenticationToken) auth;
    }

    public String getUserId() {
        AuthenticationToken token = getAuthenticationToken();
        return token == null ? null : token.getUserId();
    }

    public UserEntity getUser() {
        AuthenticationToken token = getAuthenticationToken();
        return token == null ? null : token.getUser();
    }
}
