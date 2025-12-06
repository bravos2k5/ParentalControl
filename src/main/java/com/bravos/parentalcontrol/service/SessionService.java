package com.bravos.parentalcontrol.service;

import com.bravos.parentalcontrol.dto.request.NewSessionRequest;
import com.bravos.parentalcontrol.entity.Session;
import com.bravos.parentalcontrol.repository.SessionRepository;
import com.bravos.parentalcontrol.util.DateTimeHelper;
import com.bravos.parentalcontrol.websocket.WebSocketSessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

@Slf4j
@Service
public class SessionService {

  private final SessionRepository sessionRepository;
  private final WebSocketSessionManager webSocketSessionManager;

  public SessionService(SessionRepository sessionRepository, WebSocketSessionManager webSocketSessionManager) {
    this.sessionRepository = sessionRepository;
    this.webSocketSessionManager = webSocketSessionManager;
  }

  public Session createNewSession(NewSessionRequest request, WebSocketSession webSocketSession) {
    webSocketSessionManager.register(request.getId(), webSocketSession);
    Session session = Session.builder()
        .id(request.getId())
        .deviceName(request.getDeviceName())
        .deviceId(request.getDeviceId())
        .ipAddress(request.getIpAddress())
        .createdAt(DateTimeHelper.currentTimeMillis())
        .build();
    return sessionRepository.save(session);
  }

  public List<Session> getAllSessions() {
    List<Session> sessions = sessionRepository.findAll();
    sessions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
    return sessions;
  }

  public void deleteSession(String sessionId) {
    webSocketSessionManager.remove(sessionId);
    sessionRepository.deleteById(sessionId);
  }

  public void deleteAllSessions() {
    List<Session> sessions = getAllSessions();
    for (Session session : sessions) {
      deleteSession(session.getId());
    }
  }

  public void deleteSessionsByDeviceId(String deviceId) {
    List<Session> sessions = sessionRepository.findByDeviceId(deviceId);
    for (Session session : sessions) {
      deleteSession(session.getId());
    }
  }

  public void updateLastActive(String sessionId, Long lastActiveTime) {
    if (lastActiveTime == null) {
      lastActiveTime = DateTimeHelper.currentTimeMillis();
    }
    Session session = sessionRepository.findById(sessionId).orElse(null);
    if (session != null) {
      session.setLastActive(lastActiveTime);
      sessionRepository.save(session);
    }
  }

  public Session getSessionByDeviceId(String deviceId) {
    List<Session> sessions = sessionRepository.findByDeviceId(deviceId);
    if (sessions.isEmpty()) {
      return null;
    }
    return sessions.getFirst();
  }

  public void sendMessageToSession(String sessionId, String message) {
    webSocketSessionManager.sendMessage(sessionId, message);
  }

}
