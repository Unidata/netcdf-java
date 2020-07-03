/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.CheckPointFeatureDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestPointDatasets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    // gempack sounding
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml",
        FeatureType.STATION_PROFILE, 8769});

    // gempak surface
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/gempak/2009103008_sb.gem", FeatureType.POINT, 3337});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/gempak/2009110100_ship.gem", FeatureType.POINT, 938});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.POINT, 55856});
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.STATION, 28328});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml",
        FeatureType.STATION_PROFILE, 8769});
    return result;
  }

  private final String location;
  private final FeatureType ftype;
  private final int countExpected;
  private final boolean show = false;

  public TestPointDatasets(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  @Test
  public void checkPointFeatureDataset() throws IOException {
    CheckPointFeatureDataset checker = new CheckPointFeatureDataset(location, ftype, show);
    Assert.assertEquals("npoints", countExpected, checker.check());
  }
}

