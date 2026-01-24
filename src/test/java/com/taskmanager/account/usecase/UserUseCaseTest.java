package com.taskmanager.account.usecase;

import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.taskmanager.account.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserUseCaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void testCreateUser() {
//        User user = new User();
//        user.setUsername("Test User");
//
//        when(userRepository.save(user)).thenReturn(user);
//
//        User result = userService.createUser(user);
//
//        assertNotNull(result);
//        assertEquals("Test User", result.getUsername());
//        verify(userRepository, times(1)).save(user);
//    }
//
//    @Test
//    void testGetUserById() {
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("Test User");
//
//        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
//
//        User result = userService.getUserById(1L);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("Test User", result.getUsername());
//        verify(userRepository, times(1)).findById(1L);
//    }
//
//    @Test
//    void testGetAllUsers() {
//        List<User> users = new ArrayList<>();
//        User user = new User();
//        user.setUsername("Test User");
//        users.add(user);
//
//        when(userRepository.findAll()).thenReturn(users);
//
//        List<User> result = userService.getAllUsers();
//
//        assertFalse(result.isEmpty());
//        assertEquals(1, result.size());
//        assertEquals("Test User", result.get(0).getUsername());
//        verify(userRepository, times(1)).findAll();
//    }
//
//    @Test
//    void testUpdateUser() {
//        User user = new User();
//        user.setId(1L);
//        user.setUsername("Updated User");
//
//        when(userRepository.save(user)).thenReturn(user);
//
//        User result = userService.updateUser(1L, user);
//
//        assertNotNull(result);
//        assertEquals(1L, result.getId());
//        assertEquals("Updated User", result.getUsername());
//        verify(userRepository, times(1)).save(user);
//    }
//
//    @Test
//    void testDeleteUser() {
//        Long userId = 1L;
//
//        doNothing().when(userRepository).deleteById(userId);
//
//        userService.deleteUser(userId);
//
//        verify(userRepository, times(1)).deleteById(userId);
//    }
}