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
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestBufrBuilderProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "/formats/bufr/userExamples/20120317_1800_JUBE99_EGRR.bufr";
    // showOrg(filename);
    // showNew(filename);
    compareWithBuilder(filename);
  }

  private void compareWithBuilder(String filename) throws IOException {
    logger.info("TestBuilders on {}%n", filename);
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, null);
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  @Test
  @Ignore("SequenceDS needs work")
  public void compareCoordSysBuilders() throws IOException {
    String fileLocation =
        TestDir.cdmUnitTestDir + "/formats/bufr/userExamples/US058MCUS-BUFtdp.SPOUT_00011_buoy_20091101021700.bufr";
    System.out.printf("Compare %s%n", fileLocation);
    logger.info("TestCoordSysCompare on {}%n", fileLocation);
    try (NetcdfDataset org = NetcdfDataset.openDataset(fileLocation)) {
      try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(fileLocation)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, null);
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        System.out.printf("org = %s%n", org.getRootGroup().findAttValueIgnoreCase(_Coordinate._CoordSysBuilder, ""));
        System.out.printf("new = %s%n",
            withBuilder.getRootGroup().findAttValueIgnoreCase(_Coordinate._CoordSysBuilder, ""));
        assertThat(ok).isTrue();
      }
    }
  }

  private void showOrg(String filename) throws IOException {

    try (NetcdfFile org = NetcdfFile.open(filename)) {
      // Variable v = org.findVariable("catchments_part_node_count");
      // Array data = v.read();
      System.out.printf("org = %s%n", org);
    }
  }

  private void showNew(String filename) throws IOException {

    try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
      // Variable v = withBuilder.findVariable("catchments_x");
      // Array data = v.read();
      System.out.printf("withBuilder = %s%n", withBuilder);
    }
  }
}

