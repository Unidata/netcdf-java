package ucar.nc2.internal.dataset;

import com.google.common.collect.Iterables;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Range;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ft2.coverage.CoverageCollection;
import ucar.nc2.ft2.coverage.CoverageDatasetFactory;
import ucar.nc2.ft2.coverage.FeatureDatasetCoverage;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.internal.grid.GridNetcdfDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;

public class TestDatasetClassifier {

  @Test
  public void testNoGrids() throws IOException {
    // Comes back as a GRID, but no Grids found because unidentified ensemble, time axis.
    // Coverage handles wrong
    String filename = TestDir.cdmLocalTestDataDir + "testNested.ncml";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isTrue();
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testScalarRuntime() throws IOException {
    // scalar runtime
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GEFS_Global_1p0deg_Ensemble_20120215_0000.grib2";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();

      Optional<Grid> grido =
          gridDataset.findGrid("Convective_available_potential_energy_pressure_difference_layer_ens");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      // rt (scalar), t, ens, z, y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(6);
      assertThat(gcs.getRunTimeAxis()).isNotNull();
      assertThat(gcs.getRunTimeAxis().getDependenceType() == GridAxis.DependenceType.scalar).isTrue();
      assertThat(gcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxis.DependenceType.scalar);
      assertThat(gcs.getTimeAxis()).isNotNull();
      assertThat(gcs.getEnsembleAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(new int[] {65, 21, 1, 181, 360});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testScalarVert() throws IOException {
    // scalar runtime
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/ukmo.nc";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();

      Optional<Grid> grido = gridDataset.findGrid("temperature_2m");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      // rt, to, z (scaler), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(5);
      assertThat(gcs.getRunTimeAxis()).isNotNull();
      assertThat(gcs.getRunTimeAxis().getDependenceType() == GridAxis.DependenceType.scalar).isFalse();
      assertThat(gcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxis.DependenceType.independent);
      assertThat(gcs.getTimeAxis()).isNull();
      assertThat(gcs.getTimeOffsetAxis()).isNotNull();
      assertThat(gcs.getEnsembleAxis()).isNull();
      assertThat(gcs.getVerticalAxis()).isNotNull();
      assertThat(gcs.getVerticalAxis().getDependenceType()).isEqualTo(GridAxis.DependenceType.scalar);
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(new int[] {5, 10, 77, 97});
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testFMRC() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/cfsr/pwat.gdas.199612.grb2";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      CoverageCollection cc = covDataset.getSingleCoverageCollection();
      assertThat(cc.getCoverageType()).isEqualTo(FeatureType.FMRC);
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();
      assertThat(gridDataset.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<Grid> grido = gridDataset.findGrid("Precipitable_water_entire_atmosphere_single_layer");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      // rt, to, t (depend), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(4);
      assertThat(gcs.getRunTimeAxis()).isNotNull();
      assertThat(gcs.getTimeOffsetAxis()).isNotNull();
      assertThat(gcs.getTimeOffsetAxis().getDependenceType()).isEqualTo(GridAxis.DependenceType.independent);
      assertThat(gcs.getTimeAxis()).isNull();
      assertThat(gcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxis.DependenceType.independent);
      assertThat(gcs.getEnsembleAxis()).isNull();
      assertThat(gcs.getVerticalAxis()).isNull();
      assertThat(gcs.getYHorizAxis()).isNotNull();
      assertThat(gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(new int[] {124, 7, 576, 1152});
    }
  }

  // @Test TODO not dealing with multiple groups; coverage ver6 looks wrong also (ok in ver5)
  @Category(NeedsCdmUnitTest.class)
  public void testProblemWithGroups() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ecmwf/mad/MAD10090000100900001";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  /*
   * Non-orthogoinal offsets: Differs for hour 0 vs 12:
   * time2D: time runtime=reftime nruns=16 ntimes=16 isOrthogonal=false isRegular=true
   * All time values= 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, (n=18)
   * hour 0: time: 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, 186, 192, (16)
   * hour 12: time: 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150, 156, 162, 168, 174, 180, (16)
   */
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegularTimeOffset() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gdsHashChange/noaaport/NDFD-CONUS_noaaport.ncx4";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("TwoD")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.FMRC);
        }
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
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

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMRUTP() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/GFS/Global_0p25deg_ana/GFS-Global_0p25deg_ana.ncx4";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("MRUTP")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
        }
      }
    }

    // MRUTP has a runtime and time sharing the same dimension.
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMRUTP2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/anal/HRRRanalysis.ncx4";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("MRUTP")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
        }
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  @Ignore("fix when cdmUnit Test is repaired")
  public void testMRUTP3() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.0/ei.oper.an.pv/ds627.0_46.ncx4";
    try (FeatureDatasetCoverage covDataset = CoverageDatasetFactory.open(filename)) {
      for (CoverageCollection cc : covDataset.getCoverageCollections()) {
        if (cc.getName().endsWith("MRUTP")) {
          assertThat(cc.getCoverageType()).isEqualTo(FeatureType.GRID);
        }
      }
    }

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = grido.get();
      if (!Iterables.isEmpty(gridDataset.getGrids())) {
        DatasetClassifier dclassifier = new DatasetClassifier(ds, errlog);
        DatasetClassifier.CoordSysClassifier classifier =
            dclassifier.getCoordinateSystemsUsed().stream().findFirst().orElse(null);
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

}
