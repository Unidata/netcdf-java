/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.nc2.internal.util.EscapeStrings;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * misc testing
 */
public class TestMisc {

  @Test
  public void testBackslashTokens() {
    testBackslashTokens("group/name.member.mom");
    testBackslashTokens("var\\.name.member.mom");
    testBackslashTokens("var\\.name.member.mom\\");
    testBackslashTokens("var\\.name.member.mom.");
    testBackslashTokens(".var\\.name.member.mom.");
    testBackslashTokens("...mom.");
  }

  private void testBackslashTokens(String escapedName) {
    System.out.printf("%s%n", escapedName);
    List<String> result = EscapeStrings.tokenizeEscapedName(escapedName);
    for (String r : result)
      System.out.printf("   %s%n", r);
    System.out.printf("%n");
  }

  @Test
  public void testCompareLongs() throws ParseException {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    Date[] dateList = new Date[] {sdf.parse("2002-01-01"), sdf.parse("2002-01-02"), sdf.parse("2002-01-03"),
        sdf.parse("2002-01-04"), sdf.parse("2002-02-05"), sdf.parse("2002-03-06")};

    Arrays.sort(dateList, new DateComparator1());
    System.out.println("sort error: " + Arrays.toString(dateList));

    Arrays.sort(dateList, new DateComparator2());
    System.out.println("sort fix:   " + Arrays.toString(dateList));

    Long.toString(0);
  }


  // reverse sort - latest come first
  private class DateComparator1 implements Comparator<Date> {
    public int compare(Date f1, Date f2) {
      System.out.print(
          f2 + "-" + f1 + " =" + f2.getTime() + "-" + f1.getTime() + " =  int: " + (int) (f2.getTime() - f1.getTime()));
      System.out.println("  long: " + (f2.getTime() - f1.getTime()));

      return (int) (f2.getTime() - f1.getTime());
    }
  }

  // reverse sort - latest come first
  private class DateComparator2 implements Comparator<Date> {
    public int compare(Date f1, Date f2) {
      // return (thisVal<anotherVal ? -1 : (thisVal==anotherVal ? 0 : 1));

      if (f2.getTime() == f1.getTime())
        return 0;
      return (f2.getTime() > f1.getTime()) ? 1 : -1;
    }
  }

}
