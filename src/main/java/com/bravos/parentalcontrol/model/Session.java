package com.bravos.parentalcontrol.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.index.Indexed;

import static lombok.AccessLevel.PRIVATE;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = PRIVATE)
@RedisHash("session")
public class Session {

  @Id
  String id;

  String deviceName;

  @Indexed
  String deviceId;

  String ipAddress;

  Long createdAt;

  Long lastActive;

}
