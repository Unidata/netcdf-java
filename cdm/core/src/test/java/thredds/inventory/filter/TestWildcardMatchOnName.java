package thredds.inventory.filter;

import com.google.re2j.Matcher;
import org.junit.Test;

public class TestWildcardMatchOnName {

  @Test
  public void testStuff() {
    WildcardMatchOnName m = new WildcardMatchOnName("ECMWF_GNERA_d000..20121001");
    Matcher matcher = m.pattern.matcher("ECMWF_GNERA_d0002.20121001");
    System.out.printf("%s%n", matcher.matches());
  }

}
