/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.regions.Region;
import ucar.ma2.Array;
import ucar.nc2.iosp.nexrad2.Nexrad2IOServiceProvider;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestS3ExternalCompressionRead {

  @Before()
  public void registerLevel2Iosp() throws InstantiationException, IllegalAccessException {
    NetcdfFiles.registerIOProvider(ucar.nc2.iosp.nexrad2.Nexrad2IOServiceProvider.class);
  }

  @Test
  public void testCompressedObjectRead() throws IOException {
    String region = Region.US_EAST_1.toString();
    String bucket = "noaa-nexrad-level2";
    String key = "1991/07/20/KTLX/KTLX19910720_160529.gz";
    String s3uri = "s3://" + bucket + "/" + key;

    System.setProperty("aws.region", region);
    try (NetcdfFile ncfile = NetcdfFiles.open(s3uri)) {

      Assert.assertNotNull(ncfile.findDimension("scanR"));
      Assert.assertNotNull(ncfile.findDimension("gateR"));
      Assert.assertNotNull(ncfile.findDimension("radialR"));

      Variable radiance = ncfile.findVariable("Rad");
      Variable reflectivity = ncfile.findVariable("Reflectivity");
      Assert.assertNotNull(reflectivity);

      // read array
      Array array = reflectivity.read();

      Assert.assertEquals(array.getRank(), 3);

      int[] shape = array.getShape();
      Assert.assertEquals(shape[0], 1);
      Assert.assertEquals(shape[1], 366);
      Assert.assertEquals(shape[2], 460);
    } finally {
      System.clearProperty("aws.region");
    }
  }
}
