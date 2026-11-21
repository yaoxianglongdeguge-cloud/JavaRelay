package com.example.javarelay.userlogin.service;

import com.example.javarelay.dto.Result;
import com.example.javarelay.userlogin.pojo.UserLogin;

public interface LoginService {

    Result userRegister(UserLogin user);

    Result userLogin(UserLogin user);

    UserLogin validateToken(String token);

    String issueToken(Long userId, Integer type);
}
