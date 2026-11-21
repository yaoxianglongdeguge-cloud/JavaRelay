package com.example.javarelay.infra.websocket;

import com.example.javarelay.rootconstants.RootConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MyWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> ONLINE_DRIVER = new ConcurrentHashMap<>();
    private static final Map<Long, WebSocketSession> ONLINE_PASSENGER = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Principal principal = session.getPrincipal();
        if (principal != null) {
            String[] Info = getUserInfo(principal.getName());
            Integer userType =Integer.valueOf(Info[0]);
            Long userId = Long.valueOf(Info[1]);

            if(userType.equals(RootConstants.DRIVER)){
                ONLINE_DRIVER.put(userId,
                        new ConcurrentWebSocketSessionDecorator(session, 10_000, 512 * 1024));
            }else if(userType.equals(RootConstants.PASSENGER)){
                ONLINE_PASSENGER.put(userId,
                        new ConcurrentWebSocketSessionDecorator(session, 10_000, 512 * 1024));
            }
        }
    }

    private String[] getUserInfo(String user){

        return user.split(":");
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Principal principal = session.getPrincipal();
        String reply = "收到(" + (principal != null ? principal.getName() : "未知") + "): "
                + message.getPayload();
        session.sendMessage(new TextMessage(reply));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Principal principal = session.getPrincipal();
        if (principal != null) {
            String[] Info = getUserInfo(principal.getName());
            Integer userType =Integer.valueOf(Info[0]);
            Long userId = Long.valueOf(Info[1]);
            if(userType.equals(RootConstants.DRIVER)){
                ONLINE_DRIVER.remove(userId);
            }else if(userType.equals(RootConstants.PASSENGER)){
                ONLINE_PASSENGER.remove(userId);
            }
        }
    }

    public void sendToUser(Integer userType,Long userId, String msg) {

        if(userType.equals(RootConstants.DRIVER)){
            WebSocketSession session = ONLINE_DRIVER.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(msg));
                } catch (Exception e) {
                    ONLINE_DRIVER.remove(userId);
                }
            }
        }else if(userType.equals(RootConstants.PASSENGER)){
            WebSocketSession session = ONLINE_PASSENGER.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(msg));
                } catch (Exception e) {
                    ONLINE_PASSENGER.remove(userId);
                }
            }
        }

    }
}
