package com.magentamause.cosydomainprovider.controller.v1.impl;

import com.magentamause.cosydomainprovider.controller.v1.schema.UserApi;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import com.magentamause.cosydomainprovider.services.core.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class UserController implements UserApi {
    private final UserService userService;

    @Override
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers().stream().map(u -> u.toDto()).toList());
    }

    @Override
    public ResponseEntity<UserDto> createUser(UserCreationDto userCreationDto) {
        return ResponseEntity.ok(userService.createUser(userCreationDto.toEntity()).toDto());
    }
}
