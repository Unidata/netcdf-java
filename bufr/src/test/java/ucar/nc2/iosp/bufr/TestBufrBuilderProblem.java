package ucar.nc2.iosp.bufr;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestBufrBuilderProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testEmbeddedTable() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/mixed/gdas1.t18z.osbuv8.tm00.bufr_d";
    testRead(filename);
  }

  private void testRead(String filename) throws IOException {
    System.out.printf("Test read all variables for  on %s%n", filename);
    TestDir.readAll(filename);
  }

  @Test
  public void compareCoordSysBuilders() throws IOException {
    String fileLocation =
        TestDir.cdmUnitTestDir + "/formats/bufr/userExamples/US058MCUS-BUFtdp.SPOUT_00011_buoy_20091101021700.bufr";
    System.out.printf("Compare %s%n", fileLocation);
    try (NetcdfDataset org = NetcdfDatasets.openDataset(fileLocation)) {
      try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(fileLocation)) {
        boolean ok = CompareArrayToArray.compareFiles(org, withBuilder);
        System.out.printf("%s%n", ok ? "OK" : "NOT OK");
        System.out.printf("org = %s%n", org.getRootGroup().findAttributeString(_Coordinate._CoordSysBuilder, ""));
        System.out.printf("new = %s%n",
            withBuilder.getRootGroup().findAttributeString(_Coordinate._CoordSysBuilder, ""));
        assertThat(ok).isTrue();
      }
    }
  }
}

