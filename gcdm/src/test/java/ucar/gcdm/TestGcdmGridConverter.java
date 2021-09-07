/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridTimeCoordinateSystem;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link GcdmGridConverter} by roundtripping and comparing with original. Metadata only. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGcdmGridConverter {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "permuteTest.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/namExtract/20060926_0000.nc"});
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/fmrc/GFS_Puerto_Rico_191km_20090729_0000.nc"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/coards/inittest24.QRIDV07200.ncml"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "conventions/nuwg/2003021212_avn-x.nc"});

      result.add(new Object[] {
          TestDir.cdmUnitTestDir + "tds_index/NCEP/NAM/CONUS_80km/NAM_CONUS_80km_20201027_0000.grib1.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/grid/ensemble/jitka/ECME_RIZ_201201101200_00600_GB.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4"});

      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS-Radar.ncx4"});
      result
          .add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/MRMS/Radar/MRMS_Radar_20201027_0000.grib2.ncx4"});

      // Offset (orthogonal)
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/CPC/NCEP_NDFD_CPC_Experimental.ncx4"});

      // orth, reg
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Alaska/NCEP_ALASKA_MODEL_BLEND.ncx4"});

      // OffsetRegular
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT_ver7.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NBM/Ocean/NCEP_OCEAN_MODEL_BLEND.ncx4"});

      // OffsetIrregular
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/CPC/NDFD_CPC_CONUS_CONDUIT.ncx4"});
      result.add(new Object[] {TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4"});

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmGridConverter(String filename) throws IOException {
    this.filename = filename.replace("\\", "/");
    File file = new File(filename);
    System.out.printf("getAbsolutePath %s%n", file.getAbsolutePath());
    System.out.printf("getCanonicalPath %s%n", file.getCanonicalPath());

    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testExample() throws Exception {
    Path path = Paths.get(this.filename);
    roundtrip(path);
  }

  public static void roundtrip(Path path) throws Exception {
    Formatter errlog = new Formatter();
    try (GridDataset gridDataset = GridDatasetFactory.openGridDataset(path.toString(), errlog)) {
      assertThat(gridDataset).isNotNull();

      GcdmGridProto.GridDataset proto = GcdmGridConverter.encodeGridDataset(gridDataset);
      GcdmGridDataset.Builder builder = GcdmGridDataset.builder();
      GcdmGridConverter.decodeGridDataset(proto, builder, errlog);
      GcdmGridDataset roundtrip = builder.build(false);

      compareValuesEqual(roundtrip, gridDataset, false);
    }
  }

  public static void compareValuesEqual(GridDataset roundtrip, GridDataset expected, boolean skipTimes) {
    assertThat(roundtrip.getName()).startsWith(expected.getName());
    // assertThat(roundtrip.getLocation()).isEqualTo(expected.getLocation());
    assertThat(roundtrip.getFeatureType()).isEqualTo(expected.getFeatureType());
    compareValuesEqual(roundtrip.attributes(), expected.attributes());

    assertThat(roundtrip.getGridAxes()).hasSize(expected.getGridAxes().size());
    for (GridAxis<?> axis : roundtrip.getGridAxes()) {
      GridAxis<?> expectedAxis = expected.getGridAxes().stream().filter(a -> a.getName().equals(axis.getName()))
          .findFirst().orElseThrow(() -> new RuntimeException("Cant find axis: " + axis.getName()));
      if (skipTimes && axis.getAxisType().isTime()) {
        continue;
      }
      compareValuesEqual(axis, expectedAxis);
    }

    assertThat(roundtrip.getGridCoordinateSystems()).hasSize(expected.getGridCoordinateSystems().size());
    for (GridCoordinateSystem csys : roundtrip.getGridCoordinateSystems()) {
      GridCoordinateSystem expectedCsys =
          expected.getGridCoordinateSystems().stream().filter(cs -> cs.getName().equals(csys.getName())).findFirst()
              .orElseThrow(() -> new RuntimeException("Cant find csys: " + csys.getName()));

      compareValuesEqual(csys, expectedCsys);
    }

    assertThat(roundtrip.getGrids()).hasSize(expected.getGrids().size());
    for (Grid grid : roundtrip.getGrids()) {
      Grid expectedGrid = expected.getGrids().stream().filter(a -> a.getName().equals(grid.getName())).findFirst()
          .orElseThrow(() -> new RuntimeException("Cant find grid: " + grid.getName()));
      compareValuesEqual(grid, expectedGrid);
    }
  }

  public static void compareValuesEqual(GridCoordinateSystem csys, GridCoordinateSystem expected) {
    assertThat(csys.getName()).isEqualTo(expected.getName());
    compareValuesEqual(csys.getHorizCoordinateSystem(), expected.getHorizCoordinateSystem());
    compareValuesEqual(csys.getTimeCoordinateSystem(), expected.getTimeCoordinateSystem());
  }

  public static void compareValuesEqual(GridHorizCoordinateSystem hsys, GridHorizCoordinateSystem expected) {
    compareValuesEqual(hsys.getXHorizAxis(), expected.getXHorizAxis());
    compareValuesEqual(hsys.getYHorizAxis(), expected.getYHorizAxis());
    assertThat(hsys.getProjection()).isEqualTo(expected.getProjection());
    assertThat(hsys.isLatLon()).isEqualTo(expected.isLatLon());
    assertThat(hsys.isCurvilinear()).isEqualTo(expected.isCurvilinear());
    assertThat(hsys.isGlobalLon()).isEqualTo(expected.isGlobalLon());
    assertThat(hsys.getShape()).isEqualTo(expected.getShape());
    assertThat(hsys.getGeoUnits()).isEqualTo(expected.getGeoUnits());
    assertThat(hsys.getLatLonBoundingBox().nearlyEquals(expected.getLatLonBoundingBox())).isTrue();
    assertThat(hsys.getBoundingBox().nearlyEquals(expected.getBoundingBox())).isTrue();
  }

  public static void compareValuesEqual(GridTimeCoordinateSystem tsys, GridTimeCoordinateSystem expected) {
    assertThat(tsys == null).isEqualTo(expected == null);
    if (tsys == null) {
      return;
    }
    assertThat(tsys.getType()).isEqualTo(expected.getType());
    assertThat(tsys.getRuntimeDateUnit()).isEqualTo(expected.getRuntimeDateUnit());
    assertThat(tsys.getBaseDate()).isEqualTo(expected.getBaseDate());
    if (!tsys.getNominalShape().equals(expected.getNominalShape())) {
      System.out.printf("HEY getNominalShape%n");
    }
    // assertWithMessage(tsys.toString()).that(tsys.getNominalShape()).isEqualTo(expected.getNominalShape());
    // assertThat(tsys.getMaterializedShape()).isEqualTo(expected.getMaterializedShape());
    compareValuesEqual(tsys.getRunTimeAxis(), expected.getRunTimeAxis());
    compareValuesEqual(tsys.getTimeOffsetAxis(0), expected.getTimeOffsetAxis(0));
    if (tsys.getRunTimeAxis() != null) {
      for (int i = 0; i < tsys.getRunTimeAxis().getNominalSize(); i++) {
        compareValuesEqual(tsys.getTimeOffsetAxis(i), expected.getTimeOffsetAxis(i));
        // assertThat(tsys.getTimesForRuntime(i)).isEqualTo(expected.getTimesForRuntime(i));
      }
    }
  }

  public static void compareValuesEqual(GridAxis<?> axis, GridAxis<?> expected) {
    assertThat(axis == null).isEqualTo(expected == null);
    if (axis == null) {
      return;
    }
    assertThat(axis.getAxisType().isTime()).isEqualTo(expected.getAxisType().isTime());
    if (!axis.getAxisType().isTime()) {
      assertThat(axis.getAxisType()).isEqualTo(expected.getAxisType());
    }
    if (!axis.getDescription().equals(expected.getDescription())) {
      System.out.printf("HEY %s getDescription %s != %s %n", axis.getName(), axis.getDescription(),
          expected.getDescription());
    }
    if (!axis.getUnits().equals(expected.getUnits())) {
      System.out.printf("HEY %s getUnits %s != %s %n", axis.getName(), axis.getUnits(), expected.getUnits());
    }
    assertThat(axis.getName()).isEqualTo(expected.getName());
    if (!axis.getAxisType().isTime()) {
      assertThat(axis.getDescription()).isEqualTo(expected.getDescription());
      assertThat(axis.getUnits()).isEqualTo(expected.getUnits());
    }
    if (!axis.getSpacing().equals(expected.getSpacing())) {
      System.out.printf("HEY %s getSpacing %s != %s %n", axis.getName(), axis.getSpacing(), expected.getSpacing());
    }
    assertWithMessage(axis.getName()).that(axis.getSpacing()).isEqualTo(expected.getSpacing());
    assertThat(axis.isInterval()).isEqualTo(expected.isInterval());
    // LOOK assertThat(axis.getResolution()).isEqualTo(expected.getResolution());
    // assertWithMessage(axis.getName()).that(axis.getDependenceType()).isEqualTo(expected.getDependenceType());
    // assertThat(axis.getDependsOn()).isEqualTo(expected.getDependsOn());
    // assertThat(axis.getNominalSize()).isEqualTo(expected.getNominalSize());
    compareValuesEqual(axis.attributes(), expected.attributes());
  }


  public static void compareValuesEqual(Grid grid, Grid expectedGrid) {
    if (grid.getClass() == expectedGrid.getClass()) {
      assertThat(grid).isEqualTo(expectedGrid);
    }
    assertThat(grid.getCoordinateSystem().getName()).isEqualTo(expectedGrid.getCoordinateSystem().getName());
    assertThat(grid.getName()).isEqualTo(expectedGrid.getName());
    assertThat(grid.getDescription()).isEqualTo(expectedGrid.getDescription());
    assertThat(grid.getUnits()).isEqualTo(expectedGrid.getUnits());
    assertThat(grid.getArrayType()).isEqualTo(expectedGrid.getArrayType());
    compareValuesEqual(grid.attributes(), expectedGrid.attributes());
  }

  // Just require that all expected attributes are present and equal
  public static void compareValuesEqual(AttributeContainer atts, AttributeContainer expected) {
    // assertThat(atts).isEqualTo(expected);
    // assertThat(atts.getName()).isEqualTo(expected.getName());
    for (Attribute att : expected) {
      Attribute expectedAtt = atts.findAttribute(att.getName());
      assertWithMessage(att.getName()).that(expectedAtt).isNotNull();
      assertThat(att).isEqualTo(expectedAtt);
    }
  }

}
