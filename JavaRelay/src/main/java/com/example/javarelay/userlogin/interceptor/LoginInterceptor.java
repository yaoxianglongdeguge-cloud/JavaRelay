package com.example.javarelay.userlogin.interceptor;

import com.example.javarelay.userlogin.pojo.UserLogin;
import com.example.javarelay.userlogin.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    LoginService loginService;

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        String token = req.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        UserLogin user = loginService.validateToken(token);


        if (user == null) {
            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            try{
                resp.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            }catch (Exception e){

            }

            return false;
        }

        req.setAttribute("userId", user.getUserId());
        req.setAttribute("userType",user.getUserType());
        return true;
    }
}
