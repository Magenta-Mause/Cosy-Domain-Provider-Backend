package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.magentamause.cosydomainprovider.services.notification.MessagingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private static final int ACCESS_TOKEN_LENGTH = 6;
    private static final String ACCESS_TOKEN_LETTERS = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final MessagingService messagingService;

    public void sendInitialVerification(UserEntity user) {
        user.setAccessToken(generateAccessToken());
        userRepository.save(user);
        messagingService.sendUserAccessToken(user);
    }

    public void resendVerificationCode(String uuid) {
        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + uuid + " not found"));
        if (user.isVerified()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already verified");
        }
        user.setAccessToken(generateAccessToken());
        userRepository.save(user);
        messagingService.sendUserAccessToken(user);
    }

    public void verifyUser(String uuid, String accessToken) {
        UserEntity user = userRepository.findById(uuid)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User with id " + uuid + " not found"));
        if (!user.getAccessToken().equalsIgnoreCase(accessToken.replace("-", ""))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token");
        }
        user.setVerified(true);
        userRepository.save(user);
    }

    private static String generateAccessToken() {
        return IntStream.range(0, ACCESS_TOKEN_LENGTH)
                .mapToObj(i -> String.valueOf(ACCESS_TOKEN_LETTERS.charAt(SECURE_RANDOM.nextInt(ACCESS_TOKEN_LETTERS.length()))))
                .collect(Collectors.joining());
    }
}
