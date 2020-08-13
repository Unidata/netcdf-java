package ucar.nc2.time;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.invoke.MethodHandles;

/** Test {@link ucar.nc2.time.CalendarDate} Constructors */
public class TestCalendarDateConstructors {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testCalendarDateOf() {
    assertThat(CalendarDate.of(null, 1, 1, 1, 1, 1, 1).toString()).isEqualTo("0001-01-01T01:01:01Z");
    assertThat(CalendarDate.of(null, 1, 1, 1, 0, 1, 1).toString()).isEqualTo("0001-01-01T00:01:01Z");
    assertThat(CalendarDate.of(null, 1, 1, 1, 1, 0, 1).toString()).isEqualTo("0001-01-01T01:00:01Z");
    assertThat(CalendarDate.of(null, 1, 1, 1, 1, 1, 0).toString()).isEqualTo("0001-01-01T01:01:00Z");
  }

  @Test
  public void testCalendarDateFail() {
    // Illegal month of year
    try {
      CalendarDate.of(null, 1, 0, 1, 1, 1, 1);
      fail();
    } catch (Exception e) {
      // expected
    }

    // dayOfMonth must be in the range [1,31]
    try {
      CalendarDate.of(null, 1, 1, 0, 1, 1, 1);
    } catch (Exception e) {
      System.out.printf("%s%n", e.getMessage());
      assert true;
    }
  }

  @Test
  public void testCalendarDateWithDoy() {
    // withDoy(Calendar cal, int year, int doy, int hourOfDay, int minuteOfHour, int secondOfMinute)
    assertThat(CalendarDate.withDoy(null, 1, 1, 1, 1, 1).toString()).isEqualTo("0001-01-01T01:01:01Z");

    assertThat(CalendarDate.withDoy(null, 1, 111, 1, 1, 1).toString()).isEqualTo("0001-04-21T01:01:01Z");
  }

  @Test
  public void testParseUdUnits() {
    assertThat(CalendarDate.parseUdunitsOrIso(null, "7 days since 0001-04-21T01:01:01Z").toString())
        .isEqualTo("0001-04-28T01:01:01Z");

    assertThat(CalendarDate.parseUdunitsOrIso(null, "0001-04-21T01:01:01Z").toString())
        .isEqualTo("0001-04-21T01:01:01Z");
  }

  @Test
  public void testCalendarDateFromDate() {
    java.util.Calendar cal = new GregorianCalendar(new SimpleTimeZone(0, "GMT"));
    cal.clear();
    cal.set(1999, 0, 1, 1, 1, 1);
    Date date = cal.getTime();

    CalendarDate expected = CalendarDate.of(null, 1999, 1, 1, 1, 1, 1);

    assertThat(date.getTime()).isEqualTo(expected.getMillis());
    assertThat(date.getTime()).isEqualTo(expected.toDate().getTime());
    assertThat(date).isEqualTo(expected.toDate());
    assertThat(toDateTimeStringISO(date)).isEqualTo(expected.toString());
  }

  private String toDateTimeStringISO(Date date) {
    SimpleDateFormat isoDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    isoDateTimeFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
    return isoDateTimeFormat.format(date) + "Z";
  }

}
