/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.grib.GribCollectionMissing.readAll;

/**
 * Problems with missing data in Grib Collections.
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribCollectionProblem {

  @Test
  public void testProblem() {
    String filename = TestDir.cdmUnitTestDir + "gribCollections/gfs_conus80/gfsConus80_dir.ncx4";
    String gridName = "Absolute_vorticity_isobaric";
    long start = System.currentTimeMillis();
    System.out.println("\n\nReading File " + filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      GribCollectionMissing.Count gridCount = new GribCollectionMissing.Count();
      readAll(grid, gridCount);
      long took = System.currentTimeMillis() - start;
      float r = ((float) took) / gridCount.nread;
      System.out.printf("%n%80s == %d/%d (%d)%n", "total", gridCount.nmiss, gridCount.nread, gridCount.nerrs);
      System.out.printf("%n   that took %d secs total, %f msecs per record%n", took / 1000, r);

    } catch (IOException ioe) {
      ioe.printStackTrace();
      Formatter out = new Formatter(System.out);
      GribCdmIndex.gribCollectionCache.showCache(out);
    }
  }
}
