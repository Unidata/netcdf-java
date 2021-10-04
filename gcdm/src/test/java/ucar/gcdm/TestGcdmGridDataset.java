/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.grid.*;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmGridDataset {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "permuteTest.nc"});

      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .gbx9 aggFmrc.xml cg.ncml");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/ft/grid", ff, result, true);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmGridDataset(String filename) throws IOException {
    this.filename = filename.replace("\\", "/");
    File file = new File(filename);
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestGcdmNetcdfFile  %s%n", filename);

    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(filename, info)) {
      if (local == null) {
        System.out.printf("TestGcdmNetcdfFile %s NOT a grid %s%n", filename, info);
        return;
      }
      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        if (remote == null) {
          System.out.printf(" Remote %s fails %s%n", filename, info);
          return;
        }
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        new CompareGridDataset(remote, local, false).compare();
      }
    }
  }

}
