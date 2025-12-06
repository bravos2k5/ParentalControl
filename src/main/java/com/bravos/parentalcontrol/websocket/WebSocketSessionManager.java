package com.bravos.parentalcontrol.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class WebSocketSessionManager {
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  public void register(String sessionId, WebSocketSession session) {
    sessions.put(sessionId, session);
  }

  public void remove(String sessionId) {
    WebSocketSession session = sessions.remove(sessionId);
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (IOException e) {
        log.warn("Error closing WebSocket for session {}: {}", sessionId, e.getMessage());
      }
    }
  }

  public WebSocketSession get(String sessionId) {
    return sessions.get(sessionId);
  }

  public void sendMessage(String sessionId, String message) {
    WebSocketSession session = sessions.get(sessionId);
    if (session == null || !session.isOpen()) {
      throw new IllegalStateException("WebSocket session is not open or does not exist for id: " + sessionId);
    }
    try {
      session.sendMessage(new TextMessage(message));
    } catch (IOException e) {
      log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
      throw new RuntimeException("Failed to send message to session: " + e.getMessage(), e);
    }
  }

  public boolean isOpen(String sessionId) {
    WebSocketSession session = sessions.get(sessionId);
    return session != null && session.isOpen();
  }
}
