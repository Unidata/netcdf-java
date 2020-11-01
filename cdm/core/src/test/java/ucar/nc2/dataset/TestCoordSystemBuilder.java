package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestCoordSystemBuilder {

  @Test
  public void testSeparateGroups() throws IOException {
    // This has Best and TwoD, and the coordSys are mixing them up
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/noaaport/NDFD-CONUS_noaaport.ncx4";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      for (Variable v : ds.getVariables()) {
        System.out.printf(" Check variable %s%n", v.getFullName());
        VariableDS vds = (VariableDS) v;
        Group parent = v.getParentGroup();
        for (CoordinateSystem csys : vds.getCoordinateSystems()) {
          System.out.printf("  Check csys %s%n", csys.getName());
          assertThat(csys.isCoordinateSystemFor(v));
          for (Dimension dim : csys.getDomain()) {
            assertThat(parent.findDimension(dim) == dim).isTrue();
          }
          for (CoordinateAxis axis : csys.getCoordinateAxes()) {
            System.out.printf("   Check axis %s%n", axis.getFullName());
            for (Dimension dim : axis.getDimensions()) {
              assertThat(parent.findDimension(dim) == dim).isTrue();
            }
          }
        }
      }
    }
  }
}
