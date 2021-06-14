package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestGribBasics {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testGroupAttributes() throws IOException {
    String filename = "gribCollections/dgex/20141011/dgex_46-20141011.ncx4";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(TestDir.cdmUnitTestDir + filename)) {
      Attribute att = ds.findAttribute("TwoD/@GribCollectionType");
      assertThat(att).isNotNull();
      assertThat(att.getStringValue()).isEqualTo("TwoD");

      att = ds.findAttribute("Best/@GribCollectionType");
      assertThat(att).isNotNull();
      assertThat(att.getStringValue()).isEqualTo("Best");
    }
  }
}
