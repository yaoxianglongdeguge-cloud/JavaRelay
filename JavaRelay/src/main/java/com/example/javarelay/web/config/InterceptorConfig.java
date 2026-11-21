package com.example.javarelay.web.config;

import com.example.javarelay.userlogin.interceptor.DriverInterceptor;
import com.example.javarelay.userlogin.interceptor.LoginInterceptor;
import com.example.javarelay.userlogin.interceptor.PassengerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Autowired
    LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // ① 先鉴权：/passenger/** 和 /driver/** 都必须带 token
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/passenger/**", "/driver/**")
                .order(1);

        // ② 再校验角色
        registry.addInterceptor(new PassengerInterceptor())
                .addPathPatterns("/passenger/**")
                .order(2);

        registry.addInterceptor(new DriverInterceptor())
                .addPathPatterns("/driver/**")
                .order(2);
    }
}
