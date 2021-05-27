package ucar.nc2.time2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CalendarDateFormatter} */
public class TestCalendarDateFormatter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBasics() {
    String isoDate = "1950-01-01T12:34:56.123";
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, isoDate).orElseThrow();

    System.out.printf("%s%n", cd);
    System.out.printf("  toDateTimeStringISO=%s%n", CalendarDateFormatter.toDateTimeStringISO(cd));
    System.out.printf("toDateTimeStringShort=%s%n", CalendarDateFormatter.toDateTimeString(cd));
    System.out.printf("        toDateString=%s%n", CalendarDateFormatter.toDateString(cd));
    System.out.printf("===============================%n");
    Date d = cd.toDate();
    System.out.printf("cd.toDate()=%s%n", d);

    SimpleDateFormat udunitDF = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
    udunitDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    udunitDF.applyPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'");
    System.out.printf("           udunitDF=%s%n", udunitDF.format(d));

    assertThat(CalendarDateFormatter.toDateTimeStringISO(cd)).isEqualTo("1950-01-01T12:34:56.123Z");
    assertThat(CalendarDateFormatter.toDateTimeStringISO(0)).isEqualTo(CalendarDate.unixEpoch.toString());
    assertThat(CalendarDateFormatter.toDateTimeString(cd)).isEqualTo("1950-01-01T12:34:56Z");
    assertThat(CalendarDateFormatter.toDateString(cd)).isEqualTo("1950-01-01");

    assertThat(CalendarDateFormatter.toDateTimeStringISO(d)).isEqualTo("1950-01-01T12:34:56.123Z");
    assertThat(udunitDF.format(d)).isEqualTo("1950-01-01 12:34:56.123 UTC");
  }

  @Test
  public void testCustom() {
    CalendarDateFormatter cdf = new CalendarDateFormatter("yyyy-MM-dd");
    String isoDate = "1950-01-01T12:34:56.123";
    CalendarDate cd = CalendarDate.fromUdunitIsoDate(null, isoDate).orElseThrow();

    assertThat(cdf.toString(cd)).isEqualTo("1950-01-01");
    assertThat(cdf.parse("1950-01-01")).isNotNull();
  }

  @Test
  public void testParseDateString() {
    String dateOnly = "1950-01-01";
    CalendarDate cd = CalendarDateFormatter.parseDateString(dateOnly);
    assertThat(CalendarDateFormatter.toDateString(cd)).isEqualTo("1950-01-01");
  }


}
