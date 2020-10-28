package ucar.nc2.internal.dataset;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.grid.GridDatasetImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

public class TestDatasetClassifier {

  @Test
  public void testProblem() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "testNested.ncml";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      Optional<GridDatasetImpl> grido = GridDatasetImpl.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridDatasetImpl gridDataset = grido.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isTrue();
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testScalerRuntime() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/anal/HRRR_CONUS_2p5km_ana_20150706_2000.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
    }
  }

  // @Test
  public void testScalerRuntime2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/dgex/20141011/DGEX_CONUS_12km_20141011_0600.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
    }
  }
}
