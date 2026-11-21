package com.example.javarelay.userlogin.pojo;

import lombok.Data;

@Data
public class UserLogin {

    long userId;
    int userType;
    String passWord;
    String nickname;
    String phone;
}
