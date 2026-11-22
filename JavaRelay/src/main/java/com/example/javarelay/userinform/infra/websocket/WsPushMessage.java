package com.example.javarelay.userinform.infra.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsPushMessage {

    private Integer userType;

    private Long userId;

    private String payload;

}
