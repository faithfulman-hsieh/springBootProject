package com.taskmanager;

import com.taskmanager.account.model.Role;
import com.taskmanager.account.model.User;
import com.taskmanager.account.adapter.out.repository.RoleRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.taskmanager.process.model.ProcessDef;
import com.taskmanager.process.repository.ProcessDefRepository;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootApplication
public class TaskManagerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaskManagerApplication.class, args);
	}

	@Bean
	public CommandLineRunner run(UserRepository userRepository,
								 RoleRepository roleRepository,
								 PasswordEncoder passwordEncoder,
								 RepositoryService repositoryService,
								 ProcessDefRepository processDefRepository) {
		return args -> {
			// -------------------------------------------------
			// 1. 初始化使用者與權限資料
			// -------------------------------------------------
			if (roleRepository.count() == 0) {
				Role userRole = new Role("ROLE_USER");
				Role adminRole = new Role("ROLE_ADMIN");
				// ★★★ 新增 IT 角色 ★★★
				Role itRole = new Role("ROLE_IT");

				roleRepository.save(userRole);
				roleRepository.save(adminRole);
				roleRepository.save(itRole);

				// User 1: admin
				User admin = new User("admin", "admin@example.com", passwordEncoder.encode("admin"));
				admin.getRoles().add(adminRole);
				userRepository.save(admin);

				// User 2: user
				User user = new User("user", "user@example.com", passwordEncoder.encode("user"));
				user.getRoles().add(userRole);
				userRepository.save(user);

				// User 3: manager
				User manager = new User("manager", "manager@example.com", passwordEncoder.encode("manager"));
				manager.getRoles().add(userRole);
				userRepository.save(manager);

				// ★★★ 新增 IT User ★★★
				User ituser = new User("ituser", "ituser@example.com", passwordEncoder.encode("ituser"));
				ituser.getRoles().add(itRole); // 給予 IT 權限
				userRepository.save(ituser);

				System.out.println("✅ 初始化使用者(admin, user, manager, ituser)與角色完成");
			}

			// -------------------------------------------------
			// 2. 自動部署流程定義
			// -------------------------------------------------
			System.out.println("---------- 開始自動部署 Demo 流程 ----------");

			deployProcessIfNeeded(repositoryService, processDefRepository, "leaveProcess", "請假流程", "processes/leaveProcess.bpmn20.xml");
			deployProcessIfNeeded(repositoryService, processDefRepository, "purchaseProcess", "採購流程", "processes/purchaseProcess.bpmn20.xml");
			deployProcessIfNeeded(repositoryService, processDefRepository, "todoProcess", "待辦事項流程", "processes/todoProcess.bpmn20.xml");
			deployProcessIfNeeded(repositoryService, processDefRepository, "countersignProcess", "聯合會簽流程", "processes/countersignProcess.bpmn20.xml");

			// ★★★ 部署 IT 報修流程 ★★★
			deployProcessIfNeeded(repositoryService, processDefRepository, "itRepairProcess", "IT 報修流程", "processes/itRepairProcess.bpmn20.xml");

			System.out.println("---------- Demo 流程部署檢查完成 ----------");
		};
	}

	private void deployProcessIfNeeded(RepositoryService repositoryService,
									   ProcessDefRepository processDefRepository,
									   String processKey,
									   String deploymentName,
									   String resourcePath) {
		try {
			List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
					.processDefinitionKey(processKey)
					.list();

			ProcessDefinition targetDefinition = null;

			if (list.isEmpty()) {
				Deployment deployment = repositoryService.createDeployment()
						.addClasspathResource(resourcePath)
						.name(deploymentName)
						.deploy();

				System.out.println("✅ 自動部署成功: " + deploymentName + " (ID: " + deployment.getId() + ")");

				targetDefinition = repositoryService.createProcessDefinitionQuery()
						.deploymentId(deployment.getId())
						.singleResult();
			} else {
				targetDefinition = list.get(0);
				System.out.println("ℹ️ 流程已存在: " + deploymentName);
			}

			if (targetDefinition != null) {
				if (!processDefRepository.existsById(targetDefinition.getId())) {
					ProcessDef processDef = new ProcessDef();
					processDef.setId(targetDefinition.getId());
					processDef.setName(deploymentName);
					processDef.setVersion(targetDefinition.getVersion() + ".0");
					processDef.setStatus("active");
					processDef.setDeploymentTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
					processDef.setProcessDefinitionId(targetDefinition.getId());

					processDefRepository.save(processDef);
					System.out.println("   └── ✅ 已同步至 ProcessDef 資料表");
				}
			}

		} catch (Exception e) {
			System.err.println("❌ 部署失敗 [" + deploymentName + "]: " + e.getMessage());
		}
	}
}