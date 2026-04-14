/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;

/**
 * Tests reading single grib files from S3 when index files already exists in the bucket
 */
public class TestSingleGribFileS3RemoteIndex {

  private static final String GRIB1_BUCKET_KEY = "thredds-test-data?test-grib-index/radar_national.grib1";
  private static final String GRIB2_BUCKET_KEY = "thredds-test-data?test-grib-index/cosmo-eu.grib2";
  private static final String GRIB1_LOCAL = TestDir.localTestDataDir + "radar_national.grib1";
  private static final String GRIB2_LOCAL = TestDir.localTestDataDir + "cosmo-eu.grib2";

  @Test
  public void testGrib1S3Full() throws IOException {
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s#delimiter=/", GRIB1_BUCKET_KEY);
    // .ncx4 object is older than .gbx9 object, so an update is required (which isn't supported)
    assertThrows(IOException.class, () -> basicDatasetValidation(location));
  }

  @Test
  public void testGrib1S3Short() throws IOException {
    String location = String.format("cdms3:%s#delimiter=/", GRIB1_BUCKET_KEY);
    // .ncx4 object is older than .gbx9 object, so an update is required (which isn't supported)
    assertThrows(IOException.class, () -> basicDatasetValidation(location));
  }

  @Test
  public void testGrib1SFullNoDelimiter() throws IOException {
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s", GRIB1_BUCKET_KEY);
    // .ncx4 object is older than .gbx9 object, so an update is required (which isn't supported)
    assertThrows(IOException.class, () -> basicDatasetValidation(location));
  }

  @Test
  public void testGrib1S3ShortNoDelimiter() throws IOException {
    String location = String.format("cdms3:%s", GRIB1_BUCKET_KEY);
    // .ncx4 object is older than .gbx9 object, so an update is required (which isn't supported)
    assertThrows(IOException.class, () -> basicDatasetValidation(location));
  }

  @Test
  public void testGrib2S3Full() throws IOException {
    // .ncx4 is missing, so will be written locally and used successfully
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s#delimiter=/", GRIB2_BUCKET_KEY);
    basicDatasetValidation(location);
  }

  @Test
  public void testGrib2S3Short() throws IOException {
    // .ncx4 is missing, so will be written locally and used successfully
    String location = String.format("cdms3:%s#delimiter=/", GRIB2_BUCKET_KEY);
    basicDatasetValidation(location);
  }

  @Test
  public void testGrib2SFullNoDelimiter() throws IOException {
    // .ncx4 is missing, so will be written locally and used successfully
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s", GRIB2_BUCKET_KEY);
    basicDatasetValidation(location);
  }

  @Test
  public void testGrib2S3ShortNoDelimiter() throws IOException {
    // .ncx4 is missing, so will be written locally and used successfully
    String location = String.format("cdms3:%s", GRIB2_BUCKET_KEY);
    basicDatasetValidation(location);
  }

  @Test
  public void compareGrib1() throws IOException {
    // .ncx4 object is older than .gbx9 object, so an update is required (which isn't supported)
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s#delimiter=/", GRIB1_BUCKET_KEY);
    assertThrows(IOException.class, () -> compareWithLocal(location, GRIB1_LOCAL));
  }

  @Test
  public void compareGrib2() throws IOException {
    // .ncx4 is missing, so will be written locally and used successfully
    String location = String.format("cdms3://s3.us-east-1.amazonaws.com/%s#delimiter=/", GRIB2_BUCKET_KEY);
    compareWithLocal(location, GRIB2_LOCAL);
  }

  private static void compareWithLocal(String remoteLocation, String localLocation) throws IOException {
    try (NetcdfDataset remoteNetcdfDataset = NetcdfDatasets.openDataset(remoteLocation);
        NetcdfDataset localNetcdfDataset = NetcdfDatasets.openDataset(localLocation)) {
      Formatter f = new Formatter();
      CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
      if (!compare.compare(localNetcdfDataset, remoteNetcdfDataset, null)) {
        System.out.printf("Compare %s%n%s%n", localLocation, f);
        fail();
      }
    }
  }

  private static void basicDatasetValidation(String location) throws IOException {
    try (NetcdfDataset netcdfDataset = NetcdfDatasets.openDataset(location)) {
      assertThat(netcdfDataset.getLastModified()).isGreaterThan(0);
      assertThat(netcdfDataset.getVariables()).isNotEmpty();

      Variable variable = netcdfDataset.getVariables().get(1);
      Array data = variable.read();
      assertThat(data).isNotNull();
    }
  }
}
