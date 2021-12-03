/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grid.CompareGridDataset;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Open Grib files directly through GribGrid, and indirectly through NetcdfDataset which ises GribIosp.
 * A number of these tests are withdrawn; its not surprising that the direct GRIB reading is not the same
 * as opening it through the GRIB IOSP. Generally, the direct GRIB should be correct.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribGridVsIosp {

  @Ignore("not working")
  @Test
  public void testTwod() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    testOpen(endpoint);
  }

  @Ignore("not working")
  @Test
  public void testTwodWhyNotMRUTP() throws IOException {
    // TODO why not MRUTP?
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4";
    testOpen(endpoint);
  }

  @Ignore("not working")
  @Test
  public void testTwodRegular() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4";
    testOpen(endpoint);
  }

  @Ignore("not working")
  @Test
  public void testTwodOrthogonal() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4";
    testOpen(endpoint);
  }

  @Ignore("not working")
  @Test
  public void testMRUTC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4";
    testOpen(endpoint);
  }

  @Ignore("not working")
  @Test
  public void testMRUTP() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4";
    testOpen(endpoint);
  }

  // layer_between_two_pressure_difference_from_ground_layer has intervals flipped
  @Test
  public void testSRC() throws IOException {
    String endpoint = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4";
    testOpen(endpoint);
  }

  @Test
  public void testEns() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/MOEASURGEENS20100709060002.grib";
    testOpen(filename);
  }

  @Test
  public void testEns2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4";
    testOpen(filename);
  }

  private void testOpen(String endpoint) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GribGridDataset gribDataset = GribGridDataset.open(endpoint, errlog).orElseThrow();
        GridDataset ncDataset = GridDatasetFactory.openNetcdfAsGrid(endpoint, errlog)) {
      assertThat(gribDataset).isNotNull();
      assertThat(ncDataset).isNotNull();

      new CompareGridDataset(ncDataset, gribDataset, true).compare();
    }
  }
}
