/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.calendar;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;
import ucar.nc2.calendar.CalendarPeriod.Field;

public class TestCalendarDateRange {

  @Test
  public void testBasics() {
    CalendarDate start = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range = CalendarDateRange.of(start, 3600);

    CalendarDate end = start.add(1, Field.Hour);
    CalendarDateRange range2 = CalendarDateRange.of(start, end);

    assertThat(range2).isEqualTo(range);
    assertThat(range2.hashCode()).isEqualTo(range.hashCode());

    assertThat(range.getEnd()).isEqualTo(end);
  }

  @Test
  public void testIntersect() {
    CalendarDate start1 = CalendarDate.of(2020, 7, 17, 11, 11, 11);
    CalendarDateRange range1 = CalendarDateRange.of(start1, 3600);

    CalendarDate start2 = start1.add(1, Field.Hour);
    CalendarDateRange range2 = CalendarDateRange.of(start2, 3600);

    assertThat(range1.intersects(range1)).isTrue();
    assertThat(range1.intersect(range1).getDurationInSecs()).isEqualTo(3600);
    assertThat(range1.intersects(range2)).isTrue();
    assertThat(range1.intersect(range2).getDurationInSecs()).isEqualTo(0);

    CalendarDate start3 = start2.add(2, Field.Second);
    CalendarDateRange range3 = CalendarDateRange.of(start3, 3600);
    assertThat(range3.intersects(range1)).isFalse();
    assertThat(range3.intersects(range2)).isTrue();
    assertThat(range3.intersect(range2).getDurationInSecs()).isEqualTo(3598);
  }
}
