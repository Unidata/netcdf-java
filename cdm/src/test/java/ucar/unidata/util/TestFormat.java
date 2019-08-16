package ucar.unidata.util;

import static ucar.unidata.util.Format.dfrac;

import java.util.Formatter;
import org.junit.Test;

public class TestFormat {

  //////////////////////////////////////////////////////////////////////////////////////

  /**
   * Show the value of a double to the significant figures
   *
   * @param d      double value
   * @param sigfig number of significant figures
   */
  private static void show(double d, int sigfig) {
    System.out.println("Format.d(" + d + "," + sigfig + ") == "
        + Format.d(d, sigfig));
  }

  /**
   * Show the value of a double with specified number of decimal places
   *
   * @param d          double value
   * @param dec_places number of decimal places
   */
  private static void show2(double d, int dec_places) {
    System.out.println("Format.dfrac(" + d + "," + dec_places + ") == "
        + dfrac(d, dec_places));
  }

  public static void doit(int scale, double n) {
    Formatter f = new Formatter();
    String format = "Prob_above_%f3." + scale;
    f.format(format, n);
    System.out.printf("%s %f == %s%n", format, n, f);
  }

  @Test
  public void testStuff() {
    doit(1, 1.00003);
    System.out.printf("%f == %f3.1%n", 1.00003, 1.00003);
    System.out.printf("%s%n", dfrac(1.00003, 0));
    System.out.printf("%s%n", dfrac(1.00003, 1));
  }



}
