package com.example.javarelay.web.config;

import com.example.javarelay.userinform.infra.websocket.PathScopedHandshakeInterceptor;
import com.example.javarelay.userinform.infra.websocket.MyWebSocketHandler;
import com.example.javarelay.userinform.infra.websocket.UserHandshakeHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final MyWebSocketHandler handler = null;
    private final ObjectProvider<PathScopedHandshakeInterceptor> interceptors = null;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        register(registry, "/ws");
        register(registry, "/ws/public");     // 这个端点如果不想挂鉴权，见下面说明
    }

    private void register(WebSocketHandlerRegistry registry, String path) {
        WebSocketHandlerRegistration reg = registry.addHandler(handler, path)
                .setHandshakeHandler(new UserHandshakeHandler())
                .setAllowedOriginPatterns("*");

        interceptors.orderedStream()
                .filter(i -> i.paths().isEmpty() || i.paths().contains(path))
                .forEach(reg::addInterceptors);
    }
}