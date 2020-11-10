package ucar.nc2.internal.dataset;

import com.google.common.collect.Iterables;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Range;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.internal.grid.GridDatasetImpl;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.category.NeedsExternalResource;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

@Ignore("files not available")
public class TestDatasetClassifierSpecial {

  @Test
  @Category(NeedsExternalResource.class)
  public void testNamPolar() throws IOException {
    String filename = "/media/snake/Elements/data/grib/idd/namPolar90/NAM_Polar_90km_20131203_0000.grib2";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("MRUTP")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
        }
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridDatasetImpl> grido = GridDatasetImpl.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridDatasetImpl gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  @Category(NeedsExternalResource.class)
  public void testNamPolarCollection() throws IOException {
    String filename = "/media/snake/Elements/data/grib/idd/namPolar90/NamPolar90.ncx4";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("MRUTP")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
        }
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridDatasetImpl> grido = GridDatasetImpl.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridDatasetImpl gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

}
