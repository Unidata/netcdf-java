/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.array.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/** Compare reading netcdf with Array */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestReadGridCompare {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    FileFilter ff = TestDir.FileFilterSkipSuffix(".cdl .ncml perverse.nc");
    List<Object[]> result = new ArrayList<>(500);
    try {
      result.add(new Object[] {TestDir.cdmLocalTestDataDir + "ncml/nc/cldc.mean.nc"});
    } catch (Exception e) {
      e.printStackTrace();
    }

    return result;
  }

  /////////////////////////////////////////////////////////////

  public TestReadGridCompare(String filename) {
    this.filename = filename;
  }

  private final String filename;

  @Test
  public void compareGrid() throws IOException, InvalidRangeException {
    // these are failing in old code.
    if (filename.endsWith("cdm_sea_soundings.nc4"))
      return;
    if (filename.endsWith("IntTimSciSamp.nc"))
      return;

    compareGrid(filename);
  }

  public static void compareGrid(String filename) throws IOException, InvalidRangeException {
    Formatter errlog = new Formatter();
    try (GridDataset ncd = GridDatasetFactory.openGridDataset(filename, errlog)) {
      System.out.println("compareNetcdfDataset: " + ncd.getLocation());

      boolean ok = true;
      for (Grid grid : ncd.getGrids()) {
        GridCoordinateSystem gcs = grid.getCoordinateSystem();
        assertThat(gcs.isLatLon()).isTrue();

        GridSubset subset = new GridSubset();
        subset.setTime(CalendarDate.parseISOformat(null, "1960-01-01T00:00:00Z"));
        GridReferencedArray geoArray = grid.readData(subset);
        Array<Number> data = geoArray.data();
        /*
         * if (geoArray != null) {
         * System.out.printf("  check %s %s%n", v.getDataType(), v.getNameAndDimensions());
         * Formatter f = new Formatter();
         * boolean ok1 = CompareArrayToMa2.compareData(f, v.getShortName(), org, array, false, true);
         * if (!ok1) {
         * System.out.printf("%s%n", f);
         * }
         * ok &= ok1;
         * }
         */
      }
      assertThat(ok).isTrue();
    }
  }

}

