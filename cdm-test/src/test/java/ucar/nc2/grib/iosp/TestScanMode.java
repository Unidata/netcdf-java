/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.iosp;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.InvalidRangeException;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

@Category(NeedsCdmUnitTest.class)
public class TestScanMode {

  @Test
  public void testScanMode0() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/S-HSAF-h03_20131214_1312_rom.grb";
    System.out.printf("testScanMode0 openGrid %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("Instantaneous_rain_rate").orElseThrow();

      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      GridHorizCoordinateSystem.CoordReturn result = hcs.findXYindexFromCoord(-432, 5016).orElseThrow();
      System.out.printf("  CoordReturn = %s%n", result);
      assertThat(result.xindex).isEqualTo(619);
      assertThat(result.yindex).isEqualTo(126);

      GridReferencedArray gra2 = grid.getReader().setLatLonPoint(LatLonPoint.create(60.088, -8.5)).read();
      float val2 = gra2.data().getScalar().floatValue();
      assertThat(val2).isWithin(1.e-5f).of(.001536f);

      GridReferencedArray gra =
          grid.getReader().setProjectionPoint(ProjectionPoint.create(result.xcoord, result.ycoord)).read();
      float val = gra.data().getScalar().floatValue();
      assertThat(val).isWithin(1.e-5f).of(.001536f);
    }
  }

  @Test
  public void testEcmwf() throws IOException, InvalidRangeException {
    String filename =
        TestDir.cdmUnitTestDir + "formats/grib2/MSG1-SEVI-MSGCLMK-0100-0100-20060102111500.000000000Z-12774.grb.grb";
    System.out.printf("testEcmwf openGrid %s%n", filename);

    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid("Cloud_mask").orElseThrow();

      GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
      GridHorizCoordinateSystem.CoordReturn result = hcs.findXYindexFromCoord(0, 0).orElseThrow();
      System.out.printf("  CoordReturn = %s%n", result);

      GridReferencedArray gra =
          grid.getReader().setProjectionPoint(ProjectionPoint.create(result.xcoord, result.ycoord)).read();
      float val = gra.data().getScalar().floatValue();
      assertThat(val).isWithin(1.e-5f).of(0f);
    }
  }
}
