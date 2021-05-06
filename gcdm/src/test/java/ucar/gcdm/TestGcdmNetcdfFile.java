/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.TestDir;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
public class TestGcdmNetcdfFile {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir, new SuffixFileFilter(".nc"), result, true);
      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "formats/bufr/userExamples", ff, result, false);

      // result.add(new Object[] {TestDir.cdmUnitTestDir + "formats/bufr/userExamples/WMO_v16_3-10-61.bufr"});

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String cdmrUrl;

  public TestGcdmNetcdfFile(String filename) {
    this.filename = filename.replace("\\", "/");

    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.cdmrUrl = "cdmr://localhost:16111/" + this.filename;
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestGcdmNetcdfFile %s%n", filename);
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile cdmrFile = GcdmNetcdfFile.builder().setRemoteURI(cdmrUrl).build()) {

      boolean ok = CompareArrayToArray.compareFiles(ncfile, cdmrFile);
      assertThat(ok).isTrue();
    }
  }

}
