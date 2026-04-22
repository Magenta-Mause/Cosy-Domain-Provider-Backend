package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.repository.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.magentamause.cosydomainprovider.services.notification.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final int ACCESS_TOKEN_LENGTH = 6;
    private static final String ACCESS_TOKEN_LETTERS = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessagingService messagingService;

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<UserEntity> getOptionalUserByUuid(String uuid) {
        return userRepository.findById(uuid);
    }

    public UserEntity getUserByUuid(String uuid) {
        return getOptionalUserByUuid(uuid)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User with id " + uuid + " not found"));
    }

    public UserEntity getUserByEmail(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User with email " + email + " not found"));
    }

    private UserEntity saveUser(UserEntity user) {
        return userRepository.save(user);
    }

    public void deleteUser(UserEntity user) {
        userRepository.delete(user);
    }

    public void patchUser(String uuid, UserEntity user) {
        UserEntity existingUser = getUserByUuid(uuid);
        if (user.getUsername() != null) {
            existingUser.setUsername(user.getUsername());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        saveUser(existingUser);
    }

    public UserEntity createUser(UserCreationDto dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        UserEntity user =
                UserEntity.builder()
                        .username(dto.getUsername())
                        .email(dto.getEmail())
                        .passwordHash(passwordEncoder.encode(dto.getPassword()))
                        .accessToken(generateAccessToken())
                        .build();

        messagingService.sendUserAccessToken(user);
        return userRepository.save(user);
    }

    public void resendVerificationCode(String uuid) {
        UserEntity user = getUserByUuid(uuid);
        if (user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already verified");
        }
        user.setAccessToken(generateAccessToken());
        saveUser(user);
        messagingService.sendUserAccessToken(user);
    }

    public void verifyUser(String uuid, String accessToken) {
        UserEntity user = getUserByUuid(uuid);
        if (!user.getAccessToken().equalsIgnoreCase(accessToken.replace("-", ""))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
        user.setVerified(true);
        saveUser(user);
    }

    public void initiatePasswordReset(String email) {
        Optional<UserEntity> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return;
        }
        UserEntity user = userOpt.get();
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        saveUser(user);
        messagingService.sendPasswordResetEmail(user, resetToken);
    }

    public void confirmPasswordReset(String token, String newPassword) {
        UserEntity user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token"));
        if (user.getPasswordResetExpiresAt() == null || Instant.now().isAfter(user.getPasswordResetExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid or expired reset token");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiresAt(null);
        saveUser(user);
    }

    private static String generateAccessToken() {
        return IntStream.range(0, ACCESS_TOKEN_LENGTH)
                .mapToObj(i -> String.valueOf(ACCESS_TOKEN_LETTERS.charAt(SECURE_RANDOM.nextInt(ACCESS_TOKEN_LETTERS.length()))))
                .collect(Collectors.joining());
    }
}
