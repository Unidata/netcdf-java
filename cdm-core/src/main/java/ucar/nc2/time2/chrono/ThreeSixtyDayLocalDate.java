/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.time2.chrono;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

public class ThreeSixtyDayLocalDate implements ChronoLocalDate {

  @Override
  public Chronology getChronology() {
    return ThreeSixtyDayChronology.INSTANCE;
  }

  @Override
  public int lengthOfMonth() {
    return 30;
  }

  @Override
  public long until(Temporal endExclusive, TemporalUnit unit) {
    return 0;
  }

  @Override
  public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
    return null;
  }

  @Override
  public long getLong(TemporalField field) {
    return 0;
  }
}
