package com.bravos.parentalcontrol.configuration;

import com.bravos.parentalcontrol.utils.Snowflake;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SnowflakeConfiguration {

  @Bean
  public Snowflake snowflake() {
    return new Snowflake(1);
  }

}
