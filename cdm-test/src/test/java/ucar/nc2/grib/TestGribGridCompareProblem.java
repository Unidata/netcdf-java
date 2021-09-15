package ucar.nc2.grib;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.grid.DatasetClassifier;
import ucar.nc2.internal.grid.GridNetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

public class TestGribGridCompareProblem {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testProblem() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/20141024/gfsConus80_dir-20141024.ncx4";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      if (grido.isPresent()) {
        GridNetcdfDataset gridDataset = grido.get();
        if (!Iterables.isEmpty(gridDataset.getGrids())) {
          DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
          DatasetClassifier.CoordSysClassifier classifier =
              dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
          assertThat(classifier).isNotNull();
          assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
        }
      }
    }
  }
}
