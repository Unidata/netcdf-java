/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DebugFlags;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

/** Test GridCoordinateSystem and subsets are ok */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestGridSubsetCoordinateSystem {

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4", "Pressure_surface"});
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/tp/GFS_Global_onedeg_ana_20150326_0600.grib2.ncx4",
            "Temperature_sigma"});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Soil_temperature_depth_below_surface_layer"});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "gribCollections/gfs_2p5deg/gfs_2p5deg.ncx4",
        "Soil_temperature_depth_below_surface_layer"});

    return result;
  }

  final String filename, gridName;

  public TestGridSubsetCoordinateSystem(String filename, String gridName) {
    this.filename = filename;
    this.gridName = gridName;
  }

  @Test
  public void testGridSubsetCoordinateSystem() throws Exception {
    Grib.setDebugFlags(DebugFlags.create("Grib/indexOnly")); // LOOK needed?
    System.err.printf("%nOpen %s grid='%s'%n", filename, gridName);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem csys = grid.getCoordinateSystem();
      assertThat(csys).isNotNull();
      GridTimeCoordinateSystem tcs = csys.getTimeCoordinateSystem();
      assertThat(tcs).isNotNull();
      GridHorizCoordinateSystem hcs = csys.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();

      GridReferencedArray geoArray = grid.getReader().setTimeFirst().read();
      MaterializedCoordinateSystem mcs = geoArray.getMaterializedCoordinateSystem();

      for (GridAxis<?> axis : csys.getGridAxes()) {
        Optional<GridAxis<?>> found = mcs.getGridAxes().stream().filter(ga -> ga.name.equals(axis.name)).findFirst();
        assertWithMessage(axis.getName()).that(found.isPresent()).isTrue();
      }

    } finally {
      Grib.setDebugFlags(DebugFlags.create(""));
    }
  }

}
