package com.example.javarelay.web.config;

import com.example.javarelay.userlogin.interceptor.AuthHandshakeInterceptor;
import com.example.javarelay.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.infra.websocket.UserHandshakeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MyWebSocketHandler handler;

    @Autowired
    private AuthHandshakeInterceptor interceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws")
                .addInterceptors(interceptor)
                .setHandshakeHandler(new UserHandshakeHandler())
                .setAllowedOriginPatterns("https://localhost/");
    }
}