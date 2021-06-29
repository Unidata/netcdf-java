/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grib.grid.GribGridDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

@Category(NeedsCdmUnitTest.class)
public class TestGridDatasetFactory {

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

  private void testOpenNetcdfAsGrid(String endpoint, String gridName, int[] expectedTimeShape, int[] otherCoordShape,
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

      GridTimeCoordinateSystem tcs = cs.getTimeCoordSystem();
      assertThat(tcs).isNotNull();
      assertThat(tcs.getNominalShape())
          .isEqualTo(Arrays.stream(expectedTimeShape).boxed().collect(Collectors.toList()));

      GridHorizCoordinateSystem hcs = cs.getHorizCoordSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.getShape()).isEqualTo(Arrays.stream(expectedHcsShape).boxed().collect(Collectors.toList()));

      List<Integer> expectedShape =
          IntStream.concat(IntStream.concat(Arrays.stream(expectedTimeShape), Arrays.stream(otherCoordShape)),
              Arrays.stream(expectedHcsShape)).boxed().collect(Collectors.toList());
      assertThat(cs.getNominalShape()).isEqualTo(expectedShape);
    }
  }


  @Test
  public void testFileNotFound() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "conventions/fileNot.nc";
    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(filename, errlog).orElseThrow()) {
      fail();
    } catch (FileNotFoundException e) {
      assertThat(e.getMessage()).contains("(No such file or directory)");
    }
  }

  @Test
  public void testFileNotGrid() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "point/point.ncml";
    Formatter errlog = new Formatter();
    try (GribGridDataset gds = GribGridDataset.open(filename, errlog).orElse(null)) {
      assertThat(gds).isNull();
    }
  }
}
