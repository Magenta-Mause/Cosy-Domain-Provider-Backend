package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.AdminUserUpdateDto;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.magentamause.cosydomainprovider.services.billing.StripeService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final PasswordEncoder passwordEncoder;
    private final SubdomainService subdomainService;
    private final SubdomainProperties subdomainProperties;
    private final StripeService stripeService;

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

    @Transactional
    public void deleteUserByUuid(String uuid) {
        UserEntity user = getUserByUuid(uuid);
        stripeService.cancelSubscription(user);
        oAuthIdentityRepository.deleteAllByUser_Uuid(uuid);
        subdomainService.deleteSubdomainsByOwner(uuid);
        userRepository.deleteById(uuid);
    }

    public UserEntity updateUser(UpdateUserDto dto, UserEntity user) {
        if (dto.getNewUsername() != null) {
            user.setUsername(dto.getNewUsername());
        }
        if (dto.getNewPassword() != null) {
            if (dto.getCurrentPassword() == null
                    || !passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        }
        return userRepository.save(user);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
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
                        .build();
        return userRepository.save(user);
    }

    public void setPassword(String uuid, String plainPassword) {
        UserEntity user = getUserByUuid(uuid);
        if (!user.isNeedsPasswordSetup() || user.getPasswordHash() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Password has already been set for this account");
        }
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        user.setNeedsPasswordSetup(false);
        userRepository.save(user);
    }

    public int computeMaxSubdomainCount(UserEntity user) {
        return user.computeMaxSubdomainCount(
                subdomainProperties.getMaxPerFreeUser(), subdomainProperties.getMaxPerPlusUser());
    }

    public UserEntity adminUpdateUser(String uuid, AdminUserUpdateDto dto) {
        UserEntity user = getUserByUuid(uuid);
        if (dto.getUsername() != null) {
            user.setUsername(dto.getUsername());
        }
        if (dto.getEmail() != null) {
            boolean takenByOther =
                    userRepository.existsByEmailIgnoreCase(dto.getEmail())
                            && !dto.getEmail().equalsIgnoreCase(user.getEmail());
            if (takenByOther) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            user.setEmail(dto.getEmail());
        }
        return userRepository.save(user);
    }

    public UserEntity adminSetMaxSubdomainOverride(String uuid, Integer value) {
        UserEntity user = getUserByUuid(uuid);
        user.setMaxSubdomainCountOverride(value);
        return userRepository.save(user);
    }
}
