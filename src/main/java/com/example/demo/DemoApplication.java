package com.example.demo;

import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class DemoApplication {


	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// 創建角色
			Role userRole = new Role("ROLE_USER");
			Role adminRole = new Role("ROLE_ADMIN");
			roleRepository.save(userRole);
			roleRepository.save(adminRole);

			// 創建用戶並設置角色
			User user = new User("user", "user@example.com", passwordEncoder.encode("user"));
			user.getRoles().add(userRole);
			userRepository.save(user);

			User admin = new User("admin", "admin@example.com", passwordEncoder.encode("admin"));
			admin.getRoles().add(adminRole);
			userRepository.save(admin);

			// 使用 findAll() 印出所有用戶和角色
			System.out.println("Users:");
			userRepository.findAll().forEach(System.out::println);

			System.out.println("Roles:");
			roleRepository.findAll().forEach(System.out::println);
		};
	}

}
