package com.financetracker.unit;

import com.financetracker.entity.Role;
import com.financetracker.entity.User;
import com.financetracker.repository.UserRepository;
import com.financetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRoles(Collections.singleton(Role.ROLE_USER));
        testUser.setEnabled(true);
    }

    @Test
    void loadUserByUsername_shouldReturnUserDetails() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = userService.loadUserByUsername("testuser");

        // then
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_userNotFound_shouldThrowException() {
        // given
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // when & then
        assertThrows(UsernameNotFoundException.class,
                () -> userService.loadUserByUsername("unknown"));
    }

    @Test
    void registerUser_shouldEncodePasswordAndSave() {
        // given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("new@example.com");
        newUser.setPassword("rawPassword");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // when
        User result = userService.registerUser(newUser);

        // then
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(newUser);
        assertEquals(Collections.singleton(Role.ROLE_USER), result.getRoles());
        assertTrue(result.isEnabled());
    }

    @Test
    void registerUser_usernameExists_shouldThrowException() {
        // given
        User newUser = new User();
        newUser.setUsername("existing");
        newUser.setEmail("new@example.com");

        when(userRepository.existsByUsername("existing")).thenReturn(true);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(newUser));
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_emailExists_shouldThrowException() {
        // given
        User newUser = new User();
        newUser.setUsername("newuser");
        newUser.setEmail("existing@example.com");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> userService.registerUser(newUser));
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void findByUsername_shouldReturnUser() {
        // given
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // when
        User result = userService.findByUsername("testuser");

        // then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void findById_shouldReturnUser() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // when
        User result = userService.findById(1L);

        // then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void findAllUsers_shouldReturnList() {
        // given
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        // when
        List<User> result = userService.findAllUsers();

        // then
        assertEquals(1, result.size());
    }

    @Test
    void updateUserRoles_shouldUpdateAndSave() {
        // given
        Set<Role> newRoles = Set.of(Role.ROLE_ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        User result = userService.updateUserRoles(1L, newRoles);

        // then
        assertEquals(newRoles, result.getRoles());
        verify(userRepository).save(testUser);
    }

    @Test
    void toggleUserEnabled_shouldUpdateEnabled() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // when
        userService.toggleUserEnabled(1L, false);

        // then
        assertFalse(testUser.isEnabled());
        verify(userRepository).save(testUser);
    }
}