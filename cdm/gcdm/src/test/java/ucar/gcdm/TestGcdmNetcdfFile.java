/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static ucar.gcdm.TestUtils.*;

import java.io.FileFilter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmNetcdfFile {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      // TODO skip test files that involve a Structure with a vlen member as that does not currently work
      FileFilter skipStructuresWithVlens =
          pathname -> pathname.getName().endsWith("nc") && !pathname.getName().startsWith("test_vlen3")
              && !pathname.getName().startsWith("test_vlen4") && !pathname.getName().startsWith("test_vlen5")
              && !pathname.getName().startsWith("test_vlen9") && !pathname.getName().startsWith("test_vlen10");
      String skipStructuresWithVlens2 = "vlen/IntTimSciSamp.nc vlen/cdm_sea_soundings.nc4";

      TestDir.actOnAllParameterized("../../dap4/src/test/data/resources/nctestfiles/", skipStructuresWithVlens, result,
          false);

      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir, new SuffixFileFilter(".nc"), result, true);

      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc " + skipStructuresWithVlens2);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf3", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/netcdf4", ff, result, false);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/files", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/netcdf4/vlen", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/samples", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/support", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf5/wrf", ff, result, true);
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/formats/hdf4", ff, result, true);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;

  public TestGcdmNetcdfFile(String filename) {
    this.filename = filename.replace("\\", "/");
  }

  @Test
  public void doOne() throws Exception {
    compareFiles(filename);
  }
}
