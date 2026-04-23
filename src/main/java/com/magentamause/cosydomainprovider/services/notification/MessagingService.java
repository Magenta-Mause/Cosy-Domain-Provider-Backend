package com.magentamause.cosydomainprovider.services.notification;

import com.magentamause.cosydomainprovider.entity.UserEntity;

public interface MessagingService {
    void sendUserAccessToken(UserEntity user);

    void sendPasswordResetEmail(UserEntity user, String resetToken);
}
