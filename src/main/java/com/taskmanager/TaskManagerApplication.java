package com.taskmanager;

import com.taskmanager.account.model.Role;
import com.taskmanager.account.model.User;
import com.taskmanager.account.adapter.out.repository.RoleRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.taskmanager.process.model.ProcessDef; // 引入 ProcessDef
import com.taskmanager.process.repository.ProcessDefRepository; // 引入 ProcessDefRepository
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
								 ProcessDefRepository processDefRepository) { // ★★★ 1. 注入 ProcessDefRepository ★★★
		return args -> {
			// -------------------------------------------------
			// 1. 初始化使用者與權限資料
			// -------------------------------------------------
			if (roleRepository.count() == 0) {
				Role userRole = new Role("ROLE_USER");
				Role adminRole = new Role("ROLE_ADMIN");
				roleRepository.save(userRole);
				roleRepository.save(adminRole);

				User user = new User("user", "user@example.com", passwordEncoder.encode("user"));
				user.getRoles().add(userRole);
				userRepository.save(user);

				User admin = new User("admin", "admin@example.com", passwordEncoder.encode("admin"));
				admin.getRoles().add(adminRole);
				userRepository.save(admin);

				System.out.println("初始化使用者與角色完成");
			}

			// -------------------------------------------------
			// 2. 自動部署流程定義
			// -------------------------------------------------
			System.out.println("---------- 開始自動部署 Demo 流程 ----------");

			// 傳入 processDefRepository 以便同步資料
			deployProcessIfNeeded(repositoryService, processDefRepository, "leaveProcess", "請假流程", "processes/leaveProcess.bpmn20.xml");
			deployProcessIfNeeded(repositoryService, processDefRepository, "purchaseProcess", "採購流程", "processes/purchaseProcess.bpmn20.xml");
			deployProcessIfNeeded(repositoryService, processDefRepository, "todoProcess", "待辦事項流程", "processes/todoProcess.bpmn20.xml");

			System.out.println("---------- Demo 流程部署檢查完成 ----------");
		};
	}

	// ★★★ 輔助方法：部署並同步至 ProcessDef 資料表 ★★★
	private void deployProcessIfNeeded(RepositoryService repositoryService,
									   ProcessDefRepository processDefRepository,
									   String processKey,
									   String deploymentName,
									   String resourcePath) {
		try {
			// 1. 檢查 Activiti 引擎中是否已有此 Key 的流程
			List<ProcessDefinition> list = repositoryService.createProcessDefinitionQuery()
					.processDefinitionKey(processKey)
					.list();

			ProcessDefinition targetDefinition = null;

			if (list.isEmpty()) {
				// 若無，則進行部署
				Deployment deployment = repositoryService.createDeployment()
						.addClasspathResource(resourcePath)
						.name(deploymentName)
						.deploy();

				System.out.println("自動部署成功: " + deploymentName + " (ID: " + deployment.getId() + ")");

				// 部署後，重新查詢取得該 ProcessDefinition 物件 (為了拿 ID 和 Version)
				targetDefinition = repositoryService.createProcessDefinitionQuery()
						.deploymentId(deployment.getId())
						.singleResult();
			} else {
				// 若已有，取最新版 (為了檢查是否需要補寫入 ProcessDef 表)
				targetDefinition = list.get(0);
				System.out.println("ℹ流程已存在引擎中: " + deploymentName);
			}

			// 2. ★★★ 關鍵修正：同步寫入 ProcessDef 資料表 (前端才看得到) ★★★
			if (targetDefinition != null) {
				// 檢查 ProcessDef 表是否已有此 ID
				if (!processDefRepository.existsById(targetDefinition.getId())) {
					ProcessDef processDef = new ProcessDef();
					processDef.setId(targetDefinition.getId());
					processDef.setName(deploymentName); // 使用我們指定的名稱，或用 targetDefinition.getName()
					processDef.setVersion(targetDefinition.getVersion() + ".0");
					processDef.setStatus("active");
					processDef.setDeploymentTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
					processDef.setProcessDefinitionId(targetDefinition.getId());

					processDefRepository.save(processDef);
					System.out.println("   └── 已同步至 ProcessDef 資料表 (前端可見)");
				}
			}

		} catch (Exception e) {
			// 捕捉例如檔案找不到的例外，只印出錯誤但不中斷程式
			System.err.println("部署或同步失敗 [" + deploymentName + "]: " + e.getMessage());
		}
	}
}