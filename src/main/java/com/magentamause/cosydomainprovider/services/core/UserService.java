package com.magentamause.cosydomainprovider.services.core;

import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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

    public UserEntity getUserByUsername(String username) {
        return userRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(
                        () ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "User with username " + username + " not found"));
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
        UserEntity user =
                UserEntity.builder()
                        .username(dto.getUsername())
                        .email(dto.getEmail())
                        .passwordHash(passwordEncoder.encode(dto.getPassword()))
                        .build();
        return userRepository.save(user);
    }
}
