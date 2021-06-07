package ucar.nc2.calendar;

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

/**
 * Test {@link CalendarDateUnit}
 * These are all testing difference of mixed julian/gregorian vs proleptic gregorian.
 * LOOK Currently failing.
 */
@RunWith(Parameterized.class)
public class TestCalendarDateMixedGregorian {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  UnitFormat format = UnitFormatManager.instance();

  @Parameterized.Parameters(name = "{0}: {2}")
  public static Collection params() {
    Object[][] data = new Object[][] {{"ChangeoverDate", null, "secs since 1582-10-01"},
        {"ChangeoverDate", null, "secs since 1582-10-02"}, {"ChangeoverDate", null, "secs since 1582-10-03"},
        {"ChangeoverDate", null, "secs since 1582-10-04"}, {"yearZero", null, "secs since 0000-01-01"},
        {"yearZero", null, "secs since 0001-01-01"}, {"yearZero", null, "secs since -0001-01-01"},
        {"yearZero", "gregorian", "secs since 0001-01-01"}, {"yearZero", "gregorian", "secs since -0001-01-01"},
        // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
        {"UDUnitsSeconds", null, "secs since 1992-10-8 15:15:42.534"}, // udunit only accepts 2 decimals in seconds
        {"UDUnits", null, "secs since 199-10-8"}, {"UDUnits", null, "secs since 19-10-8"},
        {"UDUnits", null, "secs since 1-10-8"}, {"UDUnits", null, "secs since +1101-10-8"},
        {"UDUnits", null, "secs since -1101-10-8"},};
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
    assertThat(cdu.getBaseDateTime()).isNotEqualTo(base);
    System.out.printf("CalendarDateUnit '%s' vs udunits '%s'%n", cdu.getBaseDateTime(), base);
  }

  private Date getUdunitBase(String s) throws Exception {
    Unit u = format.parse(s);
    assertThat(u).isInstanceOf(TimeScaleUnit.class);
    TimeScaleUnit tu = (TimeScaleUnit) u;
    return tu.getOrigin();
  }

}
