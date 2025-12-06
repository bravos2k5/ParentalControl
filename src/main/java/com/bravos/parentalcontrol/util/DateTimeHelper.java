package com.bravos.parentalcontrol.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class DateTimeHelper {
  private static final ZoneId DEFAULT_ZONE = ZoneId.systemDefault();
  private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

  private DateTimeHelper() {
  }

  public static Instant toInstant(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis);
  }

  public static LocalDateTime toLocalDateTime(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(DEFAULT_ZONE).toLocalDateTime();
  }

  public static OffsetDateTime toOffsetDateTime(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atOffset(ZoneOffset.UTC);
  }

  public static ZonedDateTime toZonedDateTime(long epochMillis) {
    return Instant.ofEpochMilli(epochMillis).atZone(DEFAULT_ZONE);
  }

  public static Date toDate(long epochMillis) {
    return new Date(epochMillis);
  }

  public static String toISOString(long epochMillis) {
    return ISO_FORMATTER.format(toInstant(epochMillis).atZone(ZoneOffset.UTC));
  }

  public static long from(Instant instant) {
    return instant.toEpochMilli();
  }

  public static long from(LocalDateTime localDateTime) {
    return localDateTime.atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
  }

  public static long from(ZonedDateTime zonedDateTime) {
    return zonedDateTime.toInstant().toEpochMilli();
  }

  public static long from(OffsetDateTime offsetDateTime) {
    return offsetDateTime.toInstant().toEpochMilli();
  }

  public static long from(Date date) {
    return date.getTime();
  }

  public static long currentTimeMillis() {
    return Instant.now().atZone(DEFAULT_ZONE).toInstant().toEpochMilli();
  }
}
