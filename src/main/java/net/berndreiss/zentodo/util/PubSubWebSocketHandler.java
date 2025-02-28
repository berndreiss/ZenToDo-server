package net.berndreiss.zentodo.util;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class PubSubWebSocketHandler extends TextWebSocketHandler {

    public static final Map<String, Map<Long, WebSocketSession>> sessions = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, String> messageAcknowledgments = Collections.synchronizedMap(new HashMap<>());

    private static Integer id = 0;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {


        Object emailHeader = session.getHandshakeHeaders().getFirst("email");
        Object deviceHeader = session.getHandshakeHeaders().getFirst("device");
        if (emailHeader == null)
            throw new RuntimeException("Missing email header");
        if (deviceHeader == null)
            throw new RuntimeException("Missing device header");

        String email = emailHeader.toString();
        String device = deviceHeader.toString();
        Map<Long, WebSocketSession> userSessions = sessions.get(email);
        if (userSessions == null) {
            userSessions = Collections.synchronizedMap(new HashMap<>());
            sessions.put(email, userSessions);
        }
        userSessions.put(Long.parseLong(device), session);
        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Received message: " + message.getPayload());
        // Echo back or process client message if needed
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("Client disconnected: " + session.getId());
    }

    public List<Long> publishEvent(String message, String email, List<Long> devices) {
        synchronized (sessions) {
            List<Long> notSent = new ArrayList<>(devices);
            Map<Long, WebSocketSession> socketSessions = sessions.get(email);

            if (socketSessions == null)
                return notSent;
            sessions.get(email).forEach( (key, value) ->{
                if (devices.contains(key)){
                    try {
                        int id;
                        synchronized (PubSubWebSocketHandler.class) {
                            id = PubSubWebSocketHandler.id++;
                        }
                        value.sendMessage(new TextMessage("{\"message\": " + message + ", \"id\": \"" + id + "\"}"));
                        messageAcknowledgments.put(id + "-" + email +  "-" + key, message);
                        notSent.remove(key);
                    } catch (Exception e) {
                        //TODO LOG
                    }
                }
            });
            return notSent;
        }
    }
    public void handleAcknowledgment(Acknowledgement acknowledgement){
        messageAcknowledgments.remove(acknowledgement.getId() + "-" + acknowledgement.getEmail() + "-" + acknowledgement.getDevice());
    }
}
