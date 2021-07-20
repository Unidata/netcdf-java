/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;

/** Test {@link GridDatasetFactory} CoordinateSystem */
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCoordinateSystem {

  // Heres a CF-encoded FMRC with offset (orthogonal). Possible offset bounds are miscoded
  // lat, lon, offset coordinates have bounds.
  // lat coded as nominal point, seems correct
  // lon coded as regular point, seems correct
  // offset coded as discontiguous interval, should probably be nominal point.
  // midpoints== 6.000000 12.000000 18.000000 24.000000 30.000000 36.000000 42.000000 48.000000 60.000000 72.000000
  // bound1== 6.000000 12.000000 18.000000 24.000000 30.000000 36.000000 42.000000 48.000000 60.000000 72.000000
  // bound2== 6.000000 12.000000 18.000000 24.000000 30.000000 36.000000 42.000000 48.000000 60.000000 72.000000
  @Test
  public void testCFmiscodedBounds() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/ukmo.nc";
    String gridName = "temperature_2m";
    testOpenNetcdfAsGrid(filename, gridName, new int[] {5, 10}, new int[] {}, new int[] {77, 97});
  }

  @Test
  public void testWithSingleTime() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "/ft/grid/cg/CG2006158_150000h_usfc.nc";
    String gridName = "CGusfc";
    testOpenNetcdfAsGrid(filename, gridName, new int[] {1}, new int[] {1}, new int[] {29, 26});
  }

  @Test
  public void testWithTimeOnly() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/NAM_CONUS_80km_20070501_1200.nc";
    String gridName = "RH";
    testOpenNetcdfAsGrid(filename, gridName, new int[] {11}, new int[] {19}, new int[] {65, 93});
  }

  @Test
  public void testDuplicateGrids() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/grid/namExtract/20060926_0000.nc";
    String gridName = "Precipitable_water";
    int ngrids = testOpenNetcdfAsGrid(filename, gridName, new int[] {2}, new int[] {}, new int[] {103, 108});
    assertThat(ngrids).isEqualTo(8);
  }

  private int testOpenNetcdfAsGrid(String endpoint, String gridName, int[] expectedTimeShape, int[] otherCoordShape,
      int[] expectedHcsShape) throws IOException {
    System.out.printf("Test Dataset %s%n", endpoint);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openNetcdfAsGrid(endpoint, errlog)) {
      assertThat(gds).isNotNull();
      System.out.printf("Test Dataset %s%n", gds.getLocation());

      Grid grid = gds.findGrid(gridName).orElse(null);
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();

      GridTimeCoordinateSystem tcs = cs.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      assertThat(tcs.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.getShape()).isEqualTo(Arrays.stream(expectedHcsShape).boxed().collect(Collectors.toList()));

      List<Integer> expectedShape =
          IntStream.concat(IntStream.concat(Arrays.stream(expectedTimeShape), Arrays.stream(otherCoordShape)),
              Arrays.stream(expectedHcsShape)).boxed().collect(Collectors.toList());
      assertThat(cs.getNominalShape()).isEqualTo(expectedShape);

      return gds.getGrids().size();
    }
  }
}
