package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api/v1/user")
public interface UserApi {
    @PostMapping
    ResponseEntity<UserDto> createUser(@RequestBody @Valid UserCreationDto userCreationDto);

    @PatchMapping
    ResponseEntity<UserDto> updateUser(@RequestBody @Valid UpdateUserDto dto);

    @DeleteMapping
    ResponseEntity<Void> deleteUser();
}
