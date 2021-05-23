package ucar.nc2.time2;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ucar.units.TimeScaleUnit;
import ucar.units.Unit;
import ucar.units.UnitFormat;
import ucar.units.UnitFormatManager;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link CalendarDateUnit} */
@RunWith(Parameterized.class)
public class TestCalendarDateVsUdunits {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  UnitFormat format = UnitFormatManager.instance();

  /*
   * http://www.w3.org/TR/NOTE-datetime.html
   * Year:
   * YYYY (eg 1997)
   * Year and month:
   * YYYY-MM (eg 1997-07)
   * Complete date:
   * YYYY-MM-DD (eg 1997-07-16)
   * Complete date plus hours and minutes:
   * YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
   * Complete date plus hours, minutes and seconds:
   * YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
   * Complete date plus hours, minutes, seconds and a decimal fraction of a
   * second
   * YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
   */

  @Parameterized.Parameters(name = "{0}: {2}")
  public static Collection params() {
    Object[][] data = new Object[][] {{"W3CISO", null, "secs since 1997"}, {"W3CISO", null, "secs since 1997"},
        {"W3CISO", null, "secs since 1997-07"}, {"W3CISO", null, "secs since 1997-07-16"},
        {"W3CISO", null, "secs since 1997-07-16T19:20+01:00"}, {"W3CISO", null, "secs since 1997-07-16T19:20:30+01:00"},
        {"ChangeoverDate", null, "secs since 1997-01-01"}, {"ChangeoverDate", null, "secs since 1582-10-16"},
        {"ChangeoverDate", null, "secs since 1582-10-15"},
        // {"ChangeoverDate", null, "secs since 1582-10-01"},
        // {"ChangeoverDate", null, "secs since 1582-10-02"},
        // {"ChangeoverDate", null, "secs since 1582-10-03"},
        // {"ChangeoverDate", null, "secs since 1582-10-04"},
        // {"yearZero", null, "secs since 0000-01-01"},
        // {"yearZero", null, "secs since 0001-01-01"},
        // {"yearZero", null, "secs since -0001-01-01"},
        // {"yearZero", "gregorian", "secs since 0001-01-01"},
        // {"yearZero", "gregorian", "secs since -0001-01-01"},
        {"Problem", null, "days since 2008-01-01 0:00:00 00:00"},
        {"Problem", null, "seconds since 1968-05-23 00:00:00 UTC"},
        {"Problem", null, "seconds since 1970-01-01 00 UTC"},
        // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
        // {"UDUnitsSeconds", null, "secs since 1992-10-8 15:15:42.534"}, // udunit only accepts 2 decimals in seconds
        {"UDUnits", null, "secs since 1992-10-8 15:15:42.5 -6:00"},
        {"UDUnits", null, "secs since 1992-10-8 15:15:42.5 +6"}, {"UDUnits", null, "secs since 1992-10-8 15:15:42"},
        {"UDUnits", null, "secs since 1992-10-8 15:15"}, {"UDUnits", null, "secs since 1992-10-8 15"},
        {"UDUnits", null, "secs since 1992-10-8T15"}, {"UDUnits", null, "secs since 1992-10-8"},
        // {"UDUnits", null, "secs since 199-10-8"},
        // {"UDUnits", null, "secs since 19-10-8"},
        // {"UDUnits", null, "secs since 1-10-8"},
        // {"UDUnits", null, "secs since +1101-10-8"},
        // {"UDUnits", null, "secs since -1101-10-8"},
        {"UDUnits", null, "secs since 1992-10-8T7:00 -6:00"}, {"UDUnits", null, "secs since 1992-10-8T7:00 +6:00"},
        {"UDUnits", null, "secs since 1992-10-8T7 -6:00"}, {"UDUnits", null, "secs since 1992-10-8T7 +6:00"},
        {"UDUnits", null, "secs since 1992-10-8 7 -6:00"}, {"UDUnits", null, "secs since 1992-10-8 7 +6:00"},
        {"UDUnits", null, "days since 1992"}, {"UDUnits", null, "hours since 2011-02-09T06:00:00Z"},
        {"UDUnits", null, "seconds since 1968-05-23 00:00:00"},
        {"UDUnits", null, "seconds since 1968-05-23 00:00:00 UTC"},
        {"UDUnits", null, "seconds since 1968-05-23 00:00:00 GMT"},
        {"UDUnits", null, "msecs since 1970-01-01T00:00:00Z"},};
    return Arrays.asList(data);
  }

  @Parameterized.Parameter
  public String category;

  @Parameterized.Parameter(value = 1)
  public String calendar;

  @Parameterized.Parameter(value = 2)
  public String datestring;

  @Test
  public void testBase() throws Exception {
    CalendarDate base = CalendarDate.of(getUdunitBase(datestring));
    Calendar cal = Calendar.get(calendar).orElse(Calendar.getDefault());
    CalendarDateUnit cdu = CalendarDateUnit.fromUdunitString(cal, datestring).orElseThrow();
    assertThat(cdu.getBaseDateTime()).isEqualTo(base);
  }

  private Date getUdunitBase(String s) throws Exception {
    Unit u = format.parse(s);
    assertThat(u).isInstanceOf(TimeScaleUnit.class);
    TimeScaleUnit tu = (TimeScaleUnit) u;
    return tu.getOrigin();
  }

}
