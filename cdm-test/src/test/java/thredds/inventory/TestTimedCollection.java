package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

public class TestTimedCollection {

  //////////////////////////////////////////////////////////////////////////
  // debugging
  private static void doit(String spec, int count) throws IOException {
    Formatter errlog = new Formatter();
    MFileCollectionManager dcm = MFileCollectionManager.open("test", spec, null, errlog);
    TimedCollection collection = new TimedCollection(dcm, errlog);
    System.out.printf("spec= %s%n%s%n", spec, collection);
    String err = errlog.toString();
    if (err.length() > 0)
      System.out.printf("%s%n", err);
    System.out.printf("-----------------------------------%n");
    assertThat(collection.getDatasets()).hasSize(count);
  }

  @Test
  public void testStuff() throws IOException {
    doit(TestDir.cdmUnitTestDir + "formats/gempak/surface/#yyyyMMdd#_sao.gem", 9);
    doit(TestDir.cdmUnitTestDir + "formats/gempak/surface/#yyyyMMdd#_sao\\.gem", 9);
    doit(TestDir.cdmUnitTestDir + "ft/station/ldm-metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", 9);
  }



}
