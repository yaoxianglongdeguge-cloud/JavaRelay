package com.example.javarelay.userlogin.interceptor;

import com.example.javarelay.rootconstants.RootConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class DriverInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        Object type = req.getAttribute("userType");

        if (type == null || !type.equals(RootConstants.DRIVER)) {

            resp.setStatus(401);
            resp.setContentType("application/json;charset=UTF-8");
            try{
                resp.getWriter().write("{\"code\":401,\"message\":\"非司机无法执行此操作\"}");
            }catch (Exception e){

            }

            return false;
        }

        return true;
    }
}
