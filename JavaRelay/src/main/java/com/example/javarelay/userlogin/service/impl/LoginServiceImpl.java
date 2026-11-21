package com.example.javarelay.userlogin.service.impl;

import com.example.javarelay.data.entity.Driver;
import com.example.javarelay.data.entity.DriverDetail;
import com.example.javarelay.data.entity.Passenger;
import com.example.javarelay.data.mapper.DriverDetailMapper;
import com.example.javarelay.data.mapper.DriverMapper;
import com.example.javarelay.data.mapper.PassengerMapper;
import com.example.javarelay.dto.Result;
import com.example.javarelay.infra.JwtUtil;
import com.example.javarelay.rootconstants.RootConstants;
import com.example.javarelay.userlogin.pojo.UserLogin;
import com.example.javarelay.userlogin.service.LoginService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginServiceImpl implements LoginService {

    @Autowired
    PassengerMapper passengerMapper;
    @Autowired
    DriverDetailMapper driverDetailMapper;
    @Autowired
    DriverMapper driverMapper;
    @Autowired
    JwtUtil jwtUtil;

    private static final PasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 用户不存在与密码错误统一提示，避免手机号被枚举 */
    private static final String LOGIN_FAIL = "手机号或密码错误";

    // ==================== 注册 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result userRegister(UserLogin user) {

        if (user == null || user.getPassWord() == null || user.getPhone() == null) {
            return Result.fail("信息不全");
        }

        if (user.getPassWord().length() > 32) {
            return Result.fail("密码不能超过 32 位");
        }

        if (user.getUserType() == RootConstants.DRIVER) {
            return driverRegister(user);
        }
        return passengerRegister(user);
    }

    private Result passengerRegister(UserLogin user) {

        if (passengerMapper.selectByPhone(user.getPhone()) != null) {
            return Result.fail("手机号已注册");
        }

        Passenger p = new Passenger();
        p.setPhone(user.getPhone());
        p.setNickname(user.getNickname());
        p.setPassword(ENCODER.encode(user.getPassWord()));
        passengerMapper.insert(p);

        return Result.success("注册成功");
    }

    private Result driverRegister(UserLogin user) {

        if (driverDetailMapper.selectByPhone(user.getPhone()) != null) {
            return Result.fail("手机号已注册");
        }

        // 先建司机主表（状态默认空闲），再建详情并关联
        Driver driver = new Driver();
        driver.setStatus(0);
        driverMapper.insert(driver);

        DriverDetail detail = new DriverDetail();
        detail.setDriverId(driver.getId());          // ← 关键：必须关联主表
        detail.setPhone(user.getPhone());
        detail.setNickname(user.getNickname());
        detail.setPassword(ENCODER.encode(user.getPassWord()));
        driverDetailMapper.insert(detail);

        return Result.success("注册成功");
    }

    // ==================== 登录 ====================

    @Override
    public Result userLogin(UserLogin user) {

        if (user == null || user.getPhone() == null || user.getPassWord() == null) {
            return Result.fail("信息不全");
        }

        if (user.getUserType() == RootConstants.DRIVER) {
            return driverLogin(user);
        }
        return passengerLogin(user);
    }

    private Result passengerLogin(UserLogin user) {

        Passenger passenger = passengerMapper.selectByPhone(user.getPhone());

        if (passenger == null
                || !ENCODER.matches(user.getPassWord(), passenger.getPassword())) {
            return Result.fail(LOGIN_FAIL);
        }

        String token = issueToken(passenger.getPassengerId(), RootConstants.PASSENGER);
        return Result.success("登录成功", token);
    }

    private Result driverLogin(UserLogin user) {

        DriverDetail detail = driverDetailMapper.selectByPhone(user.getPhone());

        if (detail == null
                || !ENCODER.matches(user.getPassWord(), detail.getPassword())) {
            return Result.fail(LOGIN_FAIL);
        }

        String token = issueToken(detail.getDriverId(), RootConstants.DRIVER);
        return Result.success("登录成功", token);
    }

    // ==================== token ====================

    public String issueToken(Long userId, Integer type) {
        return jwtUtil.issue(userId, type);
    }

    @Override
    public UserLogin validateToken(String token) {

        if (token == null || token.isBlank()) {
            return null;
        }

        Claims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (Exception e) {
            // 过期、签名错误、格式不对，一律当作无效
            return null;
        }

        Integer type = claims.get("type", Integer.class);
        if (type == null) {
            return null;
        }

        UserLogin user = new UserLogin();
        user.setUserId(Long.parseLong(claims.getSubject()));
        user.setUserType(type);
        // 不回填 passWord / phone
        return user;
    }
}
