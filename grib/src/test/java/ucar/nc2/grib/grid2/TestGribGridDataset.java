/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.grid.GribGridDataset;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGribGridDataset {

  @Test
  public void testTwodOrthogonal() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    testOpen(endpoint, gridName);
  }

  @Test
  public void testMRUTC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    testOpen(endpoint, gridName);
  }

  @Test
  public void testMRUTP() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    testOpen(endpoint, gridName);
  }

  @Test
  public void testSRC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4";
    String gridName = "Temperature_isobaric";
    testOpen(endpoint, gridName);
  }

  private void testOpen(String endpoint, String gridName) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(endpoint, errlog).orElseThrow()) {
      assertThat(gds).isNotNull();
      System.out.printf("%s%n", gds);

      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      GridHorizCoordinateSystem hcs = cs.getHorizCoordSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.getShape()).isEqualTo(new int[] {73, 144});
    }
  }
}
