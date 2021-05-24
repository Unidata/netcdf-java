/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time2.chrono;

import java.time.chrono.AbstractChronology;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.Era;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.ValueRange;
import java.util.List;

public class ThreeSixtyDayChronology extends AbstractChronology {
  static ThreeSixtyDayChronology INSTANCE = new ThreeSixtyDayChronology();

  @Override
  public String getId() {
    return null;
  }

  @Override
  public String getCalendarType() {
    return null;
  }

  @Override
  public ChronoLocalDate date(int prolepticYear, int month, int dayOfMonth) {
    return null;
  }

  @Override
  public ChronoLocalDate dateYearDay(int prolepticYear, int dayOfYear) {
    return null;
  }

  @Override
  public ChronoLocalDate dateEpochDay(long epochDay) {
    return null;
  }

  @Override
  public ChronoLocalDate date(TemporalAccessor temporal) {
    return null;
  }

  @Override
  public boolean isLeapYear(long prolepticYear) {
    return false;
  }

  @Override
  public int prolepticYear(Era era, int yearOfEra) {
    return 0;
  }

  @Override
  public Era eraOf(int eraValue) {
    return null;
  }

  @Override
  public List<Era> eras() {
    return null;
  }

  @Override
  public ValueRange range(ChronoField field) {
    return null;
  }
}
