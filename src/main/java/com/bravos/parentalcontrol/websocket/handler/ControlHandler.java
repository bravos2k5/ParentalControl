package com.bravos.parentalcontrol.websocket.handler;

import com.bravos.parentalcontrol.dto.request.NewSessionRequest;
import com.bravos.parentalcontrol.entity.Session;
import com.bravos.parentalcontrol.service.AccessService;
import com.bravos.parentalcontrol.service.SessionService;
import com.bravos.parentalcontrol.util.DateTimeHelper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class ControlHandler extends TextWebSocketHandler {
  private final SessionService sessionService;
  private final AccessService accessService;
  private final Map<String, ScheduledFuture<?>> pingTasks = new ConcurrentHashMap<>();
  private final ScheduledExecutorService pingScheduler = Executors.newSingleThreadScheduledExecutor();
  private final ExecutorService virtualExecutor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

  public ControlHandler(SessionService sessionService, AccessService accessService) {
    this.sessionService = sessionService;
    this.accessService = accessService;
  }

  @Override
  protected void handleTextMessage(@NonNull WebSocketSession session,
                                   @NonNull TextMessage message) throws IOException {
    String content = message.getPayload();
    if (content.startsWith("PASSWORD:")) {
      this.checkPasswordHandler(session, content);
    } else if (content.equalsIgnoreCase("ping")) {
      session.sendMessage(new TextMessage("pong"));
    } else if (content.startsWith("BLOCKED")) {
      this.blockedHandler(session);
    } else {
      session.sendMessage(new TextMessage("UNKNOWN_COMMAND"));
    }
    log.info("Received message from session {}: {}", session.getId(), content);
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
    ScheduledFuture<?> future = pingTasks.remove(session.getId());
    if (future != null) {
      future.cancel(true);
    }
    sessionService.deleteSession(session.getId());
    log.info("Session closed: {}", session.getId());
  }

  private void checkPasswordHandler(WebSocketSession session, String content) throws IOException {
    String sessionId = session.getId();
    String password = content.substring(9);
    Integer timeGranted = accessService.verifyAccessRequest(sessionId, password);
    if (timeGranted != null) {
      session.sendMessage(new TextMessage("GRANTED:" + timeGranted));
      ScheduledFuture<?> existing = pingTasks.remove(sessionId);
      if (existing != null) {
        existing.cancel(true);
      }
      ScheduledFuture<?> future = startPingTask(session);
      pingTasks.put(sessionId, future);
    } else {
      session.sendMessage(new TextMessage("DENIED"));
    }
  }

  private void blockedHandler(WebSocketSession session) {
    String sessionId = session.getId();
    ScheduledFuture<?> future = pingTasks.remove(sessionId);
    if (future != null) {
      future.cancel(true);
    }
    pingTasks.remove(sessionId);
  }

  private ScheduledFuture<?> startPingTask(WebSocketSession session) {
    return pingScheduler.scheduleAtFixedRate(() -> {
      if (!session.isOpen()) {
        ScheduledFuture<?> f = pingTasks.remove(session.getId());
        if (f != null) f.cancel(false);
        return;
      }
      virtualExecutor.execute(() -> {
        try {
          if (!session.isOpen()) return;
          session.sendMessage(new PingMessage());
        } catch (IOException e) {
          log.info("Ping failed for session {}, cancelling task: {}", session.getId(), e.getMessage());
          ScheduledFuture<?> f = pingTasks.remove(session.getId());
          if (f != null) f.cancel(true);
        } catch (Throwable t) {
          log.warn("Unexpected error in ping task for session {}: {}", session.getId(), t.getMessage());
          ScheduledFuture<?> f = pingTasks.remove(session.getId());
          if (f != null) f.cancel(true);
        }
      });
    }, 0, 60, TimeUnit.SECONDS);
  }

  @PreDestroy
  public void shutdownScheduler() {
    pingScheduler.shutdownNow();
    virtualExecutor.shutdownNow();
  }
}
