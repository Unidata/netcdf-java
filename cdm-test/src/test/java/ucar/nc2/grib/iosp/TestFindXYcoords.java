/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.iosp;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test coordinate extraction on grib file.
 */
public class TestFindXYcoords {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCoordExtract() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/coordExtract/TestCoordExtract.grib2";
    System.out.printf("%s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();

      Grid grid = gds.findGrid("Convective_inhibition_surface").orElseThrow();
      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();

      GridHorizCoordinateSystem.CoordReturn result = hcs.findXYindexFromCoord(-91.140575, 41.3669944444).orElseThrow();

      System.out.printf("result = %s%n", result);
      assertThat(result.xindex).isEqualTo(538);
      assertThat(result.yindex).isEqualTo(97);
    }
  }
}
