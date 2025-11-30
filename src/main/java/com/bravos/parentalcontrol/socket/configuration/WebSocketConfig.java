package com.bravos.parentalcontrol.socket.configuration;

import com.bravos.parentalcontrol.socket.handler.ControlHandler;
import com.bravos.parentalcontrol.socket.intercepter.ConnectInterceptor;
import lombok.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

  private final ConnectInterceptor connectInterceptor;
  private final ControlHandler controlHandler;

  public WebSocketConfig(ConnectInterceptor connectInterceptor, ControlHandler controlHandler) {
    this.connectInterceptor = connectInterceptor;
    this.controlHandler = controlHandler;
  }

  @Override
  public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
    registry.addHandler(controlHandler, "/ws/**")
        .setAllowedOrigins("*")
        .addInterceptors(connectInterceptor)
    ;
  }
}
