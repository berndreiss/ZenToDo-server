package net.berndreiss.zentodo.util;

import net.berndreiss.zentodo.data.Acknowledgement;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;

@Component
public class PubSubWebSocketHandler extends TextWebSocketHandler {

    public static final Map<String, Map<Integer, WebSocketSession>> sessions = Collections.synchronizedMap(new HashMap<>());

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
        Map<Integer, WebSocketSession> userSessions = sessions.computeIfAbsent(email, k -> Collections.synchronizedMap(new HashMap<>()));
        userSessions.put(Integer.parseInt(device), session);

        System.out.println("Client connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("Received message: " + message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object emailHeader = session.getHandshakeHeaders().getFirst("email");
        Object deviceHeader = session.getHandshakeHeaders().getFirst("device");
        if (emailHeader == null)
            throw new RuntimeException("Missing email header");
        if (deviceHeader == null)
            throw new RuntimeException("Missing device header");

        String email = emailHeader.toString();
        String device = deviceHeader.toString();
        Map<Integer, WebSocketSession> userSessions = sessions.get(email);
        userSessions.remove(Integer.parseInt(device));
        if (userSessions.isEmpty())
            sessions.remove(email);
        System.out.println("Client disconnected: " + session.getId());
    }

    public List<Integer> publishEvent(String id, String message, String email, List<Integer> devices) {
        synchronized (sessions) {
            List<Integer> notSent = new ArrayList<>(devices);
            Map<Integer, WebSocketSession> socketSessions = sessions.get(email);

            if (socketSessions == null)
                return notSent;
            sessions.get(email).forEach( (key, value) ->{
                if (devices.contains(key)){
                    try {
                        value.sendMessage(new TextMessage("{\"message\": " + message + ", \"id\": \"" + id + "\"}"));
                        notSent.remove(key);
                    } catch (Exception e) {
                        //TODO LOG
                    }
                }
            });
            return notSent;
        }
    }
    public int getNumberOfSession(){
        return sessions.size();
    }

}
