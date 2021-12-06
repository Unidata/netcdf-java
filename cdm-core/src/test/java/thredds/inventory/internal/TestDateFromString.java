/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory.internal;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Ignore;
import org.junit.Test;
import ucar.nc2.calendar.CalendarDate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** Test {@link DateFromString} */
public class TestDateFromString {

  private final String fileName = "xzy_tds_20051129_1235_junk.grib";
  private final String dateAsISOString = "2005-11-29T12:35";
  private final long dateAsLong = 1133267700000L;

  @Test
  public void testGetDateUsingSimpleDateFormat() {
    String dateFormatString = "yyyyMMdd_HHmm";
    Date date = DateFromString.getDateUsingSimpleDateFormat(fileName, dateFormatString);
    assertThat(date.getTime()).isEqualTo(dateAsLong);
  }

  @Test
  @Ignore("failing on locale on github")
  public void testSimpleDateFormatProblem() throws ParseException {
    // expected: Wed Nov 29 06:00:00 MST 2006
    // but was : Wed Nov 29 06:00:00 UTC 2006
    assertThat(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_06").toString())
        .isEqualTo("Wed Nov 29 06:00:00 MST 2006");

    // TODO investigate this
    assertThat(new SimpleDateFormat("yyyyMMdd_HH").parse("20061129_0600").toString())
        .isEqualTo("Sun Dec 24 00:00:00 MST 2006");
  }

  @Test
  public void testGetDateUsingCompleteDateFormat() {
    String dateFormatString = "'xzy_tds_'yyyyMMdd_HHmm'_junk.grib'";

    Date date = DateFromString.getDateUsingCompleteDateFormat(fileName, dateFormatString);
    System.out.printf("date = %s%n", date);
    assertWithMessage("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <"
        + dateAsISOString + "[" + dateAsLong + "]>." + "\nUsing fileName <" + fileName + "> and dateFormatString <"
        + dateFormatString + ">").that(date.getTime()).isEqualTo(dateAsLong);
  }

  @Test
  public void testGetDateUsingRegExp() {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1-$2-$3T$4:$5";
    Date date = DateFromString.getDateUsingRegExp(fileName, matchPattern, substitutionPattern);
    assertWithMessage("Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <"
        + dateAsISOString + "[" + dateAsLong + "]>." + "\nUsing fileName <" + fileName + ">, matchPattern <"
        + matchPattern + ">, and substitutionPattern <" + substitutionPattern + ">").that(date.getTime())
            .isEqualTo(dateAsLong);
  }

  @Test
  public void testGetDateUsingRegExpAndDateFormat() {
    String matchPattern = ".*([0-9]{4})([0-9]{2})([0-9]{2})_([0-9]{2})([0-9]{2}).*grib";
    String substitutionPattern = "$1$2$3_$4$5";
    String dateFormatString = "yyyyMMdd_HHmm";

    Date date =
        DateFromString.getDateUsingRegExpAndDateFormat(fileName, matchPattern, substitutionPattern, dateFormatString);
    assertWithMessage(
        "Calculated date <" + date.toString() + " [" + date.getTime() + "]> not as expected <" + dateAsISOString + "["
            + dateAsLong + "]>." + "\nUsing fileName <" + fileName + ">, matchPattern <" + matchPattern
            + ">, substitutionPattern <" + substitutionPattern + ">, and dateFormatString <" + dateFormatString + ">")
                .that(date.getTime()).isEqualTo(dateAsLong);
  }

  @Test
  public void testGetDateUsingDemarkatedMatch() {
    Date result = DateFromString.getDateUsingDemarkatedMatch(
        "/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc", "#wrfout_d01_#yyyy-MM-dd_HHmm", '#');
    assertThat(result).isNotNull();
    CalendarDate cd = CalendarDate.of(result);
    assertThat(cd.toString()).isEqualTo("2006-07-06T08:00Z");

    result = DateFromString.getDateUsingDemarkatedMatch("C:\\data\\nomads\\gfs-hi\\gfs_3_20061129_0600",
        "#gfs_3_#yyyyMMdd_HH", '#');
    assertThat(result).isNotNull();
    cd = CalendarDate.of(result);
    assertThat(cd.toString()).isEqualTo("2006-11-29T06:00Z");

    result = DateFromString.getDateUsingDemarkatedMatch("/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc",
        "yyyyMMddHH#/wrfout_d01_#", '#');
    assertThat(result).isNotNull();
    cd = CalendarDate.of(result);
    assertThat(cd.toString()).isEqualTo("2006-07-06T11:00Z");
  }

  @Test
  public void testGetDateUsingDemarkatedMatchFails() {
    // doesnt have two demark's
    assertThat(DateFromString.getDateUsingDemarkatedMatch("/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc",
        "#wrfout_d01_yyyy-MM-dd_HHmm", '#')).isNull();

    // doesnt match
    assertThat(DateFromString.getDateUsingDemarkatedMatch("/data/anything/2006070611/wrfout_d01_2006-07-06_080000.nc",
        "#wrfout_d02_#yyyy-MM-dd_HHmm", '#')).isNull();
  }

}
