package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.magentamause.cosydomainprovider.services.notification.MessagingService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessagingService messagingService;

    public void initiatePasswordReset(String email) {
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return;
        }
        UserEntity user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        userRepository.save(user);
        messagingService.sendPasswordResetEmail(user, resetToken);
    }

    public void confirmPasswordReset(String token, String newPassword) {
        UserEntity user =
                userRepository
                        .findByPasswordResetToken(token)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "Invalid or expired reset token"));
        if (user.getPasswordResetExpiresAt() == null
                || Instant.now().isAfter(user.getPasswordResetExpiresAt())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        userRepository.save(user);
    }
}
