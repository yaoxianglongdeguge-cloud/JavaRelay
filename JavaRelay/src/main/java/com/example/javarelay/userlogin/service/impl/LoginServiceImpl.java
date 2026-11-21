package com.example.javarelay.userlogin.service.impl;

import com.example.javarelay.data.entity.DriverDetail;
import com.example.javarelay.data.entity.Passenger;
import com.example.javarelay.data.mapper.DriverDetailMapper;
import com.example.javarelay.data.mapper.PassengerMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.rootconstants.RootConstants;
import com.example.javarelay.userlogin.pojo.UserLogin;
import com.example.javarelay.userlogin.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;

public class LoginServiceImpl implements LoginService {

    @Autowired
    PassengerMapper passengerMapper;
    @Autowired
    DriverDetailMapper driverDetailMapper;

    @Override
    public Result userRegister(UserLogin user) {

        if(user.getUserType() == RootConstants.DRIVER){
            return driverRegister(user);
        }else{
            return passengerRegister(user);
        }
    }

    private Result passengerRegister(UserLogin user){
        Passenger passenger = passengerMapper.selectBy(user.getUserId());
        if(passenger != null){
            return Result.success("用户已存在");
        }
    }
    private Result driverRegister(UserLogin user){

    }

    @Override
    public Result userLogin(UserLogin user) {
        return null;
    }

    @Override
    public Result userChange(UserLogin user, UserLogin userNew) {
        return null;
    }

    @Override
    public UserLogin validateToken(String token) {
        return null;
    }
}
