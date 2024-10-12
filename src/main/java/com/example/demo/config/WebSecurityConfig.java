package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    // 建立白名單
    private static final String[] WHITE_LIST = {
            //"/api/login",
            "/api/adduser"
    };

    // 1. 建立http相關的規則
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 跨域請求設定: 先用預設
                .cors(Customizer.withDefaults())
                // stateless API使用token驗證時, 不需要csrf保護
                .csrf(csrf -> csrf.disable())
                // 驗證API的規則, 依序處理
                .authorizeHttpRequests(authorize -> authorize
                        // (1). 符合特定URL規則, 直接通過
                        .requestMatchers(WHITE_LIST).permitAll()
                        // (2). 任何請求皆須經過驗證
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())  // 允許 Basic Auth，供 Postman 使用
                .formLogin(Customizer.withDefaults()) // 啟用預設登錄表單
                .logout(logout -> logout.logoutUrl("/logout") // 登出所使用的url
                        .permitAll());

        return http.build();
    }

    @Bean
    // 建立全域規則
    public WebSecurityCustomizer webSecurityCustomizer() {
        // bypass特定url
        return (web) -> web.ignoring().requestMatchers("/matrix/pass");
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        // default: bcrypt >> SHA-256 + salt + secret key
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        UserDetails userA = User.builder()
                .username("user")
                .password(encoder.encode("user"))
                .roles("USER")
                .build();

        UserDetails userB = User.builder()
                .username("admin")
                .password(encoder.encode("admin"))
                .roles("ADMIN")
                .build();

        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        users.createUser(userA);
        users.createUser(userB);

        return users;
    }
}
