package com.bravos.parentalcontrol.websocket.interceptor;

import com.bravos.parentalcontrol.service.SessionService;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
public class ConnectInterceptor implements HandshakeInterceptor {
  private final SessionService sessionService;

  public ConnectInterceptor(SessionService sessionService) {
    this.sessionService = sessionService;
  }

  @Override
  public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                 @NonNull ServerHttpResponse response,
                                 @NonNull WebSocketHandler wsHandler,
                                 @NonNull Map<String, Object> attributes) {
    String deviceId = request.getHeaders().getFirst("X-Device-Id");
    String deviceName = request.getHeaders().getFirst("X-Device-Name");
    String ipAddress = request.getHeaders().getFirst("X-Real-IP");

    if (deviceId == null || deviceName == null || ipAddress == null) {
      return false;
    }

    sessionService.deleteSessionsByDeviceId(deviceId);

    attributes.put("deviceId", deviceId);
    attributes.put("deviceName", deviceName);
    attributes.put("ipAddress", ipAddress);

    return true;
  }

  @Override
  public void afterHandshake(@NonNull ServerHttpRequest request,
                             @NonNull ServerHttpResponse response,
                             @NonNull WebSocketHandler wsHandler,
                             @Nullable Exception exception) {
    if (exception != null) {
      log.error("Handshake error: {}", exception.getMessage());
    }
  }
}
