/*
 * Copyright (c) 1998-2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.s3;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.amazon.awssdk.regions.Region;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsExternalResource;

@Category(NeedsExternalResource.class)
public class TestS3Read {

  @Test
  public void testFullS3ReadNetcdfFile() throws IOException {
    String region = Region.US_EAST_1.toString();
    String bucket = "noaa-goes16";
    String key =
        "ABI-L1b-RadC/2019/363/21/OR_ABI-L1b-RadC-M6C16_G16_s20193632101189_e20193632103574_c20193632104070.nc";
    String s3uri = "s3://" + bucket + "/" + key;

    System.setProperty("aws.region", region);
    try (NetcdfFile ncfile = NetcdfFiles.open(s3uri)) {

      Dimension x = ncfile.findDimension("x");
      Dimension y = ncfile.findDimension("y");
      assertThat(x).isNotNull();
      assertThat(y).isNotNull();

      Variable radiance = ncfile.findVariable("Rad");
      Assert.assertNotNull(radiance);

      // read full array
      Array array = radiance.read();
      assertThat(array.getRank()).isEqualTo(2);

      // check shape of array is the same as the shape of the variable
      int[] variableShape = radiance.getShape();
      int[] arrayShape = array.getShape();
      assertThat(arrayShape).isEqualTo(arrayShape);
    } finally {
      System.clearProperty("aws.region");
    }
  }

  @Test
  public void testPartialS3NetcdfFile() throws IOException, InvalidRangeException {
    String region = Region.US_EAST_1.toString();
    String bucket = "noaa-goes16";
    String key =
        "ABI-L1b-RadC/2019/363/21/OR_ABI-L1b-RadC-M6C16_G16_s20193632101189_e20193632103574_c20193632104070.nc";
    String s3uri = "s3://" + bucket + "/" + key;

    System.setProperty("aws.region", region);
    try (NetcdfFile ncfile = NetcdfFiles.open(s3uri)) {

      Variable radiance = ncfile.findVariable("Rad");
      Assert.assertNotNull(radiance);

      // read part of the array
      Section section = new Section("(100:200:2,10:20:1)");
      Array array = radiance.read(section);
      assertThat(array.getRank()).isEqualTo(2);

      // check shape of array is the same as the shape of the section
      assertThat(array.getShape()).isEqualTo(section.getShape());
    } finally {
      System.clearProperty("aws.region");
    }
  }

  @Test
  public void testFullS3ReadNetcdfDatasets() throws IOException {
    String region = Region.US_EAST_1.toString();
    String bucket = "noaa-goes16";
    String key =
        "ABI-L1b-RadC/2019/363/21/OR_ABI-L1b-RadC-M6C16_G16_s20193632101189_e20193632103574_c20193632104070.nc";
    String s3uri = "s3://" + bucket + "/" + key;

    System.setProperty("aws.region", region);
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(s3uri)) {
      String partialConventionValue = "CF-1.";
      // read conventions string
      String conventions = ds.getRootGroup().attributes().findAttValueIgnoreCase(CF.CONVENTIONS, "");

      // check that the file was read the CF convention builder
      assertThat(conventions).startsWith(partialConventionValue);
      assertThat(ds.getConventionUsed()).startsWith(partialConventionValue);

    } finally {
      System.clearProperty("aws.region");
    }
  }
}
