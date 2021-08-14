/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.grid.GribGridDataset;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Open Grib files through Netcdf and Grib, and compare. */
@Category(NeedsCdmUnitTest.class)
public class TestGcdmTimeCS {

  @Ignore("not working")
  @Test
  public void testTwod() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String gridName = "Total_precipitation_surface_Mixed_intervals_Accumulation_probability_above_0p254";
    testOpen(endpoint, false);
  }

  @Ignore("not working")
  @Test
  public void testTwodWhyNotMRUTP() throws IOException {
    // LOOK why not MRUTP?
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4";
    String gridName = "Temperature_surface_6_Day_Average_probability_below_0";
    testOpen(endpoint, false);
  }

  @Ignore("not working")
  @Test
  public void testTwodRegular() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";
    String gridName = "Wind_speed_height_above_ground";
    testOpen(endpoint, false);
  }

  @Ignore("not working")
  @Test
  public void testTwodOrthogonal() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    String gridName = "Ozone_Mixing_Ratio_isobaric";
    testOpen(endpoint, false);
  }

  @Ignore("not working")
  @Test
  public void testMRUTC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4";
    String gridName = "VIL_altitude_above_msl";
    testOpen(endpoint, true);
  }

  @Ignore("not working")
  @Test
  public void testMRUTP() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4";
    String gridName = "MESHMax1440min_altitude_above_msl";
    testOpen(endpoint, false);
  }

  @Test
  public void testSRC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4";
    String gridName = "Temperature_isobaric";
    testOpen(endpoint, false);
  }

  @Test
  public void testEns() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    String gridName = "VAR10-3-192_FROM_74-0--1_surface_ens";
    testOpen(filename, false);
  }

  @Test
  public void testEns2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4";
    String gridName = "Total_precipitation_surface";
    testOpen(filename, false);
  }

  private void testOpen(String endpoint, boolean skipTimes) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GribGridDataset gribDataset = GribGridDataset.open(endpoint, errlog).orElseThrow();
        GridDataset ncDataset = GridDatasetFactory.openNetcdfAsGrid(endpoint, errlog)) {
      assertThat(gribDataset).isNotNull();
      assertThat(ncDataset).isNotNull();

      TestGcdmGridConverter.compareValuesEqual(ncDataset, gribDataset, skipTimes);
    }
  }
}
