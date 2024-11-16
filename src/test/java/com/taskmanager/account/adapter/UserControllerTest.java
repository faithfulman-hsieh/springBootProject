//package com.taskmanager.account.adapter;
//
//import com.taskmanager.account.adapter.in.rest.UserController;
//import com.taskmanager.account.model.User;
//import com.taskmanager.account.usecase.UserUseCase;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//import org.springframework.http.ResponseEntity;
//
//import java.util.Collections;
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class UserControllerTest {
//
//    private UserUseCase userUseCase;
//    private UserController userController;
//
//    @BeforeEach
//    void setUp() {
//        // Mock the UserUseCase
//        userUseCase = Mockito.mock(UserUseCase.class);
//        userController = new UserController(userUseCase);
//    }
//
//    @Test
//    void testGetUsers() {
//        // Arrange
//        when(userUseCase.getAllUsers()).thenReturn(Collections.emptyList());
//
//        // Act
//        List<User> users = userController.getUsers(null);
//
//        // Assert
//        assertNotNull(users);
//        assertTrue(users.isEmpty());
//        verify(userUseCase, times(1)).getAllUsers();
//    }
//
//    @Test
//    void testAddUser() {
//        // Arrange
//        User newUser = new User();
//        doNothing().when(userUseCase).createUser(newUser);
//
//        // Act
//        ResponseEntity<String> response = userController.addUser(newUser);
//
//        // Assert
//        assertEquals("User added successfully!", response.getBody());
//        verify(userUseCase, times(1)).createUser(newUser);
//    }
//}