package com.example.javarelay.web.config;

import com.example.javarelay.userinform.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.userinform.infra.websocket.UserHandshakeHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class InformWebSocketConfig implements WebSocketConfigurer {


    private MyWebSocketHandler handler = null;
    private HandshakeInterceptor interceptor = null;

    public InformWebSocketConfig( MyWebSocketHandler handler,HandshakeInterceptor handshakeInterceptor){
        this.handler = handler;
        this.interceptor = handshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        WebSocketHandlerRegistration reg = registry.addHandler(handler, "/ws")
                .addInterceptors(interceptor)
                .setHandshakeHandler(new UserHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }

}