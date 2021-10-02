/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.InvalidRangeException;
import ucar.gcdm.client.GcdmGridDataset;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.grid.*;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.unidata.util.test.TestDir;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test {@link GcdmNetcdfFile} */
@RunWith(Parameterized.class)
public class TestGcdmGridDataset {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "permuteTest.nc"});

      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .gbx9 aggFmrc.xml cg.ncml");
      TestDir.actOnAllParameterized(TestDir.cdmUnitTestDir + "/ft/grid", ff, result, true);

    } catch (Exception e) {
      e.printStackTrace();
    }
    return result;
  }

  private final String filename;
  private final String gcdmUrl;

  public TestGcdmGridDataset(String filename) throws IOException {
    this.filename = filename.replace("\\", "/");
    File file = new File(filename);
    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
  }

  @Test
  public void doOne() throws Exception {
    System.out.printf("TestGcdmNetcdfFile  %s%n", filename);

    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(filename, info)) {
      if (local == null) {
        System.out.printf("TestGcdmNetcdfFile %s NOT a grid %s%n", filename, info);
        return;
      }
      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        if (remote == null) {
          System.out.printf(" Remote %s fails %s%n", filename, info);
          return;
        }
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        boolean ok = compareGridDataset(remote, local);
        if (!ok) {
          System.out.printf("info = '%s'%n", info);
        }
        assertThat(ok).isTrue();

        for (Grid grid : remote.getGrids()) {
          local.findGrid(grid.getName()).ifPresent(remoteGrid -> compareGrid(remoteGrid, remoteGrid));
        }
      }
    }
  }

  private boolean compareGridDataset(GridDataset remote, GridDataset local) {
    System.out.printf(" remote (%s) = %s%n%n", remote.getClass().getName(), remote.getLocation());
    System.out.printf(" local (%s) = %s%n%n", local.getClass().getName(), local.getLocation());

    boolean ok = true;

    assertThat(remote.getName()).isEqualTo(local.getName());
    assertThat(remote.getFeatureType()).isEqualTo(local.getFeatureType());
    assertThat(remote.attributes()).isEqualTo(local.attributes());

    assertThat(remote.getGridCoordinateSystems()).hasSize(local.getGridCoordinateSystems().size());
    for (GridCoordinateSystem gcs : remote.getGridCoordinateSystems()) {
      GridCoordinateSystem rcs = local.getGridCoordinateSystems().stream()
          .filter(cs -> cs.getName().equals(gcs.getName())).findFirst().orElse(null);
      assertThat(rcs).isNotNull();
      assertWithMessage(gcs.getName()).that(compareGridCoordinateSystem(gcs, rcs)).isTrue();
    }

    assertThat(remote.getGridAxes()).hasSize(local.getGridAxes().size());
    for (GridAxis<?> axis : remote.getGridAxes()) {
      GridAxis<?> raxis =
          local.getGridAxes().stream().filter(cs -> cs.getName().equals(axis.getName())).findFirst().orElse(null);
      assertThat((Object) raxis).isNotNull();
      System.out.printf(" axis %s%n", axis.getName());
      assertWithMessage(axis.getName()).that(compareGridAxis(axis, raxis)).isTrue();
    }

    assertThat(remote.getGrids()).hasSize(local.getGrids().size());
    for (Grid grid : remote.getGrids()) {
      Grid rgrid = local.getGrids().stream().filter(cs -> cs.getName().equals(grid.getName())).findFirst().orElse(null);
      assertThat(rgrid).isNotNull();
      System.out.printf(" grid %s%n", grid.getName());
      assertWithMessage(grid.getName()).that(compareGrid(grid, rgrid)).isTrue();
    }

    return ok;
  }

  private boolean compareGridCoordinateSystem(GridCoordinateSystem remote, GridCoordinateSystem local) {
    boolean ok = true;

    assertThat(remote.getName()).isEqualTo(local.getName());
    assertThat(remote.getFeatureType()).isEqualTo(local.getFeatureType());
    assertThat(remote.getVerticalTransform() == null).isEqualTo(local.getVerticalTransform() == null);
    if (remote.getVerticalTransform() != null) {
      assertThat(remote.getVerticalTransform().getName()).isEqualTo(local.getVerticalTransform().getName());
    }
    assertThat(remote.getNominalShape()).isEqualTo(local.getNominalShape());
    assertThat(remote.isZPositive()).isEqualTo(local.isZPositive());

    assertWithMessage(remote.getName())
        .that(compareGridHorizCoordinateSystem(remote.getHorizCoordinateSystem(), local.getHorizCoordinateSystem()))
        .isTrue();
    assertWithMessage(remote.getName())
        .that(compareGridTimeCoordinateSystem(remote.getTimeCoordinateSystem(), local.getTimeCoordinateSystem()))
        .isTrue();

    assertThat(remote.getGridAxes()).hasSize(local.getGridAxes().size());
    for (GridAxis<?> axis : remote.getGridAxes()) {
      GridAxis<?> raxis =
          local.getGridAxes().stream().filter(cs -> cs.getName().equals(axis.getName())).findFirst().orElse(null);
      assertThat((Object) raxis).isNotNull();
    }

    return ok;
  }

  private boolean compareGridHorizCoordinateSystem(GridHorizCoordinateSystem remote, GridHorizCoordinateSystem local) {
    boolean ok = true;

    assertThat(remote.getProjection()).isEqualTo(local.getProjection());
    assertThat(remote.getBoundingBox()).isEqualTo(local.getBoundingBox());
    assertThat(remote.getLatLonBoundingBox()).isEqualTo(local.getLatLonBoundingBox());
    assertThat(remote.getShape()).isEqualTo(local.getShape());
    assertThat(remote.getXHorizAxis().getName()).isEqualTo(local.getXHorizAxis().getName());
    assertThat(remote.getYHorizAxis().getName()).isEqualTo(local.getYHorizAxis().getName());
    assertThat(remote.isCurvilinear()).isEqualTo(local.isCurvilinear());
    assertThat(remote.isGlobalLon()).isEqualTo(local.isGlobalLon());
    assertThat(remote.isLatLon()).isEqualTo(local.isLatLon());
    assertThat(remote.getGeoUnits()).isEqualTo(local.getGeoUnits());

    return ok;
  }

  private boolean compareGridTimeCoordinateSystem(GridTimeCoordinateSystem remote, GridTimeCoordinateSystem local) {
    boolean ok = true;
    assertThat(remote == null).isEqualTo(local == null);
    if (remote == null) {
      return ok;
    }

    assertThat(remote.getType()).isEqualTo(local.getType());
    assertThat(remote.getNominalShape()).isEqualTo(local.getNominalShape());
    assertThat(remote.getBaseDate()).isEqualTo(local.getBaseDate());
    assertThat(remote.getOffsetPeriod()).isEqualTo(local.getOffsetPeriod());
    assertThat(remote.getRuntimeDateUnit()).isEqualTo(local.getRuntimeDateUnit());

    assertThat(remote.getRunTimeAxis() == null).isEqualTo(local.getRunTimeAxis() == null);
    if (remote.getRunTimeAxis() != null) {
      assertThat(remote.getRunTimeAxis().getName()).isEqualTo(local.getRunTimeAxis().getName());
      assertThat(remote.getRunTimeAxis().getNominalSize()).isEqualTo(local.getRunTimeAxis().getNominalSize());
      int nruns = remote.getRunTimeAxis().getNominalSize();
      for (int i = 0; i < nruns; i++) {
        assertThat(remote.getRuntimeDate(i)).isEqualTo(local.getRuntimeDate(i));
        assertThat(compareGridAxis(remote.getTimeOffsetAxis(i), local.getTimeOffsetAxis(i))).isTrue();
      }
    } else {
      assertThat(remote.getRuntimeDate(0)).isEqualTo(local.getRuntimeDate(0));
      assertThat(compareGridAxis(remote.getTimeOffsetAxis(0), local.getTimeOffsetAxis(0))).isTrue();
    }

    assertThat(compareGridAxis(remote.getTimeOffsetAxis(0), local.getTimeOffsetAxis(0))).isTrue();

    return ok;
  }

  private boolean compareGridAxis(GridAxis<?> remote, GridAxis<?> local) {
    boolean ok = true;

    assertThat(remote.getName()).isEqualTo(local.getName());
    assertThat(remote.getNominalSize()).isEqualTo(local.getNominalSize());
    assertThat(remote.getAxisType()).isEqualTo(local.getAxisType());
    assertThat(remote.getUnits()).isEqualTo(local.getUnits());
    assertThat(remote.getDescription()).isEqualTo(local.getDescription());
    assertThat(remote.getDependenceType()).isEqualTo(local.getDependenceType());
    assertThat(remote.getDependsOn()).isEqualTo(local.getDependsOn());
    assertThat(remote.getSpacing()).isEqualTo(local.getSpacing());
    assertThat(remote.getResolution()).isEqualTo(local.getResolution());
    assertThat(remote.isInterval()).isEqualTo(local.isInterval());
    assertThat(remote.isRegular()).isEqualTo(local.isRegular());

    if (remote.isInterval()) {
      GridAxisInterval intvLocal = (GridAxisInterval) remote;
      GridAxisInterval intvRemote = (GridAxisInterval) local;
      for (int i = 0; i < remote.getNominalSize(); i++) {
        assertThat(intvLocal.getCoordInterval(i)).isEqualTo(intvRemote.getCoordInterval(i));
        assertThat(intvLocal.getCoordinate(i)).isEqualTo(intvRemote.getCoordinate(i));
      }
    } else {
      GridAxisPoint intvLocal = (GridAxisPoint) remote;
      GridAxisPoint intvRemote = (GridAxisPoint) local;
      for (int i = 0; i < remote.getNominalSize(); i++) {
        assertThat(intvLocal.getCoordInterval(i)).isEqualTo(intvRemote.getCoordInterval(i));
        assertThat(intvLocal.getCoordinate(i)).isEqualTo(intvRemote.getCoordinate(i));
      }
    }

    return ok;
  }


  private boolean compareGrid(Grid remote, Grid local) {
    boolean ok = true;

    assertThat(remote.getName()).isEqualTo(local.getName());
    assertThat(remote.attributes()).isEqualTo(local.attributes());
    assertThat(remote.getCoordinateSystem().getName()).isEqualTo(local.getCoordinateSystem().getName());
    assertThat(remote.getUnits()).isEqualTo(local.getUnits());
    assertThat(remote.getDescription()).isEqualTo(local.getDescription());
    assertThat(remote.getArrayType()).isEqualTo(local.getArrayType());

    assertWithMessage(remote.getName())
        .that(compareGridHorizCoordinateSystem(remote.getHorizCoordinateSystem(), local.getHorizCoordinateSystem()))
        .isTrue();
    assertWithMessage(remote.getName())
        .that(compareGridTimeCoordinateSystem(remote.getTimeCoordinateSystem(), local.getTimeCoordinateSystem()))
        .isTrue();

    return ok;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////
  private static boolean doRunTime(Grid remote, Grid local, GridSubset subset) throws Exception {
    boolean ok = true;
    GridTimeCoordinateSystem tcs = remote.getCoordinateSystem().getTimeCoordinateSystem();
    if (tcs == null) {
      ok &= doVert(remote, local, subset);
    } else {
      GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
      if (runtimeAxis == null) {
        ok &= doTime(remote, local, subset, 0);
      } else {
        for (int runIdx = 0; runIdx < runtimeAxis.getNominalSize(); runIdx++) {
          subset.setRunTime(tcs.getRuntimeDate(runIdx));
          ok &= doTime(remote, local, subset, runIdx);
        }
      }
    }
    return ok;
  }

  private static boolean doTime(Grid remote, Grid local, GridSubset subset, int runIdx) throws Exception {
    boolean ok = true;
    GridTimeCoordinateSystem tcs = remote.getCoordinateSystem().getTimeCoordinateSystem();
    assertThat(tcs).isNotNull();
    GridAxis<?> timeAxis = tcs.getTimeOffsetAxis(runIdx);
    if (timeAxis == null) {
      ok &= doVert(remote, local, subset);
    } else {
      for (Object coord : timeAxis) {
        subset.setTimeOffsetCoord(coord);
        ok &= doVert(remote, local, subset);
      }
    }
    return ok;
  }

  private static boolean doVert(Grid remote, Grid local, GridSubset subset) throws Exception {
    boolean ok = true;
    GridAxis<?> vertAxis = remote.getCoordinateSystem().getVerticalAxis();
    if (vertAxis == null) {
      ok &= doSubset(remote, local, subset);
    } else {
      for (Object vertCoord : vertAxis) {
        subset.setVertCoord(vertCoord);
        ok &= doSubset(remote, local, subset);
      }
    }
    return ok;
  }

  private static boolean doSubset(Grid remote, Grid local, GridSubset subset)
      throws IOException, InvalidRangeException {
    System.out.printf(" Grid %s subset %s %n", remote.getName(), subset);
    GridReferencedArray localArray = remote.getReader().read();
    GridReferencedArray remoteArray = local.getReader().read();
    Formatter f = new Formatter();
    boolean ok1 =
        CompareArrayToArray.compareData(f, remote.getName(), localArray.data(), remoteArray.data(), true, true);
    if (!ok1) {
      System.out.printf("  FAIL %s%n", f);
    }
    return ok1;
  }

}
