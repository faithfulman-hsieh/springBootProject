package com.taskmanager.account.adapter;

import com.taskmanager.account.model.User;
import com.taskmanager.account.adapter.out.repository.AccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
public class AccountRepositorySpringJpaAdapterTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    @DisplayName("Test finding a user by name")
    public void testFindByName() {
        // Arrange: 建立並保存一個 User
        User user = new User();
        user.setName("TestUser");
        accountRepository.save(user);

        // Act: 使用 findByName 方法查找 User
        Optional<User> foundUser = accountRepository.findByName("TestUser");

        // Assert: 驗證查找結果
        assertTrue(foundUser.isPresent(), "User should be present");
        assertEquals("TestUser", foundUser.get().getName());
    }
}