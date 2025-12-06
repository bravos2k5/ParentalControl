package com.bravos.parentalcontrol.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.protocol.ProtocolVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@Configuration
@EnableRedisRepositories("com.bravos.parentalcontrol.repository")
public class RedisConfig {
  @Bean
  public RedisConnectionFactory connectionFactory() {
    ClientOptions options = ClientOptions.builder()
        .protocolVersion(ProtocolVersion.RESP2)
        .pingBeforeActivateConnection(true)
        .build();
    LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
        .clientOptions(options)
        .build();
    RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration();
    redisConfig.setHostName(System.getenv("REDIS_HOST"));
    redisConfig.setPort(Integer.parseInt(System.getenv("REDIS_PORT")));
    redisConfig.setPassword(System.getenv("REDIS_PASSWORD"));
    return new LettuceConnectionFactory(redisConfig, clientConfiguration);
  }

  @Bean
  public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
    RedisTemplate<byte[], byte[]> template = new RedisTemplate<>();
    template.setConnectionFactory(redisConnectionFactory);
    return template;
  }
}
