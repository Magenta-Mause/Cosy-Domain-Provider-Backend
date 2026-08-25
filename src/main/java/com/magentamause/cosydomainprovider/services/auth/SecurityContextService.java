package com.magentamause.cosydomainprovider.services.auth;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.security.jwtfilter.AuthenticationToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

    /**
     * Returns the authenticated user, failing with 401 instead of yielding {@code null}. Use this
     * from endpoints that are only reachable while authenticated; {@link #getUser()} stays
     * available for the few endpoints that legitimately work both signed in and signed out.
     */
    public UserEntity requireUser() {
        UserEntity user = getUser();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return user;
    }

    /** Returns the authenticated user's id, failing with 401 instead of yielding {@code null}. */
    public String requireUserId() {
        String userId = getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        return userId;
    }
}
