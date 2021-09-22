/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.AxisType;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Some GOES-16/17 products are "CF-ish", in that they don't quite use projection variable attributes that match what CF
 * expects. Given the widespread use of these files, it's best we try to identify them and "do the right thing", and it
 * no longer appears that products are being produced without duplicating the attributes, so it's for historical data
 * only. This tests our CFConventions class to make sure this case is handled.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGoesCFish {

  private static final String testFile = TestDir.cdmUnitTestDir + "/conventions/cf/cf-ish/OR_EFD-060.nc";

  @Test
  public void testBadProjVarAttrs() throws IOException {
    try (NetcdfDataset ncd = NetcdfDatasets.openDataset(testFile)) {
      checkAxis(ncd);
    }
  }

  private void checkAxis(NetcdfDataset ncd) {
    CoordinateAxis latAxis = ncd.findCoordinateAxis(AxisType.GeoX);
    assertThat(latAxis).isNotNull();
    assertThat(latAxis.getAxisType()).isEqualTo(AxisType.GeoX);
  }

}
