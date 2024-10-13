package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLogFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // 紀錄請求的 URL 和 HTTP 方法
        logger.info("Request URL: {}, Method: {}", request.getRequestURI(), request.getMethod());

        filterChain.doFilter(request, response);

        // 計算處理時間並紀錄狀態碼
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Response Status: {}, Duration: {} ms", response.getStatus(), duration);
    }
}

