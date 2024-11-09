package com.taskmanager.account.adapter;

import com.taskmanager.account.adapter.out.repository.RoleRepository;
import com.taskmanager.account.model.Role;
import com.taskmanager.account.model.User;
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
public class RoleRepositorySpringJpaAdapterTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    @DisplayName("Test finding a role by name")
    public void testFindByName() {
        // Arrange: 建立並保存一個 Role
        Role roel = new Role();
        roel.setName("TestRole");
        roleRepository.save(roel);

        // Act: 使用 findByName 方法查找 Role
        Optional<Role> foundRole = Optional.ofNullable(roleRepository.findByName("TestRole"));

        // Assert: 驗證查找結果
        assertTrue(foundRole.isPresent(), "Role should be present");
        assertEquals("TestRole", foundRole.get().getName());
    }
}