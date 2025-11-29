package com.bravos.parentalcontrol.socket.handler;

import com.bravos.parentalcontrol.model.NewSessionRequest;
import com.bravos.parentalcontrol.model.Session;
import com.bravos.parentalcontrol.service.AccessService;
import com.bravos.parentalcontrol.service.SessionService;
import com.bravos.parentalcontrol.utils.DateTimeHelper;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ControlHandler extends TextWebSocketHandler {

  private final SessionService sessionService;
  private final AccessService accessService;

  private final Map<String, Thread> pingThreads = new ConcurrentHashMap<>();

  public ControlHandler(SessionService sessionService, AccessService accessService) {
    this.sessionService = sessionService;
    this.accessService = accessService;
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session,
                                   @NonNull TextMessage message) throws IOException {
    String content = message.getPayload();
    String sessionId = session.getId();
    if(content.startsWith("PASSWORD:")) {
      String password = content.substring(9);
      Integer timeGranted = accessService.verifyAccessRequest(sessionId, password);
      if(timeGranted != null) {
        session.sendMessage(new TextMessage("GRANTED:" + timeGranted));
        Thread pingThread = pingThreads.get(sessionId);
        if(pingThread != null) {
          pingThread.interrupt();
          pingThreads.remove(sessionId);
        }
        pingThread = startPingThread(session);
        pingThreads.put(sessionId, pingThread);
      } else {
        session.sendMessage(new TextMessage("DENIED"));
      }
    } else if (content.startsWith("ping")) {
      session.sendMessage(new TextMessage("pong"));
    } else if (content.startsWith("BLOCKED")) {
      pingThreads.computeIfPresent(sessionId, (k, v) -> {
        v.interrupt();
        return null;
      });
      pingThreads.remove(sessionId);
    } else {
      session.sendMessage(new TextMessage("UNKNOWN_COMMAND"));
    }
  }

  @Override
  protected void handlePongMessage(@NonNull WebSocketSession session,
                                   @NonNull PongMessage message) {
    long now = DateTimeHelper.currentTimeMillis();
    sessionService.updateLastActive(session.getId(), now);
  }

  @Override
  public void afterConnectionEstablished(@NonNull WebSocketSession session) {
    NewSessionRequest request = NewSessionRequest.builder()
        .id(session.getId())
        .deviceId((String) session.getAttributes().get("deviceId"))
        .deviceName((String) session.getAttributes().get("deviceName"))
        .ipAddress((String) session.getAttributes().get("ipAddress"))
        .build();
    Session newSession = sessionService.createNewSession(request, session);
    log.info("New session established: {}", newSession.getId());
  }

  @Override
  public void afterConnectionClosed(@NonNull WebSocketSession session,
                                    @NonNull CloseStatus status) {
    pingThreads.computeIfPresent(session.getId(), (k, v) -> {
      v.interrupt();
      return null;
    });
    pingThreads.remove(session.getId());
    sessionService.deleteSession(session.getId());
    log.info("Session closed: {}", session.getId());
  }

  private Thread startPingThread(WebSocketSession session) {
    return Thread.startVirtualThread(() -> {
      try {
        while (session.isOpen()) {
          session.sendMessage(new PingMessage());
          Thread.sleep(30000);
        }
      } catch (IOException | InterruptedException e) {
        log.info("Ping thread stopped for session: {}", session.getId());
      }
    });
  }

}
