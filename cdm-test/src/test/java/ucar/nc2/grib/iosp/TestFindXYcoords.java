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

  /*
   * old grid wotks with slurp://www.unidata.ucar.edu/software/netcdf/examples/sresa1b_ncar_ccsm3_0_run1_200001.nc
   * // but not new grid
   * // franck.reinquin@free.fr
   * // 3/11/2015
   * 
   * @Test
   * public void bugReport() throws IOException {
   * 
   * String filename = "slurp://www.unidata.ucar.edu/software/netcdf/examples/sresa1b_ncar_ccsm3_0_run1_200001.nc";
   * System.out.printf("%s%n", filename);
   * Formatter errlog = new Formatter();
   * try (ucar.nc2.grid.GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
   * assertThat(gds).isNotNull();
   * 
   * Grid grid = gds.findGrid("Convective_inhibition_surface").orElseThrow();
   * GridHorizCoordinateSystem hcs = grid.getHorizCoordinateSystem();
   * 
   * 
   * try (GridDataset dataset =
   * GridDataset.open("http://www.unidata.ucar.edu/software/netcdf/examples/sresa1b_ncar_ccsm3_0_run1_200001.nc")) {
   * 
   * GridDatatype firstGridInfo = dataset.getGrids().get(0);
   * System.out.println("Grid name =" + firstGridInfo.getName());
   * GeoGrid firstGrid = (GeoGrid) dataset.getGrids().get(0);
   * 
   * System.out.println("WHOLE GRID");
   * GridCoordSystem gcs = firstGrid.getCoordinateSystem();
   * System.out.println("is lat/lon system ? " + gcs.isLatLon());
   * assert gcs.isLatLon();
   * 
   * LatLonRect rect = gcs.getLatLonBoundingBox();
   * System.out.println("gcs bounding box : latmin=" + rect.getLatMin() + " latmax=" + rect.getLatMax() + " lonmin="
   * + rect.getLonMin() + " lonmax=" + rect.getLonMax());
   * System.out.println("projection       : " + gcs.getProjection());
   * System.out.println("width =" + gcs.getXHorizAxis().getSize() + ", height=" + gcs.getYHorizAxis().getSize());
   * System.out.println("X is regular     ? " + ((CoordinateAxis1D) gcs.getXHorizAxis()).isRegular());
   * System.out.println("X is contiguous  ? " + gcs.getXHorizAxis().isContiguous());
   * System.out.println("X start          : " + ((CoordinateAxis1D) gcs.getXHorizAxis()).getStart());
   * System.out.println("X increment      : " + ((CoordinateAxis1D) gcs.getXHorizAxis()).getIncrement());
   * System.out.println("Y is regular     ? " + ((CoordinateAxis1D) gcs.getYHorizAxis()).isRegular());
   * System.out.println("Y is contiguous  ? " + gcs.getYHorizAxis().isContiguous());
   * System.out.println("Y start          : " + ((CoordinateAxis1D) gcs.getYHorizAxis()).getStart());
   * System.out.println("Y increment      : " + ((CoordinateAxis1D) gcs.getYHorizAxis()).getIncrement());
   * 
   * LatLonPoint p = gcs.getLatLon(0, 0);
   * System.out.println("index (0,0) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
   * p = gcs.getLatLon(1, 1);
   * System.out.println("index (1,1) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
   * 
   * System.out.println("looking up lat=" + p.getLatitude() + "  lon=" + p.getLongitude());
   * int[] xy = gcs.findXYindexFromLatLon(p.getLatitude(), p.getLongitude(), null);
   * System.out.println("index= (" + xy[0] + ", " + xy[1] + ")");
   * Assert.assertEquals(xy[0], 1);
   * Assert.assertEquals(xy[1], 1);
   * 
   * // --------------------------------------------------------------------------
   * double latMin = -20.D, latMax = -10.D, lonMin = 35.D, lonMax = 45.D;
   * System.out.println(
   * "\nSUBGRID (latmin=" + latMin + "  latmax=" + latMax + "  lonmin=" + lonMin + "  lonmax=" + lonMax + ")");
   * 
   * LatLonRect latLonRect = new LatLonRect(latMin, lonMin, latMax, lonMax);
   * 
   * GeoGrid gridSubset = firstGrid.subset(null, null, latLonRect, 0, 1, 1);
   * 
   * GridCoordSystem gcs2 = gridSubset.getCoordinateSystem();
   * 
   * rect = gcs2.getLatLonBoundingBox();
   * System.out.println("is lat/lon system ? " + gcs2.isLatLon());
   * System.out.println("gcs bounding box : latmin=" + rect.getLatMin() + " latmax=" + rect.getLatMax() + " lonmin="
   * + rect.getLonMin() + " lonmax=" + rect.getLonMax());
   * System.out.println("projection       : " + gcs.getProjection());
   * System.out.println("width =" + gcs2.getXHorizAxis().getSize() + ", height=" + gcs2.getYHorizAxis().getSize());
   * System.out.println("X is regular     ? " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).isRegular());
   * System.out.println("X is contiguous  ? " + gcs2.getXHorizAxis().isContiguous());
   * System.out.println("X start          : " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).getStart());
   * System.out.println("X increment      : " + ((CoordinateAxis1D) gcs2.getXHorizAxis()).getIncrement());
   * System.out.println("Y is regular     ? " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).isRegular());
   * System.out.println("Y is contiguous  ? " + gcs2.getYHorizAxis().isContiguous());
   * System.out.println("Y start          : " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).getStart());
   * System.out.println("Y increment      : " + ((CoordinateAxis1D) gcs2.getYHorizAxis()).getIncrement());
   * 
   * p = gcs2.getLatLon(0, 0);
   * System.out.println("index (0,0) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
   * p = gcs2.getLatLon(1, 1);
   * System.out.println("index (1,1) --> lat/lon : " + p.getLatitude() + " ; " + p.getLongitude());
   * 
   * System.out.println("looking up lat=" + p.getLatitude() + "  lon=" + p.getLongitude());
   * xy = gcs2.findXYindexFromLatLon(p.getLatitude(), p.getLongitude(), null);
   * System.out.println("index= (" + xy[0] + ", " + xy[1] + ")");
   * Assert.assertEquals(xy[0], 1);
   * Assert.assertEquals(xy[1], 1);
   * 
   * } catch (IOException | InvalidRangeException e) {
   * e.printStackTrace();
   * }
   * }
   */
}
