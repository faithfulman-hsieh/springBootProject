package com.taskmanager.account.usecase;

import com.taskmanager.account.adapter.out.repository.RoleRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.taskmanager.account.model.Role;
import com.taskmanager.account.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private RoleRepository roleRepository;
    private BCryptPasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, roleRepository, passwordEncoder);
    }

//    @Test
//    void testCreateUser() {
//        // Arrange
//        User newUser = new User();
//        newUser.setPassword("plainPassword");
//        Role role = new Role(1L, "ROLE_USER");
//        newUser.setRoles(Set.of(role));
//
//        when(roleRepository.findById(1L)).thenReturn(Optional.of(role));
//        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
//
//        // Act
//        User createdUser = userService.createUser(newUser);
//
//        // Assert
//        assertNotNull(createdUser);
//        assertNotEquals("plainPassword", createdUser.getPassword()); // 密碼應加密
//        assertTrue(createdUser.getRoles().contains(role)); // 角色應設置正確
//    }
//
//    @Test
//    void testCreateUserWithInvalidRole() {
//        // Arrange
//        User newUser = new User();
//        newUser.setPassword("plainPassword");
//        Role invalidRole = new Role(99L, "INVALID_ROLE");
//        newUser.setRoles(Set.of(invalidRole));
//
//        when(roleRepository.findById(99L)).thenReturn(Optional.empty());
//
//        // Act & Assert
//        assertThrows(IllegalArgumentException.class, () -> userService.createUser(newUser));
//    }
}