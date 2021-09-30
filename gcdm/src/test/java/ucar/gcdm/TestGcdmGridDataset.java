/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.junit.Ignore;
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
@Ignore("not ready")
public class TestGcdmGridDataset {
  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "permuteTest.nc"});

      FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
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
    System.out.printf("getAbsolutePath %s%n", file.getAbsolutePath());
    System.out.printf("getCanonicalPath %s%n", file.getCanonicalPath());

    // LOOK kludge for now. Also, need to auto start up CmdrServer
    this.gcdmUrl = "gcdm://localhost:16111/" + file.getCanonicalPath();
  }

  @Test
  public void doOne() throws Exception {
    Formatter info = new Formatter();
    try (GridDataset local = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
      if (local == null) {
        System.out.printf("TestGcdmNetcdfFile %s NOT a grid%n", filename);
        return;
      }
      System.out.printf("TestGcdmNetcdfFile call server for %s%n", filename);
      try (GridDataset remote = GridDatasetFactory.openGridDataset(gcdmUrl, info)) {
        assertThat(remote).isNotNull();
        assertThat(remote).isInstanceOf(GcdmGridDataset.class);
        boolean ok = compareGridDataset(local, remote);
        if (!ok) {
          System.out.printf("infp = '%s'%n", info);
        }
        assertThat(ok).isTrue();

        for (Grid localGrid : local.getGrids()) {
          remote.findGrid(localGrid.getName()).ifPresent(remoteGrid -> compareGrid(localGrid, remoteGrid));
        }
      }
    }
  }

  private boolean compareGridDataset(GridDataset local, GridDataset remote) {
    System.out.printf("local (%s) = %s%n%n", local.getClass().getName(), local);
    System.out.printf("====================================================%n");
    System.out.printf("remote (%s) = %s%n%n", remote.getClass().getName(), remote);
    System.out.printf("====================================================%n");

    boolean ok = true;

    assertThat(local.getName()).isEqualTo(remote.getName());
    assertThat(local.getFeatureType()).isEqualTo(remote.getFeatureType());

    for (GridCoordinateSystem gcs : local.getGridCoordinateSystems()) {
      GridCoordinateSystem rcs = remote.getGridCoordinateSystems().stream()
          .filter(cs -> cs.getName().equals(gcs.getName())).findFirst().orElse(null);
      assertThat(rcs).isNotNull();
      assertWithMessage(gcs.getName()).that(rcs).isEqualTo(gcs);
    }

    for (GridAxis<?> axis : local.getGridAxes()) {
      GridAxis<?> raxis =
          remote.getGridAxes().stream().filter(cs -> cs.getName().equals(axis.getName())).findFirst().orElse(null);
      assertThat((Object) raxis).isNotNull();
      assertThat((Object) raxis).isEqualTo(axis);
    }

    for (Grid grid : local.getGrids()) {
      Grid rgrid =
          remote.getGrids().stream().filter(cs -> cs.getName().equals(grid.getName())).findFirst().orElse(null);
      assertThat(rgrid).isNotNull();
      assertWithMessage(grid.getName()).that(rgrid.getCoordinateSystem()).isEqualTo(grid.getCoordinateSystem());
    }

    return ok;
  }

  private static boolean compareGrid(Grid local, Grid remote) {
    GridSubset subset = GridSubset.create();
    try {
      return doRunTime(local, remote, subset);
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private static boolean doRunTime(Grid local, Grid remote, GridSubset subset) throws Exception {
    boolean ok = true;
    GridTimeCoordinateSystem tcs = local.getCoordinateSystem().getTimeCoordinateSystem();
    if (tcs == null) {
      ok &= doVert(local, remote, subset);
    } else {
      GridAxisPoint runtimeAxis = tcs.getRunTimeAxis();
      if (runtimeAxis == null) {
        ok &= doTime(local, remote, subset, 0);
      } else {
        for (int runIdx = 0; runIdx < runtimeAxis.getNominalSize(); runIdx++) {
          subset.setRunTime(tcs.getRuntimeDate(runIdx));
          ok &= doTime(local, remote, subset, runIdx);
        }
      }
    }
    return ok;
  }

  private static boolean doTime(Grid local, Grid remote, GridSubset subset, int runIdx) throws Exception {
    boolean ok = true;
    GridTimeCoordinateSystem tcs = local.getCoordinateSystem().getTimeCoordinateSystem();
    assertThat(tcs).isNotNull();
    GridAxis<?> timeAxis = tcs.getTimeOffsetAxis(runIdx);
    if (timeAxis == null) {
      ok &= doVert(local, remote, subset);
    } else {
      for (Object coord : timeAxis) {
        subset.setTimeOffsetCoord(coord);
        ok &= doVert(local, remote, subset);
      }
    }
    return ok;
  }

  private static boolean doVert(Grid local, Grid remote, GridSubset subset) throws Exception {
    boolean ok = true;
    GridAxis<?> vertAxis = local.getCoordinateSystem().getVerticalAxis();
    if (vertAxis == null) {
      ok &= doSubset(local, remote, subset);
    } else {
      for (Object vertCoord : vertAxis) {
        subset.setVertCoord(vertCoord);
        ok &= doSubset(local, remote, subset);
      }
    }
    return ok;
  }

  private static boolean doSubset(Grid local, Grid remote, GridSubset subset)
      throws IOException, InvalidRangeException {
    System.out.printf(" Grid %s subset %s %n", local.getName(), subset);
    GridReferencedArray localArray = local.getReader().read();
    GridReferencedArray remoteArray = remote.getReader().read();
    Formatter f = new Formatter();
    boolean ok1 =
        CompareArrayToArray.compareData(f, local.getName(), localArray.data(), remoteArray.data(), true, true);
    if (!ok1) {
      System.out.printf("  FAIL %s%n", f);
    }
    return ok1;
  }

}
