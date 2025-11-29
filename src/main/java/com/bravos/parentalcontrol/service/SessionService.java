package com.bravos.parentalcontrol.service;

import com.bravos.parentalcontrol.model.NewSessionRequest;
import com.bravos.parentalcontrol.model.Session;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;

public interface SessionService {

  Session createNewSession(NewSessionRequest request, WebSocketSession webSocketSession);

  List<Session> getAllSessions();

  void deleteSession(String sessionId);

  void deleteAllSessions();

  void deleteSessionsByDeviceId(String deviceId);

  void updateLastActive(String sessionId, Long lastActiveTime);

  Session getSessionByDeviceId(String deviceId);

  void sendMessageToSession(String sessionId, String message);

}
