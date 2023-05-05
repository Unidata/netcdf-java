/*
 * Copyright (c) 2019-2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestS3ExternalCompressionRead {
  private static final String compressedObject = "cdms3:noaa-nexrad-level2?1991/07/20/KTLX/KTLX19910720_160529.gz";
  private static final String fragment = "#delimiter=/";

  @Test
  public void testCompressedObjectRead() throws IOException {
    testCompressedObjectRead(compressedObject);
  }

  @Test
  public void testCompressedObjectReadWithDelimiterFragment() throws IOException {
    testCompressedObjectRead(compressedObject + fragment);
  }

  private static void testCompressedObjectRead(String s3uri) throws IOException {
    System.setProperty(S3TestsCommon.AWS_REGION_PROP_NAME, S3TestsCommon.AWS_G16_REGION);
    try (NetcdfFile ncfile = NetcdfFiles.open(s3uri)) {

      assertThat(ncfile.findDimension("scanR")).isNotNull();
      assertThat(ncfile.findDimension("gateR")).isNotNull();
      assertThat(ncfile.findDimension("radialR")).isNotNull();

      Variable reflectivity = ncfile.findVariable("Reflectivity");
      Assert.assertNotNull(reflectivity);

      // read array
      Array array = reflectivity.read();

      assertThat(array.getRank()).isEqualTo(3);

      assertThat(array.getShape()).isEqualTo(new int[] {1, 366, 460});
    } finally {
      System.clearProperty("aws.region");
    }
  }

  @Test
  @Ignore("Failing. Reason: This operation is not permitted on an archived blob. ?")
  public void testMicrosoftBlobS3() throws IOException {
    // https://nexradsa.blob.core.windows.net/nexrad-l2/1997/07/07/KHPX/KHPX19970707_000827.gz
    String host = "nexradsa.blob.core.windows.net";
    String bucket = "nexrad-l2";
    String key = "1991/07/20/KTLX/KTLX19910720_160529.gz";
    String s3Uri = "cdms3://" + host + "/" + bucket + "?" + key;
    try (NetcdfFile ncfile = NetcdfFiles.open(s3Uri)) {

      assertThat(ncfile.findDimension("scanR")).isNotNull();
      assertThat(ncfile.findDimension("gateR")).isNotNull();
      assertThat(ncfile.findDimension("radialR")).isNotNull();

      Variable reflectivity = ncfile.findVariable("Reflectivity");
      Assert.assertNotNull(reflectivity);

      // read array
      Array array = reflectivity.read();

      assertThat(array.getRank()).isEqualTo(3);

      assertThat(array.getShape()).isEqualTo(new int[] {1, 366, 460});
    }
  }
}
