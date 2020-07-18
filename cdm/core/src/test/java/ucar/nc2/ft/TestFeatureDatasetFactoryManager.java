/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE.txt for license information.
 */

package ucar.nc2.ft;

import static com.google.common.truth.Truth.assertThat;
import java.util.Formatter;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.TestDir;

/** Test that FeatureDatasetFactoryManager works when grib module is not loaded. */
public class TestFeatureDatasetFactoryManager {

  /** Tests a non-CF compliant trajectory file */
  @Test
  public void testSimpleTrajectory() throws IOException {
    Path location_path =
        Paths.get(TestDir.cdmLocalTestDataDir, "trajectory", "aircraft", "uw_kingair-2005-01-19-113957.nc");
    System.out.printf("testSimpleTrajectory on %s%n", location_path);
    FeatureDataset featureDataset =
        FeatureDatasetFactoryManager.open(FeatureType.ANY, location_path.toString(), null, new Formatter());
    assertThat(featureDataset).isNotNull();
    assertThat(featureDataset.getFeatureType()).isEqualTo(FeatureType.TRAJECTORY);
  }
}
