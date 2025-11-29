package com.bravos.parentalcontrol.service.impl;

import com.bravos.parentalcontrol.model.Session;
import com.bravos.parentalcontrol.service.SessionService;
import com.bravos.parentalcontrol.service.AccessService;
import com.bravos.parentalcontrol.utils.DateTimeHelper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AccessServiceImpl implements AccessService {

  private final RedisTemplate<Object, Object> redisTemplate;
  private final SessionService sessionService;

  public AccessServiceImpl(RedisTemplate<Object, Object> redisTemplate, SessionService sessionService) {
    this.redisTemplate = redisTemplate;
    this.sessionService = sessionService;
  }

  @Override
  public String grantTime(String deviceId, int seconds) {
    Session session = sessionService.getSessionByDeviceId(deviceId);
    if(session == null) {
      throw new IllegalArgumentException("No active session for device: " + deviceId);
    }
    String code = String.valueOf((int)(Math.random() * 900000) + 100000);
    String key = "time_grant:" + session.getId() + ":" + code;
    Long expiration = DateTimeHelper.currentTimeMillis() + seconds * 1000L;
    redisTemplate.opsForValue().set(key, expiration, Duration.ofSeconds(seconds));
    return code;
  }

  @Override
  public void blockAfterTime(String deviceId, int seconds) {
    Session session = sessionService.getSessionByDeviceId(deviceId);
    if(session == null) {
      throw new IllegalArgumentException("No active session for device: " + deviceId);
    }
    String key = "block_device:" + deviceId;
    redisTemplate.opsForValue().set(key, seconds, Duration.ofSeconds(seconds));
    sessionService.sendMessageToSession(session.getId(), "BLOCK:" + seconds);
  }

  @Override
  public Integer getRemainingBlockTime(String deviceId) {
    var value = redisTemplate.opsForValue().get("block_device:" + deviceId);
    if(value != null) {
      return (Integer) value;
    }
    return null;
  }

  @Override
  public Integer verifyAccessRequest(String sessionId, String code) {
    var value = redisTemplate.opsForValue().get("time_grant:" + sessionId + ":" + code);
    if(value != null) {
      long remainingMillis = (Long) value - DateTimeHelper.currentTimeMillis();
      if(remainingMillis > 0) {
        return (int)(remainingMillis / 1000);
      }
    }
    return null;
  }

}
