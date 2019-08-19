package ucar.nc2.time;

import static org.junit.Assert.assertEquals;
import static ucar.nc2.time.CalendarDateFormatter.toDateString;
import static ucar.nc2.time.CalendarDateFormatter.toDateTimeString;
import static ucar.nc2.time.CalendarDateFormatter.toDateTimeStringISO;
import static ucar.nc2.time.CalendarDateFormatter.toTimeUnits;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.util.Locale;
import java.util.TimeZone;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.units.DateFormatter;

/**
 * Test CalendarDateFormatter
 *
 * @author caron
 * @since 5/3/12
 */
public class TestCalendarDateFormatter {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testBad() {
    claimBad("1143848700");
  }

  @Test
  public void testW3cIso() {
    claimGood("1997");
    claimGood("1997-07");
    claimGood("1997-07-16");
    claimGood("1997-07-16T19:20+01:00");
    claimGood("1997-07-16T19:20:30+01:00");

  }

  @Test
  public void testBasicFormatIso() {
    claimGood("19500101T000000Z"); // from https://github.com/Unidata/thredds/issues/772
    claimGood("199707");
    claimGood("19970716");
    claimGood("19970716T1920");
    claimGood("19970716T192030");
    claimGood("19970716T192030.1");
    claimGood("19970716T1920+01:00");
    claimGood("19970716T192030+0100");
    claimGood("19970716T192030+01");
    claimGood("19970716T192030.1+0100");
    claimGood("19970716T192030Z");
    claimGood("19970716T192030.1Z");
    // these should fail
    claimBad("19970716T192030.1UTC");
    claimBad("19501"); // fail because ambiguous
    claimBad("1950112"); // fail because ambiguous
    claimBad("19501120T121"); // fail because ambiguous
    claimBad("19501120T12151"); // fail because ambiguous
  }

  @Test
  public void testChangeoverDate() {
    claimGood("1997-01-01");
    claimGood("1582-10-16");
    claimGood("1582-10-15");
    claimGood("1582-10-01");
    claimGood("1582-10-02");
    claimGood("1582-10-03");
    claimGood("1582-10-04");
    //testBase("1582-10-14"); // fail
    //testBase("1582-10-06"); // fail
  }


  // UNIT since [-]Y[Y[Y[Y]]]-MM-DD[(T| )hh[:mm[:ss[.sss*]]][ [+|-]hh[[:]mm]]]
  @Test
  public void testUdunits() {
    claimGood("1992-10-8 15:15:42.5 -6:00");
    claimGood("1992-10-8 15:15:42.5 +6");
    claimGood("1992-10-8 15:15:42.534");
    claimGood("1992-10-8 15:15:42");
    claimGood("1992-10-8 15:15");
    claimGood("1992-10-8 15");
    claimGood("1992-10-8T15");
    claimGood("1992-10-8");
    claimGood("199-10-8");
    claimGood("19-10-8");
    claimGood("1-10-8");
    claimGood("+1101-10-8");
    claimGood("-1101-10-8");
    claimGood("1992-10-8T7:00 -6:00");
    claimGood("1992-10-8T7:00 +6:00");
    claimGood("1992-10-8T7 -6:00");
    claimGood("1992-10-8T7 +6:00");
    claimGood("1992-10-8 7 -6:00");
    claimGood("1992-10-8 7 +6:00");
  }

  @Test
  public void shouldBeSameTime() {

    String isoCET = "2012-04-27T16:00:00+0200";
    Date cetDate = CalendarDateFormatter.isoStringToDate(isoCET);
    String isoMST = "2012-04-27T08:00:00-0600";
    Date mstDate = CalendarDateFormatter.isoStringToDate(isoMST);
    String isoUTC = "2012-04-27T14:00Z";
    Date utcDate = CalendarDateFormatter.isoStringToDate(isoUTC);
    assertEquals(mstDate.getTime(), cetDate.getTime()); //This passes -> times with offset are ok
    assertEquals(mstDate.getTime(), utcDate.getTime()); //This fails!!
  }

  @Test
  public void shouldHandleOffsetWithoutColon() {

    String isoCET = "2012-04-27T16:00:00+0200";
    Date cetDate = CalendarDateFormatter.isoStringToDate(isoCET);//We get 2012-04-19T02:00:00-0600 and is
    String isoMST = "2012-04-27T08:00:00-0600";
    Date mstDate = CalendarDateFormatter.isoStringToDate(isoMST); //Fails here, unable to create a date with 600 hours of offset!!!
    String isoUTC = "2012-04-27T14:00Z";
    Date utcDate = CalendarDateFormatter.isoStringToDate(isoUTC);

    assertEquals(mstDate.getTime(), cetDate.getTime()); //This fails because offset
    assertEquals(mstDate.getTime(), utcDate.getTime()); //This fails!!
  }

  private void claimGood(String s) {
    try {
      CalendarDate result = CalendarDateFormatter.isoStringToCalendarDate(null, s);
      logger.debug("%s == %s%n", s, result);
    } catch (Exception e) {
      logger.error("FAIL %s%n", s);
      e.printStackTrace();
      CalendarDateFormatter.isoStringToCalendarDate(null, s);
      assert false;
    }
  }

  private void claimBad(String s) {
    try {
      CalendarDate result = CalendarDateFormatter.isoStringToCalendarDate(null, s);
      logger.error("FAIL %s%n", s);
      assert false;
    } catch (Exception e) {
      logger.debug("Expected fail = %s%n", s);
      return;
    }
  }

  @Test
  public void testStuff() {
    CalendarDate cd = CalendarDate.present();
     /* {"S", "M", "L", "F", "-"}
     System.out.printf("%s%n", DateTimeFormat.forStyle("SS").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("MM").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("LL").print(cd.getDateTime()));
     System.out.printf("%s%n", DateTimeFormat.forStyle("FF").print(cd.getDateTime())); */

    System.out.printf("%s%n", cd);
    System.out.printf("toDateTimeStringISO=%s%n", toDateTimeStringISO(cd));
    System.out.printf("   toDateTimeString=%s%n", toDateTimeString(cd));
    System.out.printf("       toDateString=%s%n", toDateString(cd));
    System.out.printf("        toTimeUnits=%s%n", toTimeUnits(cd));
    System.out.printf("===============================%n");
    Date d = cd.toDate();
    System.out.printf("cd.toDate()=%s%n", toDateTimeString(d));

    SimpleDateFormat udunitDF = (SimpleDateFormat) DateFormat
        .getDateInstance(DateFormat.SHORT, Locale.US);
    udunitDF.setTimeZone(TimeZone.getTimeZone("UTC"));
    udunitDF.applyPattern("yyyy-MM-dd HH:mm:ss.SSS 'UTC'");
    System.out.printf("           udunitDF=%s%n", udunitDF.format(d));

    System.out.printf("===============================%n");
    DateFormatter df = new DateFormatter();
    System.out.printf("     toTimeUnits(date)=%s%n", toTimeUnits(cd));
    System.out.printf("toDateTimeString(date)=%s%n", df.toDateTimeString(d));
    System.out.printf("toDateOnlyString(date)=%s%n", df.toDateOnlyString(d));

  }



}
