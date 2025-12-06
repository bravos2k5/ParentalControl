package com.bravos.parentalcontrol.config;

import com.bravos.parentalcontrol.util.Snowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
public class AppConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public ServletServerContainerFactoryBean createWebSocketContainer() {
    ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
    container.setMaxTextMessageBufferSize(128 * 1024);
    container.setMaxBinaryMessageBufferSize(128 * 1024);
    return container;
  }

  @Bean
  public Snowflake snowflake() {
    return new Snowflake(1);
  }
}
