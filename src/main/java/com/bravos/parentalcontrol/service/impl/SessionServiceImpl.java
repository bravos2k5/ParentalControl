package com.bravos.parentalcontrol.service.impl;

import com.bravos.parentalcontrol.model.NewSessionRequest;
import com.bravos.parentalcontrol.model.Session;
import com.bravos.parentalcontrol.repository.SessionRepository;
import com.bravos.parentalcontrol.service.SessionService;
import com.bravos.parentalcontrol.utils.DateTimeHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class SessionServiceImpl implements SessionService {

  private final SessionRepository sessionRepository;
  private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

  public SessionServiceImpl(SessionRepository sessionRepository, RedisTemplate<?, ?> redisTemplate) {
    this.sessionRepository = sessionRepository;
    redisTemplate.getConnectionFactory().getConnection().flushAll();
  }

  @Override
  public Session createNewSession(NewSessionRequest request, WebSocketSession webSocketSession) {
    sessions.put(request.getId(), webSocketSession);
    Session session = Session.builder()
        .id(request.getId())
        .deviceName(request.getDeviceName())
        .deviceId(request.getDeviceId())
        .ipAddress(request.getIpAddress())
        .createdAt(DateTimeHelper.currentTimeMillis())
        .build();
    return sessionRepository.save(session);
  }

  @Override
  public List<Session> getAllSessions() {
    List<Session> sessions = sessionRepository.findAll();
    sessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return sessions;
  }

  @Override
  public void deleteSession(String sessionId) {
    this.removeSession(sessionId);
  }

  @Override
  public void deleteAllSessions() {
    List<Session> sessions = getAllSessions();
    for (Session session : sessions) {
      this.removeSession(session.getId());
    }
  }

  @Override
  public void deleteSessionsByDeviceId(String deviceId) {
    List<Session> sessions = sessionRepository.findByDeviceId(deviceId);
    for (Session session : sessions) {
      this.removeSession(session.getId());
    }
  }

  @Override
  public void updateLastActive(String sessionId, Long lastActiveTime) {
    if(lastActiveTime == null) {
      lastActiveTime = DateTimeHelper.currentTimeMillis();
    }
    Session session = sessionRepository.findById(sessionId).orElse(null);
    if (session != null) {
      session.setLastActive(lastActiveTime);
      sessionRepository.save(session);
    }
  }

  @Override
  public Session getSessionByDeviceId(String deviceId) {
    List<Session> sessions = sessionRepository.findByDeviceId(deviceId);
    if(sessions.isEmpty()) {
      return null;
    }
    return sessions.getFirst();
  }

  @Override
  public void sendMessageToSession(String sessionId, String message) {
    WebSocketSession webSocketSession = sessions.get(sessionId);
    try {
      if (webSocketSession != null && webSocketSession.isOpen()) {
        webSocketSession.sendMessage(new TextMessage(message));
      } else {
        throw new IllegalStateException("WebSocket session is not open or does not exist.");
      }
    } catch (Exception e) {
      log.error("Failed to send message to session {}: {}", sessionId, e.getMessage());
      throw new RuntimeException("Failed to send message to session: " + e.getMessage(), e);
    }
  }

  private void removeSession(String sessionId) {
    try {
      WebSocketSession webSocketSession = sessions.get(sessionId);
      if (webSocketSession != null) {
        try {
          if (webSocketSession.isOpen()) {
            webSocketSession.close();
          }
        } catch (Exception closeEx) {
          log.warn("Error closing WebSocket for session {}: {}", sessionId, closeEx.getMessage());
        }
        sessions.remove(sessionId);
      } else {
        sessions.remove(sessionId);
      }
      sessionRepository.deleteById(sessionId);
    } catch (Exception e) {
      log.error("Error removing session {}: {}", sessionId, e.getMessage());
    }
  }

}
