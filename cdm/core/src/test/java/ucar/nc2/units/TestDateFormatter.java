package ucar.nc2.units;

import java.util.Date;
import org.junit.Test;

public class TestDateFormatter {

  private static void test(String text) {
    DateFormatter formatter = new DateFormatter();
    Date date = formatter.getISODate(text);
    String text2 = formatter.toDateTimeStringISO(date);
    Date date2 = formatter.getISODate(text2);
    assert date.equals(date2);
    System.out.println(text + " == " + text2);
  }

  @Test
  public void testStuff() {
    test("2001-09-11T12:09:20");
    test("2001-09-11 12:10:12");
    test("2001-09-11T12:10");
    test("2001-09-11 12:01");
    test("2001-09-11");
  }
}
