package com.magentamause.cosydomainprovider.services.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.magentamause.cosydomainprovider.configuration.subdomain.SubdomainProperties;
import com.magentamause.cosydomainprovider.entity.UserEntity;
import com.magentamause.cosydomainprovider.model.action.AdminUserUpdateDto;
import com.magentamause.cosydomainprovider.model.action.UpdateUserDto;
import com.magentamause.cosydomainprovider.model.action.UserCreationDto;
import com.magentamause.cosydomainprovider.model.core.Plan;
import com.magentamause.cosydomainprovider.model.exception.UserNotFoundException;
import com.magentamause.cosydomainprovider.repository.OAuthIdentityRepository;
import com.magentamause.cosydomainprovider.repository.UserRepository;
import com.magentamause.cosydomainprovider.services.billing.StripeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OAuthIdentityRepository oAuthIdentityRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SubdomainService subdomainService;
    @Mock private SubdomainProperties subdomainProperties;
    @Mock private StripeService stripeService;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService =
                new UserService(
                        userRepository,
                        oAuthIdentityRepository,
                        passwordEncoder,
                        subdomainService,
                        subdomainProperties,
                        stripeService);
    }

    private UserEntity user(String uuid) {
        return UserEntity.builder().uuid(uuid).username("alice").email("alice@example.com").build();
    }

    @Test
    void getUserByUuid_found() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        assertThat(userService.getUserByUuid("u1")).isSameAs(u);
    }

    @Test
    void getUserByUuid_notFound() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserByUuid("missing"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void getOptionalUserByUuid_present() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        assertThat(userService.getOptionalUserByUuid("u1")).contains(u);
    }

    @Test
    void getOptionalUserByUuid_empty() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThat(userService.getOptionalUserByUuid("x")).isEmpty();
    }

    @Test
    void getUserByEmail_found() {
        UserEntity u = user("u1");
        when(userRepository.findByEmailIgnoreCase("alice@example.com")).thenReturn(Optional.of(u));
        assertThat(userService.getUserByEmail("alice@example.com")).isSameAs(u);
    }

    @Test
    void getUserByEmail_notFound() {
        when(userRepository.findByEmailIgnoreCase("x@y.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.getUserByEmail("x@y.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void deleteUserByUuid_deletesEverything() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        userService.deleteUserByUuid("u1");
        verify(stripeService).cancelSubscription(u);
        verify(oAuthIdentityRepository).deleteAllByUser_Uuid("u1");
        verify(subdomainService).deleteSubdomainsByOwner("u1");
        verify(userRepository).deleteById("u1");
    }

    @Test
    void getAllUsers_returnsList() {
        List<UserEntity> list = List.of(user("u1"), user("u2"));
        when(userRepository.findAll()).thenReturn(list);
        assertThat(userService.getAllUsers()).hasSize(2);
    }

    @Test
    void createUser_success() {
        UserCreationDto dto =
                UserCreationDto.builder()
                        .username("bob")
                        .email("bob@example.com")
                        .password("pass")
                        .build();
        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("hashed");
        UserEntity saved = user("new");
        when(userRepository.save(any())).thenReturn(saved);

        UserEntity result = userService.createUser(dto);
        assertThat(result).isSameAs(saved);
    }

    @Test
    void createUser_emailConflict() {
        UserCreationDto dto =
                UserCreationDto.builder()
                        .username("bob")
                        .email("bob@example.com")
                        .password("pass")
                        .build();
        when(userRepository.existsByEmailIgnoreCase("bob@example.com")).thenReturn(true);
        assertThatThrownBy(() -> userService.createUser(dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void updateUser_updatesUsername() {
        UserEntity u = user("u1");
        UpdateUserDto dto = new UpdateUserDto();
        dto.setNewUsername("newname");
        when(userRepository.save(u)).thenReturn(u);

        UserEntity result = userService.updateUser(dto, u);
        assertThat(result.getUsername()).isEqualTo("newname");
    }

    @Test
    void updateUser_updatesPassword_correctCurrentPassword() {
        UserEntity u = user("u1");
        u.setPasswordHash("oldHash");
        UpdateUserDto dto = new UpdateUserDto();
        dto.setCurrentPassword("old");
        dto.setNewPassword("new");
        when(passwordEncoder.matches("old", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newHash");
        when(userRepository.save(u)).thenReturn(u);

        userService.updateUser(dto, u);
        assertThat(u.getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void updateUser_updatesPassword_wrongCurrentPassword() {
        UserEntity u = user("u1");
        u.setPasswordHash("oldHash");
        UpdateUserDto dto = new UpdateUserDto();
        dto.setCurrentPassword("wrong");
        dto.setNewPassword("new");
        when(passwordEncoder.matches("wrong", "oldHash")).thenReturn(false);

        assertThatThrownBy(() -> userService.updateUser(dto, u))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void updateUser_updatesPassword_nullCurrentPassword() {
        UserEntity u = user("u1");
        u.setPasswordHash("oldHash");
        UpdateUserDto dto = new UpdateUserDto();
        dto.setCurrentPassword(null);
        dto.setNewPassword("new");

        assertThatThrownBy(() -> userService.updateUser(dto, u))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    void setPassword_success() {
        UserEntity u = user("u1");
        u.setNeedsPasswordSetup(true);
        u.setPasswordHash(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newpass")).thenReturn("hash");

        userService.setPassword("u1", "newpass");
        assertThat(u.getPasswordHash()).isEqualTo("hash");
        assertThat(u.isNeedsPasswordSetup()).isFalse();
    }

    @Test
    void setPassword_alreadySet_throwsConflict() {
        UserEntity u = user("u1");
        u.setNeedsPasswordSetup(false);
        u.setPasswordHash("existing");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> userService.setPassword("u1", "new"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void setPassword_needsSetupFalseButHashNull_throwsConflict() {
        UserEntity u = user("u1");
        u.setNeedsPasswordSetup(false);
        u.setPasswordHash(null);
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));

        assertThatThrownBy(() -> userService.setPassword("u1", "new"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void computeMaxSubdomainCount_delegatesToEntity() {
        UserEntity u = UserEntity.builder().uuid("u1").username("x").email("x@x.com").build();
        when(subdomainProperties.getMaxPerFreeUser()).thenReturn(1);
        when(subdomainProperties.getMaxPerPlusUser()).thenReturn(5);
        assertThat(userService.computeMaxSubdomainCount(u)).isEqualTo(1);
    }

    @Test
    void adminUpdateUser_updatesUsernameAndEmail() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.save(u)).thenReturn(u);

        AdminUserUpdateDto dto =
                AdminUserUpdateDto.builder()
                        .username("newname")
                        .email("new@example.com")
                        .build();
        UserEntity result = userService.adminUpdateUser("u1", dto);
        assertThat(result.getUsername()).isEqualTo("newname");
        assertThat(result.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    void adminUpdateUser_emailConflict() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);

        AdminUserUpdateDto dto = AdminUserUpdateDto.builder().email("taken@example.com").build();
        assertThatThrownBy(() -> userService.adminUpdateUser("u1", dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e ->
                                assertThat(((ResponseStatusException) e).getStatusCode())
                                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void adminUpdateUser_sameEmail_noConflict() {
        UserEntity u = user("u1");
        u.setEmail("alice@example.com");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.existsByEmailIgnoreCase("alice@example.com")).thenReturn(true);
        when(userRepository.save(u)).thenReturn(u);

        AdminUserUpdateDto dto = AdminUserUpdateDto.builder().email("alice@example.com").build();
        assertThatNoException().isThrownBy(() -> userService.adminUpdateUser("u1", dto));
    }

    @Test
    void adminSetMaxSubdomainOverride_setsValue() {
        UserEntity u = user("u1");
        when(userRepository.findById("u1")).thenReturn(Optional.of(u));
        when(userRepository.save(u)).thenReturn(u);

        userService.adminSetMaxSubdomainOverride("u1", 10);
        assertThat(u.getMaxSubdomainCountOverride()).isEqualTo(10);
    }
}
