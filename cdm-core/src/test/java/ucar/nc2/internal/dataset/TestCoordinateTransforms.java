package ucar.nc2.internal.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.internal.grid.GridNetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridNetcdfDataset} */
public class TestCoordinateTransforms {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOne() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/dgex_46-20141011.ncx4";
    String gname = "TwoD/Temperature_isobaric";
    System.out.printf("filename %s%n", filename);

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      // TODO that we have the projection twice, one in Best and one in TwoD. Is that ok?
      // The transform name should be TwoD/.. but it got lost.

      VariableDS v = (VariableDS) ds.findVariable(gname);
      CoordinateSystem csys = v.getCoordinateSystems().get(0);

      assertThat(ds.getCoordinateSystems()).isNotEmpty();
      assertThat(csys).isNotNull();
      assertThat(csys.getVerticalCT()).isNull();
      assertThat(csys.getProjection()).isNotNull();
      assertThat(csys.getCoordinateTransforms()).isNotEmpty();
      CoordinateTransform ct = csys.getCoordinateTransforms().get(0);
      assertThat(ct.getName()).isEqualTo("TwoD/LambertConformal_Projection");
    }
  }
}
