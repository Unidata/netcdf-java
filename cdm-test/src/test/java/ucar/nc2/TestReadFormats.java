package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/** Just open all the files in the selected directory. */
@Category(NeedsCdmUnitTest.class)
public class TestReadFormats {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private static int countGood = 0;
  private static int countFail = 0;

  private List<String> failFiles = new ArrayList<>();

  @Test
  public void testReadFormats() throws IOException {
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/netcdf3", null, this::doRead);
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/netcdf4", null, this::doRead);
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/hdf5", TestDir.FileFilterSkipSuffix("xml"), this::doRead);
    TestDir.actOnAll(TestDir.cdmUnitTestDir + "/formats/hdf4", null, this::doRead);
    System.out.printf("Good=%d Fail=%d%n", countGood, countFail);
    if (countFail > 0) {
      System.out.printf("Failed Files%n");
      for (String f : failFiles) {
        System.out.printf("  %s%n", f);
      }
    }
    assertThat(countFail).isEqualTo(0);
  }

  // these are fairly complete hdf4 files from nsidc
  public void utestHdf4() throws IOException {
    TestDir.actOnAll("F:/data/formats/hdf4", null, this::doRead);
    System.out.printf("Good=%d Fail=%d%n", countGood, countFail);
  }

  private int doRead(String name) {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(name, null)) {
      System.out.printf("  GOOD on %s == %s%n", name, ncfile.getFileTypeId());
      countGood++;
      return 1;
    } catch (Throwable t) {
      System.out.printf("  FAIL on %s == %s%n", name, t.getMessage());
      failFiles.add(name);
      t.printStackTrace();
      countFail++;
      return 0;
    }
  }

}
