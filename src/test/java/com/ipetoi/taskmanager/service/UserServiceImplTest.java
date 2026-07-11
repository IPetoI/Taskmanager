package com.ipetoi.taskmanager.service;

import com.ipetoi.taskmanager.exception.DuplicateResourceException;
import com.ipetoi.taskmanager.exception.InvalidCredentialsException;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;
import com.ipetoi.taskmanager.repository.UserRepository;
import com.ipetoi.taskmanager.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static com.ipetoi.taskmanager.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    UserServiceImpl userService;

    // --- CreateUser ---

    @Test
    void registerShouldHashPasswordAndSaveUser() {
        when(userRepository.findByUsername("peto")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("asdasd")).thenReturn("hashed-asdasd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User user = userService.createUser("peto", "asdasd", "peto@example.com");

        assertEquals("peto", user.getUsername());
        assertEquals("hashed-asdasd", user.getPassword());
        assertEquals(UserRole.USER, user.getRole());
        verify(userRepository).save(any(User.class));
        verify(userRepository).findByUsername("peto");
        verify(passwordEncoder).encode("asdasd");
    }

    @Test
    void registerShouldRejectDuplicateUsername() {
        when(userRepository.findByUsername("peto")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser("peto", "asdasd", "peto@example.com"));

        verify(userRepository, never()).save(any());
    }

    @Test
    void registerShouldRejectDuplicateEmail() {
        when(userRepository.findByUsername("peto")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("peto@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(DuplicateResourceException.class,
                () -> userService.createUser("peto", "asdasd", "peto@example.com"));

        verify(userRepository, never()).save(any());
    }

    // --- Authenticate ---

    @Test
    void loginShouldValidatePassword() {
        User user = regularUser(1L, "peto", "peto@example.com");
        user.setPassword("hashed-asdasd");
        when(userRepository.findByUsername("peto")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("asdasd", "hashed-asdasd")).thenReturn(true);

        User result = userService.authenticate("peto", "asdasd");

        assertEquals(user, result);
        verify(passwordEncoder).matches("asdasd", "hashed-asdasd");
    }

    @Test
    void loginShouldRejectWrongPassword() {
        User user = regularUser(1L, "peto", "peto@example.com");
        user.setPassword("hashed-asdasd");
        when(userRepository.findByUsername("peto")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("rossz-jelszo", "hashed-asdasd")).thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> userService.authenticate("peto", "rossz-jelszo"));
    }

    @Test
    void loginShouldRejectUnknownUsername() {
        when(userRepository.findByUsername("ismeretlen")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> userService.authenticate("ismeretlen", "akarmi"));

        verify(passwordEncoder, never()).matches(any(), any());
    }

    // --- FindByUsername ---

    @Test
    void findByUsernameShouldReturnNullWhenMissing() {
        when(userRepository.findByUsername("ismeretlen")).thenReturn(Optional.empty());

        assertNull(userService.findByUsername("ismeretlen"));
    }

    // --- FindAllNonAdminUsers ---

    @Test
    void findAllNonAdminUsersShouldExcludeAdmins() {
        User admin = adminUser(1L);
        User regular = regularUser(2L, "peto", "peto@example.com");
        when(userRepository.findAll()).thenReturn(List.of(admin, regular));

        List<User> users = userService.findAllNonAdminUsers();

        assertEquals(1, users.size());
        assertEquals(UserRole.USER, users.get(0).getRole());
    }

    // --- EnsureAdminAccount ---

    @Test
    void ensureAdminAccountShouldCreateWhenMissing() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("admin")).thenReturn("hashed-admin");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.ensureAdminAccount("admin", "admin", "petoojozsef@gmail.com");

        assertEquals(UserRole.ADMIN, result.getRole());
        assertEquals("petoojozsef@gmail.com", result.getEmail());
        assertEquals("hashed-admin", result.getPassword());
    }

    @Test
    void ensureAdminAccountShouldFixDriftedEmailAndPassword() {
        User existing = adminUser(1L);
        existing.setEmail("old@example.com");
        existing.setPassword("old-hash");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("admin", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("admin")).thenReturn("new-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.ensureAdminAccount("admin", "admin", "petoojozsef@gmail.com");

        assertEquals("petoojozsef@gmail.com", result.getEmail());
        assertEquals("new-hash", result.getPassword());
        verify(userRepository).save(existing);
    }

    @Test
    void ensureAdminAccountShouldNotResaveWhenAlreadyUpToDate() {
        User existing = adminUser(1L);
        existing.setPassword("current-hash");
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("admin", "current-hash")).thenReturn(true);

        userService.ensureAdminAccount("admin", "admin", "petoojozsef@gmail.com");

        verify(userRepository, never()).save(any());
    }

    // --- UpdateUser ---

    @Test
    void updateUserShouldChangeUsernameEmailAndPassword() {
        User existing = regularUser(2L, "regifelhasznalonev", "regi@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.findByUsername("ujfelhasznalonev")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("uj@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("ujjelszo")).thenReturn("hashed-ujjelszo");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(2L, "ujfelhasznalonev", "uj@example.com", "ujjelszo");

        assertEquals("ujfelhasznalonev", result.getUsername());
        assertEquals("uj@example.com", result.getEmail());
        assertEquals("hashed-ujjelszo", result.getPassword());
    }

    @Test
    void updateUserShouldKeepPasswordWhenBlank() {
        User existing = regularUser(2L, "peto", "peto@example.com");
        existing.setPassword("unchanged-hash");
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateUser(2L, "peto", "peto@example.com", "");

        assertEquals("unchanged-hash", result.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUserShouldRejectEditingAdminAccount() {
        User admin = adminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class,
                () -> userService.updateUser(1L, "uj", "uj@example.com", ""));

        verify(userRepository, never()).save(any());
    }

    // --- DeleteUser ---

    @Test
    void deleteUserShouldRemoveRegularUser() {
        User regular = regularUser(2L, "peto", "peto@example.com");
        when(userRepository.findById(2L)).thenReturn(Optional.of(regular));

        userService.deleteUser(2L);

        verify(userRepository).delete(regular);
    }

    @Test
    void deleteUserShouldRejectDeletingAdminAccount() {
        User admin = adminUser(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThrows(AccessDeniedException.class, () -> userService.deleteUser(1L));

        verify(userRepository, never()).delete(any(User.class));
    }

}