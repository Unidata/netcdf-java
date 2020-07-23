/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.CheckPointFeatureDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Run specific problem. */
@Category(NeedsCdmUnitTest.class)
public class TestPointProblem {

  @Test
  public void checkPointDataset() throws IOException {
    String location = TestDir.cdmUnitTestDir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc";
    FeatureType ftype = FeatureType.ANY_POINT;
    int countExpected = 17280;
    CheckPointFeatureDataset checker = new CheckPointFeatureDataset(location, ftype, true);
    Assert.assertTrue(checker.check() > 0);
    // Assert.assertEquals("npoints", countExpected, checker.check());
  }

}
