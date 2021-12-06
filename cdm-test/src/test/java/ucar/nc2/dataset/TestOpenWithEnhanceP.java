/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestOpenWithEnhanceP {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "conventions", new SuffixFileFilter(".nc"), result);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  String filename;

  public TestOpenWithEnhanceP(String filename) {
    this.filename = filename;
  }

  @Test
  public void openWithEnhance() throws Exception {
    try (NetcdfDataset ncDataset = NetcdfDatasets.openDataset(filename, true, null)) {
      assertThat(NetcdfDataset.getDefaultEnhanceMode()).isEqualTo(ncDataset.getEnhanceMode());
      assertThat(ncDataset.getCoordinateSystems().size()).isGreaterThan(0);
    }
  }
}
