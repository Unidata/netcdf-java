package thredds.inventory;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import thredds.inventory.TimedCollection.Dataset;
import ucar.nc2.calendar.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestTimedCollection {

  private static void doit(String spec, int count, String name, String start, String end) throws IOException {
    Formatter errlog = new Formatter();
    MFileCollectionManager dcm = MFileCollectionManager.open("test", spec, null, errlog);
    TimedCollection collection = new TimedCollection(dcm, errlog);
    System.out.printf("spec= %s%n%s%n", spec, collection);
    String err = errlog.toString();
    if (err.length() > 0) {
      System.out.printf("%s%n", err);
    }
    System.out.printf("-----------------------------------%n");
    assertThat(collection.getDatasets()).hasSize(count);
    if (count == 0) {
      return;
    }
    Optional<Dataset> opt = collection.getDatasets().stream().filter(d -> d.location.endsWith(name)).findFirst();
    assertThat(opt.isPresent());
    Dataset ds = opt.get();
    assertThat(ds.dateRange.getStart()).isEqualTo(CalendarDate.fromUdunitIsoDate(null, start).orElseThrow());
    assertThat(ds.dateRange.getEnd()).isEqualTo(CalendarDate.fromUdunitIsoDate(null, end).orElseThrow());
  }

  @Test
  public void testParseDateRange() throws IOException {
    doit(TestDir.cdmUnitTestDir + "formats/gempak/surface/#yyyyMMdd#_sao.gem", 9, "surface/19580807_sao.gem",
        "1958-08-07T00:00Z", "2009-05-21T00:00Z");
    doit(TestDir.cdmUnitTestDir + "formats/gempak/surface/#yyyyMMdd#_sao\\.gem", 9, "20090528_sao.gem",
        "2009-05-28T00:00Z", "2009-05-29T00Z");
    doit(TestDir.cdmUnitTestDir + "ft/station/ldm-metar/Surface_METAR_#yyyyMMdd_HHmm#.nc", 9,
        "Surface_METAR_20060325_0000.nc", "2006-03-25T00Z", "2006-03-26T00Z");
  }
}
