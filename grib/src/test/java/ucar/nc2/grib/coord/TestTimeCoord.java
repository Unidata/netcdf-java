package ucar.nc2.grib.coord;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;

@RunWith(JUnit4.class)
public class TestTimeCoord {

  @Test
  public void testTinvDate() {
    CalendarDate start = CalendarDate.of(1269820799000L);
    CalendarDate end = CalendarDate.of(1269824399000L);
    TimeCoordIntvDateValue tinvDate = new TimeCoordIntvDateValue(start, end);
    System.out.printf("tinvDate = %s%n", tinvDate);
    assertEquals("(2010-03-28T23:59:59Z,2010-03-29T00:59:59Z)", tinvDate.toString());

    CalendarDate refDate = CalendarDate.of(1269820800000L);
    CalendarPeriod timeUnit = CalendarPeriod.of("Hour");

    TimeCoordIntvValue tinv = tinvDate.convertReferenceDate(refDate, timeUnit);
    System.out.printf("tinv = %s offset from %s%n", tinv, refDate);
    assertEquals("2010-03-29T00:00:00Z", refDate.toString());
  }

  @Test
  public void shouldPreserveTimeIntervalLengthWithStartAfterRefDate() {
    final CalendarDate start = CalendarDate.parseISOformat(null, "2022-08-16T01:00:00Z");
    final CalendarDate end = CalendarDate.parseISOformat(null, "2022-08-16T12:00:00Z");
    final TimeCoordIntvDateValue timeCoordIntvDateValue = new TimeCoordIntvDateValue(start, end);

    final CalendarDate refDate = CalendarDate.parseISOformat(null, "2022-08-16T00:30:00Z");
    final CalendarPeriod timeUnit = CalendarPeriod.of("Hour");

    final TimeCoordIntvValue timeCoordIntvValue = timeCoordIntvDateValue.convertReferenceDate(refDate, timeUnit);
    assertThat(timeCoordIntvValue.getBounds1()).isEqualTo(0);
    assertThat(timeCoordIntvValue.getBounds2()).isEqualTo(11);
    assertThat(timeCoordIntvValue.getIntervalSize()).isEqualTo(11);
  }

  @Test
  public void shouldPreserveTimeIntervalLengthWithStartBeforeRefDate() {
    final CalendarDate start = CalendarDate.parseISOformat(null, "2022-08-16T01:00:00Z");
    final CalendarDate end = CalendarDate.parseISOformat(null, "2022-08-16T12:00:00Z");
    final TimeCoordIntvDateValue timeCoordIntvDateValue = new TimeCoordIntvDateValue(start, end);

    final CalendarDate refDate = CalendarDate.parseISOformat(null, "2022-08-16T01:30:00Z");
    final CalendarPeriod timeUnit = CalendarPeriod.of("Hour");

    final TimeCoordIntvValue timeCoordIntvValue = timeCoordIntvDateValue.convertReferenceDate(refDate, timeUnit);
    assertThat(timeCoordIntvValue.getBounds1()).isEqualTo(-1);
    assertThat(timeCoordIntvValue.getBounds2()).isEqualTo(10);
    assertThat(timeCoordIntvValue.getIntervalSize()).isEqualTo(11);
  }
}

