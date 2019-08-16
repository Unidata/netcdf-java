package thredds.inventory;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;

public class TestTimedCollection {

  //////////////////////////////////////////////////////////////////////////
  // debugging
  private static void doit(String spec, Formatter errlog) throws IOException {
    MFileCollectionManager dcm = MFileCollectionManager.open("test", spec, null, errlog);
    TimedCollection specp = new TimedCollection(dcm, errlog);
    System.out.printf("spec= %s%n%s%n", spec, specp);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
  }

  @Test
  public void testStuff() throws IOException {
    doit("C:/data/formats/gempak/surface/#yyyyMMdd#_sao.gem", new Formatter());
    //doit("C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", new Formatter());
    // doit("Q:/station/ldm/metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", new Formatter());
  }



}
