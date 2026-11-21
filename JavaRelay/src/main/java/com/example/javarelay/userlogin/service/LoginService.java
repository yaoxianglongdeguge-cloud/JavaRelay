package com.example.javarelay.userlogin.service;

import com.example.javarelay.dto.Result;
import com.example.javarelay.userlogin.pojo.UserLogin;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public interface LoginService {

    public Result userRegister(@RequestBody UserLogin user);

    public Result userLogin(@RequestBody UserLogin user);

    public Result userChange(@RequestBody UserLogin user,@RequestBody UserLogin userNew);

    public UserLogin validateToken(String token);

}
