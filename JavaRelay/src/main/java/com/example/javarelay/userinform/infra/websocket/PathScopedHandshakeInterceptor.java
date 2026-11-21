package com.example.javarelay.userinform.infra.websocket;

import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Set;

public interface PathScopedHandshakeInterceptor extends HandshakeInterceptor {
    //返回空集合表示对所有端点生效
    Set<String> paths();
}