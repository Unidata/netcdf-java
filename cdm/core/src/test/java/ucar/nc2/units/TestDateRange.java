/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.units;

import com.google.common.truth.Truth;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * test that DateRange moves when start or end is "present".
 *
 * @author edavis
 * @since 4.0
 */
public class TestDateRange {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /**
   * Check if start and end dates change over time for a DateRange with start set to "present" and a duration set.
   */
  @Test
  public void testStartPresentAndDuration() {
    DateRange drStartIsPresent;
    try {
      drStartIsPresent = new DateRange(new DateType("present", null, null), null, new TimeDuration("P7D"), null);
    } catch (ParseException e) {
      Truth.assertWithMessage("Failed to parse \"present\" and/or \"P7D\": " + e.getMessage()).fail();
      return;
    }
    checkValuesAfterDelay(drStartIsPresent);
  }

  /**
   * Check if start and end dates change over time for a DateRange with end set to "present" and a duration set.
   */
  @Test
  public void testEndPresentAndDuration() {
    DateRange drEndIsPresent;
    try {
      drEndIsPresent = new DateRange(null, new DateType("present", null, null), new TimeDuration("P7D"), null);
    } catch (ParseException e) {
      Truth.assertWithMessage("Failed to parse \"present\" and/or \"P7D\": " + e.getMessage()).fail();
      return;
    }
    checkValuesAfterDelay(drEndIsPresent);
  }

  private void checkValuesAfterDelay(DateRange dr) {
    long d = Calendar.getInstance().getTimeInMillis();
    Date startDate = dr.getStart().getDate();
    Date endDate = dr.getEnd().getDate();
    System.out.println("Current : " + d);
    System.out.println("Start   :  [" + startDate.getTime() + "].");
    System.out.println("End     :  [" + endDate.getTime() + "].");

    try {
      synchronized (this) {
        boolean cond = false;
        while (!cond) {
          this.wait(10);
          cond = true;
        }
      }
    } catch (InterruptedException e) {
      Truth.assertWithMessage("Failed to wait: " + e.getMessage()).fail();
      return;
    }

    long d2 = Calendar.getInstance().getTimeInMillis();
    Date startDate2 = dr.getStart().getDate();
    Date endDate2 = dr.getEnd().getDate();
    System.out.println("\nCurrent : " + d2);
    System.out.println("Start   : [" + startDate2.getTime() + "].");
    System.out.println("End     : [" + endDate2.getTime() + "].");

    Truth.assertThat(startDate).isNotEqualTo(startDate2);
    Truth.assertThat(endDate).isNotEqualTo(endDate2);
  }
}
