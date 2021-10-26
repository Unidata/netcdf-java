package ucar.nc2.iosp;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Just open all the files in the selected directory. */
@Category(NeedsCdmUnitTest.class)
public class TestJustReadFormats {
  private static int countGood = 0;
  private static int countFail = 0;
  private List<String> failFiles = new ArrayList<>();

  @Test
  public void testReadFormats() throws IOException {
    TestDir.actOnAll(TestDir.cdmLocalTestDataDir, TestDir.FileFilterSkipSuffix("dump txt"), this::doRead, false);
    TestDir.actOnAll(TestDir.cdmLocalTestDataDir + "/hdf4", TestDir.FileFilterSkipSuffix("invalidhdf4"), this::doRead);
    TestDir.actOnAll(TestDir.cdmLocalTestDataDir + "/hdf5", null, this::doRead);
    TestDir.actOnAll(TestDir.cdmLocalTestDataDir + "/wrf", null, this::doRead);
    System.out.printf("Good=%d Fail=%d%n", countGood, countFail);
    if (countFail > 0) {
      System.out.printf("Failed Files%n");
      for (String f : failFiles) {
        System.out.printf("  %s%n", f);
      }
    }
    assertThat(countFail).isEqualTo(0);
  }

  @Test
  public void testProblem() {
    doRead(TestDir.cdmLocalTestDataDir
        + "/hdf5/GATRO-SATMR_npp_d20020906_t0409572_e0410270_b19646_c20090720223122943227_devl_int.h5");
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
