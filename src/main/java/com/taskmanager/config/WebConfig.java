package com.taskmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOriginPatterns(
//                        "http://localhost:5173", // 本地開發的前端來源
//                        "https://vue3-project-faithfulman.vercel.app/" // 部署後的前端來源
//                )
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允許的 HTTP 方法
//                .allowedHeaders("*") // 允許的請求頭
//                .allowCredentials(true); // 允許攜帶憑證
//    }

//    @Bean
//    public CorsFilter corsFilter() {
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        CorsConfiguration config = new CorsConfiguration();
//        config.setAllowCredentials(true); // 允許攜帶憑證
//        config.addAllowedOriginPattern("http://localhost:5173"); // 允許的來源
//        config.addAllowedOriginPattern("https://vue3-project-faithfulman.vercel.app"); // 允許的來源
//        config.addAllowedHeader("*"); // 允許的標頭
//        config.addAllowedMethod("*"); // 允許的請求方法
//        source.registerCorsConfiguration("/**", config);
//        return new CorsFilter(source);
//    }

}
