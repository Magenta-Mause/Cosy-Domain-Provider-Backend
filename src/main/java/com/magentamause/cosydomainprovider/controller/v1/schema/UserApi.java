package com.magentamause.cosydomainprovider.controller.v1.schema;

import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "User", description = "User management (admin)")
@RequestMapping("/v1/user")
public interface UserApi {

    @Operation(summary = "List all users")
    @ApiResponse(responseCode = "200", description = "User list returned")
    @GetMapping
    ResponseEntity<List<UserDto>> getAllUsers();

    @Operation(summary = "Create a new user and send verification email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User created"),
        @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PostMapping
    ResponseEntity<UserDto> createUser(@RequestBody @Valid UserCreationDto userCreationDto);
}
