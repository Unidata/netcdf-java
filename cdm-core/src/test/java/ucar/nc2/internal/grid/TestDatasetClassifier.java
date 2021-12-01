package ucar.nc2.internal.grid;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link DatasetClassifier} */
public class TestDatasetClassifier {

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testNamPolar() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM_Polar_90km_20201027_0000.grib2.ncx4";

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
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
  public void testNamPolarCollection() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/Polar_90km/NAM-Polar_90km.ncx4";

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertWithMessage(errlog.toString()).that(grido.isPresent()).isTrue();
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
  public void testNoGrids() throws IOException {
    // No Grids found because unidentified ensemble, time axis.
    String filename = TestDir.cdmLocalTestDataDir + "testNested.ncml";
    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      Optional<GridNetcdfDataset> grido = GridNetcdfDataset.create(ds, errlog);
      assertThat(grido.isPresent()).isFalse();
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
      System.out.printf("testScalarRuntime %s%n", gridDataset.getLocation());

      Optional<Grid> grido =
          gridDataset.findGrid("Convective_available_potential_energy_pressure_difference_layer_ens");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt (scalar), t, ens, z, y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(6);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat(tcs.getRunTimeAxis().getDependenceType() == GridAxisDependenceType.scalar).isTrue();
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.scalar);
      assertThat((Object) gcs.getEnsembleAxis()).isNotNull();
      assertThat((Object) gcs.getVerticalAxis()).isNotNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(1, 65, 21, 1, 181, 360));
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
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt, to, z (scaler), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(5);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat(tcs.getRunTimeAxis().getDependenceType() == GridAxisDependenceType.scalar).isFalse();
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat((Object) tcs.getTimeOffsetAxis(0)).isNotNull();
      assertThat((Object) gcs.getEnsembleAxis()).isNull();
      assertThat((Object) gcs.getVerticalAxis()).isNotNull();
      assertThat((Object) gcs.getVerticalAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.scalar);
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(5, 10, 77, 97));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testFMRC() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/cfsr/pwat.gdas.199612.grb2";

    try (NetcdfDataset ds = NetcdfDatasets.openDataset(filename)) {
      Formatter errlog = new Formatter();
      DatasetClassifier classifier = new DatasetClassifier(ds, errlog);
      assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);

      Optional<GridNetcdfDataset> gdso = GridNetcdfDataset.create(ds, errlog);
      assertThat(gdso.isPresent()).isTrue();
      GridNetcdfDataset gridDataset = gdso.get();
      assertThat(Iterables.isEmpty(gridDataset.getGrids())).isFalse();
      assertThat(gridDataset.getFeatureType()).isEqualTo(FeatureType.GRID);
      System.out.printf("testFMRC %s%n", gridDataset.getLocation());

      Optional<Grid> grido = gridDataset.findGrid("Precipitable_water_entire_atmosphere_single_layer");
      assertThat(grido.isPresent()).isTrue();
      Grid grid = grido.get();
      GridCoordinateSystem gcs = grid.getCoordinateSystem();
      assertThat(gcs).isNotNull();
      GridTimeCoordinateSystem tcs = gcs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();

      // rt, to, t (depend), y, x
      assertThat(Iterables.size(gcs.getGridAxes())).isEqualTo(4);
      assertThat((Object) tcs.getRunTimeAxis()).isNotNull();
      assertThat((Object) tcs.getTimeOffsetAxis(0)).isNotNull();
      assertThat(tcs.getTimeOffsetAxis(0).getDependenceType()).isEqualTo(GridAxisDependenceType.independent);
      assertThat(tcs.getRunTimeAxis().getDependenceType()).isEqualTo(GridAxisDependenceType.dependent);
      assertThat((Object) gcs.getEnsembleAxis()).isNull();
      assertThat((Object) gcs.getVerticalAxis()).isNull();
      assertThat((Object) gcs.getYHorizAxis()).isNotNull();
      assertThat((Object) gcs.getXHorizAxis()).isNotNull();

      assertThat(gcs.getNominalShape()).isEqualTo(ImmutableList.of(868, 576, 1152));
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  @Ignore("TODO not dealing with multiple groups; coverage ver6 looks wrong also (ok in ver5)")
  public void problemWithGroups() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/ecmwf/mad/MAD10090000100900001";

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
        assertThat(classifier).isNotNull();
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.GRID);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMRUTP2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/anal/HRRRanalysis.ncx4";

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
  public void testMRUTP3() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/rdavm/ds627.0/ei.oper.an.pv/ds627.0_46.ncx4";

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
  public void testBothXYandLatLon() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/coverage/testCFwriter.nc";
    String gridName = "Temperature";
    System.out.printf("file %s coverage %s%n", filename, gridName);

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
  public void testCurvilinear1D() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/coverage/Run_20091025_0000.nc";
    String gridName = "temp";
    System.out.printf("file %s coverage %s%n", filename, gridName);

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
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCurvilinear() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc";
    String gridName = "temp";
    System.out.printf("file %s coverage %s%n", filename, gridName);

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
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCurvilinear2() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "conventions/cf/signell/signell_bathy_fixed.nc";
    String gridName = "h";
    System.out.printf("file %s coverage %s%n", filename, gridName);

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
        assertThat(classifier.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      }
    }
  }



}
