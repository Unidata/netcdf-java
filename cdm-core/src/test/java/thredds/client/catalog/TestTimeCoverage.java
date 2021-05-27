/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.client.catalog;

import org.junit.Test;
import ucar.nc2.time2.CalendarDate;

import java.text.ParseException;
import java.util.Calendar;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link TimeCoverage} */
public class TestTimeCoverage {
  /**
   * Check if start and end dates change over time for a TimeCoverage with start set to "present" and a duration set.
   */
  @Test
  public void testStartPresentAndDuration() {
    TimeCoverage drStartIsPresent;
    try {
      drStartIsPresent = TimeCoverage.create(DateType.parse("present"), null, TimeDuration.parse("P7D"), null);
    } catch (ParseException e) {
      assertWithMessage("Failed to parse \"present\" and/or \"P7D\": " + e.getMessage()).fail();
      return;
    }
    checkValuesAfterDelay(drStartIsPresent);
  }

  /**
   * Check if start and end dates change over time for a TimeCoverage with end set to "present" and a duration set.
   */
  @Test
  public void testEndPresentAndDuration() {
    TimeCoverage drEndIsPresent;
    try {
      drEndIsPresent = TimeCoverage.create(null, DateType.parse("present"), TimeDuration.parse("P7D"), null);
    } catch (ParseException e) {
      assertWithMessage("Failed to parse \"present\" and/or \"P7D\": " + e.getMessage()).fail();
      return;
    }
    checkValuesAfterDelay(drEndIsPresent);
  }

  private void checkValuesAfterDelay(TimeCoverage dr) {
    long d = Calendar.getInstance().getTimeInMillis();
    CalendarDate startDate = dr.getStart().getCalendarDate();
    CalendarDate endDate = dr.getEnd().getCalendarDate();
    System.out.println("Current : " + d);
    System.out.println("Start   :  [" + startDate + "].");
    System.out.println("End     :  [" + endDate + "].");

    try {
      synchronized (this) {
        boolean cond = false;
        while (!cond) {
          this.wait(10);
          cond = true;
        }
      }
    } catch (InterruptedException e) {
      assertWithMessage("Failed to wait: " + e.getMessage()).fail();
      return;
    }

    long d2 = Calendar.getInstance().getTimeInMillis();
    CalendarDate startDate2 = dr.getStart().getCalendarDate();
    CalendarDate endDate2 = dr.getEnd().getCalendarDate();
    System.out.println("\nCurrent : " + d2);
    System.out.println("Start   : [" + startDate2 + "].");
    System.out.println("End     : [" + endDate2 + "].");

    assertThat(startDate).isNotEqualTo(startDate2);
    assertThat(endDate).isNotEqualTo(endDate2);
  }

  /*
   * LOOK
   * 
   * @Test
   * public void testDuration() throws ParseException {
   * TimeCoverage tc =
   * TimeCoverage.create(DateType.parse("2005-05-12 00:52:56"), DateType.parse("2005-05-14 12:52:56"), null, null);
   * assertThat(tc.getResolution()).isNull();
   * assertThat(tc.getDuration()).isEqualTo(TimeDuration.parse("60.0 hours"));
   * }
   */
}
