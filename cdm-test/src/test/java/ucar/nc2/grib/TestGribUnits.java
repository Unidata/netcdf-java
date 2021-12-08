package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by rmay on 3/2/16.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribUnits {

  @Test
  public void test_ordered_sequence_units() throws IOException {
    // Make sure we return the udunits string of "count"
    String filename = "tds/ncep/WW3_Coastal_Alaska_20140804_0000.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + filename)) {
      Variable var = ds.getRootGroup().findVariableLocal("ordered_sequence_of_data");
      assertThat(var).isNotNull();
      Attribute att = var.findAttribute("units");
      assertThat(att).isNotNull();
      assertThat("count").isEqualTo(att.getStringValue());
    }
  }

  @Test
  public void test_true_degrees() throws IOException {
    // Make sure we return grib units of "degree true" as "degree_true"
    String filename = "tds/ncep/NDFD_CONUS_5km_20140805_1200.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + filename)) {
      Group grp = ds.getRootGroup();
      assertThat(grp).isNotNull();

      Variable var = grp.findVariableLocal("Wind_direction_from_which_blowing_height_above_ground");
      assertThat(var).isNotNull();

      Attribute att = var.findAttribute("units");
      assertThat(att).isNotNull();
      assertThat("degree_true").isEqualTo(att.getStringValue());
    }
  }

  @Test
  public void test_code_table() throws IOException {
    // Make sure we don't add '.' to "Code table a.b.c"
    String filename = "tds/ncep/NDFD_CONUS_5km_20140805_1200.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + filename)) {
      Group grp = ds.getRootGroup();
      assertThat(grp).isNotNull();

      Variable var = grp.findVariableLocal("Categorical_Rain_surface");
      assertThat(var).isNotNull();

      Attribute att = var.findAttribute("units");
      assertThat(att).isNotNull();
      assertThat("Code table 4.222").isEqualTo(att.getStringValue());
    }
  }
}
