/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Formatter;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.regions.Region;
import ucar.unidata.io.InMemoryRandomAccessFile;
import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.http.HTTPRandomAccessFile;
import ucar.unidata.io.s3.S3RandomAccessFile;
import ucar.unidata.util.test.CompareNetcdf;
import ucar.unidata.util.test.category.NeedsExternalResource;

public class TestNetcdfFilesRafs {

  private final String s3Bucket = "noaa-goes16";
  private final String s3Key =
      "ABI-L1b-RadM/2017/241/23/OR_ABI-L1b-RadM1-M3C11_G16_s20172412359247_e20172412359304_c20172412359341.nc";
  private final String s3uri = "cdms3://" + s3Bucket + "?" + s3Key;

  private final String baseHttpLocation = "noaa-goes16.s3.amazonaws.com/" + s3Key;
  private final String inMemLocation = "slurp://" + baseHttpLocation;
  private final String httpsLocation = "https://" + baseHttpLocation;

  @BeforeClass
  public static void setup() {
    System.setProperty("aws.region", Region.US_EAST_1.toString());
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testHttpRaf() throws IOException {
    try (NetcdfFile ncf = NetcdfFiles.open(httpsLocation)) {
      Object raf = ncf.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(HTTPRandomAccessFile.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testRedirectedHttpRaf() throws IOException {
    String httpLocation = "http://" + baseHttpLocation;
    try (NetcdfFile ncf = NetcdfFiles.open(httpLocation)) {
      Object raf = ncf.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(HTTPRandomAccessFile.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testInMemoryRaf() throws IOException {
    try (NetcdfFile ncf = NetcdfFiles.open(inMemLocation)) {
      Object raf = ncf.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(InMemoryRandomAccessFile.class);
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void compareRafs() throws IOException {
    // download a local copy of the file
    File tempFile = File.createTempFile("ncj-", null);
    tempFile.deleteOnExit();
    FileUtils.copyURLToFile(new URL(httpsLocation), tempFile);

    // open using three different RAFs
    NetcdfFile local = NetcdfFiles.open(tempFile.getCanonicalPath());
    NetcdfFile inMem = NetcdfFiles.open(inMemLocation);
    NetcdfFile http = NetcdfFiles.open(httpsLocation);
    NetcdfFile s3 = NetcdfFiles.open(s3uri);

    // check that expected RAFs are used
    Object raf = local.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    assertThat(raf).isInstanceOf(RandomAccessFile.class);
    raf = inMem.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    assertThat(raf).isInstanceOf(InMemoryRandomAccessFile.class);
    raf = http.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    assertThat(raf).isInstanceOf(HTTPRandomAccessFile.class);
    raf = s3.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
    assertThat(raf).isInstanceOf(S3RandomAccessFile.class);

    // compare at a NetcdfFile level
    // CompareNetcdf(showCompare, showEach, compareData)
    CompareNetcdf comparer = new CompareNetcdf(false, false, true);

    Assert.assertTrue(comparer.compare(local, inMem, new Formatter()));
    Assert.assertTrue(comparer.compare(local, http, new Formatter()));
    Assert.assertTrue(comparer.compare(local, s3, new Formatter()));

    local.close();
    inMem.close();
    http.close();
    s3.close();
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testS3Raf() throws IOException {
    try (NetcdfFile ncf = NetcdfFiles.open(s3uri)) {
      Object raf = ncf.iosp.sendIospMessage(NetcdfFile.IOSP_MESSAGE_RANDOM_ACCESS_FILE);
      assertThat(raf).isInstanceOf(S3RandomAccessFile.class);
    }
  }

  @AfterClass
  public static void cleanup() {
    System.clearProperty("aws.region");
  }
}
