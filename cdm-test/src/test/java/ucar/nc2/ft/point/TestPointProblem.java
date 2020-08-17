/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ft.point;

import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.CheckPointFeatureDataset;
import ucar.unidata.util.test.TestDir;

/** Run specific problem. */
public class TestPointProblem {

  @Test
  public void checkPointDataset() throws IOException {
    String location = TestDir.cdmLocalFromTestDataDir + "pointPre1.6/kunicki.structs.nc4";
    FeatureType ftype = FeatureType.ANY_POINT;
    int countExpected = 17280;
    CheckPointFeatureDataset checker = new CheckPointFeatureDataset(location, ftype, true);
    Assert.assertTrue(checker.check() > 0);
    // Assert.assertEquals("npoints", countExpected, checker.check());
  }

}
