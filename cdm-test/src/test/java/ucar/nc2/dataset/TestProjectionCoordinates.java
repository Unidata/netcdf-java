/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridHorizCoordinateSystem;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test specific projections
 */
@Category(NeedsCdmUnitTest.class)
public class TestProjectionCoordinates {

  private static final String testDir = TestDir.cdmUnitTestDir + "transforms/";

  @Test
  public void testPSscaleFactor() throws IOException {
    testCoordinates(testDir + "stereographic/foster.grib2", 26.023346, 251.023136 - 360.0, 41.527360,
        270.784605 - 360.0);
  }

  @Test
  public void testMercatorScaleFactor() throws IOException {
    // this one is using meters for the :semi_major_axis = 6378137.0;
    // and m for the coordinate values
    testCoordinates(testDir + "melb-small_M-1SP.nc", -36.940012, 145.845218, -36.918406, 145.909746);
  }

  @Test
  public void testRotatedPole() throws IOException {
    testCoordinates(testDir + "rotatedPole/snow.DMI.ecctrl.v5.ncml", 28.690059, -3.831161, 68.988028, 57.076276);
  }

  @Test
  public void testRotatedPole2() throws IOException {
    testCoordinates(testDir + "rotatedPole/DMI-HIRHAM5_ERAIN_DM_AMMA-50km_1989-1990_as.nc", -19.8, -35.64, 35.2, 35.2);
  }

  private void testCoordinates(String filename, double startLat, double startLon, double endLat, double endLon)
      throws IOException {
    System.out.printf("%n***Open %s%n", filename);
    Formatter errlog = new Formatter();
    try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      GridCoordinateSystem gridCoordinateSystem = gds.getGridCoordinateSystems().get(0);
      GridHorizCoordinateSystem hcs = gridCoordinateSystem.getHorizCoordinateSystem();
      Projection proj = hcs.getProjection();
      assertThat(proj).isNotNull();
      System.out.printf("proj=%s%n", proj);

      GridAxisPoint xaxis = hcs.getXHorizAxis();
      GridAxisPoint yaxis = hcs.getYHorizAxis();
      proj.projToLatLon(xaxis.getCoordDouble(0), yaxis.getCoordDouble(0));
      LatLonPoint start1 = proj.projToLatLon(xaxis.getCoordDouble(0), yaxis.getCoordDouble(0));
      LatLonPoint start2 = proj.projToLatLon(xaxis.getCoordDouble(xaxis.getNominalSize() - 1),
          yaxis.getCoordDouble(yaxis.getNominalSize() - 1));
      System.out.printf("start = %f %f%n", start1.getLatitude(), start1.getLongitude());
      System.out.printf("end = %f %f%n", start2.getLatitude(), start2.getLongitude());

      assertThat(start1.nearlyEquals(LatLonPoint.create(startLat, startLon), 2.0E-4)).isTrue();
      assertThat(start2.nearlyEquals(LatLonPoint.create(endLat, endLon), 2.0E-4)).isTrue();
    }
  }

}
