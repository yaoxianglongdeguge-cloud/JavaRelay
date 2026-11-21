package com.example.javarelay.userlogin.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.example.javarelay.dto.Result;
import com.example.javarelay.userlogin.pojo.UserLogin;
import com.example.javarelay.userlogin.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Autowired
    LoginService loginService;

    @PostMapping("/register")
    public Result register(@RequestBody UserLogin user) {
        return loginService.userRegister(user);
    }

    @SentinelResource(value = "login:signin", blockHandler = "signinBlocked")
    @PostMapping("/signin")
    public Result signin(@RequestBody UserLogin user) {
        return loginService.userLogin(user);
    }

    public Result signinBlocked(UserLogin user, BlockException e) {
        return Result.tooManyRequests("登录请求过于频繁，请稍后再试");
    }
}
