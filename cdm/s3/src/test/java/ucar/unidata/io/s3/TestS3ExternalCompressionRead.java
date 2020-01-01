/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.regions.Region;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestS3ExternalCompressionRead {

  @Before()
  public void registerLevel2Iosp() throws InstantiationException, IllegalAccessException {
    // NetcdfFiles.registerIOProvider(ucar.nc2.iosp.nexrad2.Nexrad2IOServiceProvider.class);
  }

  @Test
  public void testCompressedObjectRead() throws IOException {
    String region = Region.US_EAST_1.toString();
    String bucket = "noaa-nexrad-level2";
    String key = "1991/07/20/KTLX/KTLX19910720_160529.gz";
    String s3uri = "s3://" + bucket + "/" + key;

    System.setProperty("aws.region", region);
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
}
