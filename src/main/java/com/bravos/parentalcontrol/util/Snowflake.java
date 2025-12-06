package com.bravos.parentalcontrol.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class Snowflake {
  private static final long MACHINE_ID_BITS = 10;
  private static final long SEQUENCE_BITS = 12;
  private static final long TIME_STAMP_BITS = 41;
  private static final long MACHINE_ID_SHIFT = SEQUENCE_BITS + TIME_STAMP_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS;
  private static final long DEFAULT_EPOCH = LocalDateTime.of(2025, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli();
  private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
  private final long machineId;
  private final long epoch;
  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public Snowflake(long machineId) {
    this(machineId, DEFAULT_EPOCH);
  }

  public Snowflake(long machineId, long customEpoch) {
    this.epoch = customEpoch;
    if (machineId < 0 || ((machineId > (1L << MACHINE_ID_BITS) - 1))) {
      throw new IllegalArgumentException("Machine ID must be between 0 and " + ((1L << MACHINE_ID_BITS) - 1));
    }
    this.machineId = machineId;
  }

  private long waitForNextMillis() {
    long currentTimeMillis = DateTimeHelper.currentTimeMillis();
    while (currentTimeMillis <= lastTimestamp) {
      currentTimeMillis = DateTimeHelper.currentTimeMillis();
    }
    return currentTimeMillis;
  }

  public synchronized long next() {
    long currentTimestamp = DateTimeHelper.currentTimeMillis();
    if (currentTimestamp < lastTimestamp) {
      throw new IllegalStateException("Clock moved backwards. Refusing to generate id for " +
          (lastTimestamp - currentTimestamp) + " milliseconds");
    }
    long timestamp = currentTimestamp - epoch;
    if (currentTimestamp != lastTimestamp) {
      sequence = 0L;
      lastTimestamp = currentTimestamp;
    } else if (sequence >= SEQUENCE_MASK) {
      long nextMillis = waitForNextMillis();
      timestamp = nextMillis - epoch;
      sequence = 0L;
      lastTimestamp = nextMillis;
    } else {
      ++sequence;
    }
    long timestampShifted = timestamp << TIMESTAMP_SHIFT;
    long machineIdShifted = machineId << MACHINE_ID_SHIFT;
    return timestampShifted | machineIdShifted | sequence;
  }

  public long extractTimestamp(long id) {
    return ((id >> TIMESTAMP_SHIFT) &
        ((1L << TIME_STAMP_BITS) - 1)) + epoch;
  }

  public long extractMachineId(long id) {
    return (id >> MACHINE_ID_SHIFT) &
        ((1L << MACHINE_ID_BITS) - 1);
  }
}
