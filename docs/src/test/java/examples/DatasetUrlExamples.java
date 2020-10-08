/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package examples;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.dataset.NetcdfDatasets;

public class DatasetUrlExamples {

  /**
   * Code snippet to read a cdm file off the AWS S3 object store
   *
   * @throws IOException Encountered a problem reading from the object store
   */
  public static void awsGoes16Example() throws IOException {
    String region = "us-east-1";
    String bucket = "noaa-goes16";
    String key =
        "ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
    String cdmS3Uri = "cdms3:" + bucket + "?" + key;

    System.setProperty("aws.region", region);

    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      // do cool stuff here
      assertThat(ncfile).isNotNull(); /* DOCS-IGNORE */
    } finally {
      System.clearProperty("aws.region");
    }
  }

  /**
   * Code snippet to read a cdm file off the Google Cloud Storage object store
   *
   * @throws IOException Encountered a problem reading from the object store
   */
  public static void gcsGoes16Example() throws IOException {
    String host = "storage.googleapis.com";
    String bucket = "gcp-public-data-goes-16";
    String key =
        "ABI-L1b-RadC/2017/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
    String cdmS3Uri = "cdms3://" + host + "/" + bucket + "?" + key;

    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      // do cool stuff here
      assertThat(ncfile).isNotNull(); /* DOCS-IGNORE */
    }
  }

  /**
   * Code snippet to read a cdm file off the Open Science Data Cloud Ceph object store
   *
   * @throws IOException Encountered a problem reading from the object store
   */
  public static void osdcGoes16Example() throws IOException {
    String host = "griffin-objstore.opensciencedatacloud.org";
    String bucket = "noaa-goes16-hurricane-archive-2017";
    String key =
        "ABI-L1b-RadC/242/00/OR_ABI-L1b-RadC-M3C01_G16_s20172420002168_e20172420004540_c20172420004583.nc";
    String cdmS3Uri = "cdms3://" + host + "/" + bucket + "?" + key;

    try (NetcdfFile ncfile = NetcdfFiles.open(cdmS3Uri)) {
      // do cool stuff here
      assertThat(ncfile).isNotNull(); /* DOCS-IGNORE */
    }
  }

  /**
   * Code snippet to open a local .dods file
   *
   * @param pathToDodsFile: path to the file containing the dap2 binary response (must end in .dods)
   * @throws IOException Encountered a problem opening the .dods or .das temp files
   */
  public static void openDodsBinaryFile(String pathToDodsFile) throws IOException {
    // pathToDodsFile looks like C:/Users/me/Downloads/cool-dataset.nc.dods
    try (NetcdfFile ncf = NetcdfDatasets.openFile("file:" + pathToDodsFile, null)) {
      // Do cool stuff here
      assertThat(ncf).isNotNull(); /* DOCS-IGNORE */
    }
  }
}
