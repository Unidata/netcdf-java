package ucar.nc2.internal.grid;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridDatasetImpl} */
public class TestGridDatasetImpl {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testOne() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/dgex_46-20141011.ncx4";

    try (NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      Formatter infoLog = new Formatter();
      Optional<GridDatasetImpl> result =
          GridDatasetImpl.create(ds, infoLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
      if (!result.isPresent()) {
        fail();
      }
      GridDatasetImpl gridDataset = result.get();
      System.out.printf("gridDataset =%s%n", gridDataset);
      assertThat(gridDataset.getCoordAxes()).hasSize(14);
      assertThat(gridDataset.getCoordSystems()).hasSize(11);
      assertThat(gridDataset.getGrids()).hasSize(23);
    }
  }
}
