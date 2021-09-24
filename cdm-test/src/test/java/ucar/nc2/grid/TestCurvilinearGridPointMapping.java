/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test index to coordinate space mapping for curvilinear grids, e.g., lat(i,j), lon(i,j).
 */
@Category(NeedsCdmUnitTest.class)
public class TestCurvilinearGridPointMapping {

  @Test
  public void testUtmArtabro() throws IOException, InvalidRangeException {
    final String datasetLocation = TestDir.cdmUnitTestDir + "transforms/UTM/artabro_20120425.nc";
    final String covName = "hs";
    final int lonIndex = 170;
    final int latIndex = 62;

    NetcdfFile ncf = NetcdfFiles.open(datasetLocation);
    Variable latVar = ncf.findVariable("lat");
    assertThat(latVar).isNotNull();
    Section section = Section.builder().appendRange(latIndex, latIndex).appendRange(lonIndex, lonIndex).build();
    Array<Number> latArray = (Array<Number>) latVar.readArray(section);
    Variable lonVar = ncf.findVariable("lon");
    assertThat(lonVar).isNotNull();
    Array<Number> lonArray = (Array<Number>) lonVar.readArray(section);

    float latVal = latArray.getScalar().floatValue();
    float lonVal = lonArray.getScalar().floatValue();
    System.out.printf("latVal = %f lonVal = %f%n", latVal, lonVal);

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(datasetLocation, errlog)) {
      assertThat(gds).isNotNull();
      assertThat(gds.getGrids()).hasSize(10);
      Grid grid = gds.findGrid(covName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      float TOL = 1.0e-4f; // just the average of the edges, not exact.
      LatLonPoint llPnt = hcs.getLatLon(lonIndex, latIndex);
      assertThat(latVal).isWithin(TOL).of((float) llPnt.getLatitude());
      assertThat(lonVal).isWithin(TOL).of((float) llPnt.getLongitude());
    }
  }

  @Test
  public void testApexFmrc() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/apex_fmrc/Run_20091025_0000.nc";
    final String gridName = "temp";
    double lonVal = -73.6650;
    double latVal = 40.33211;

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      float TOL = 1.0e-4f; // just the average of the edges, not exact.
      LatLonPoint llPnt = hcs.getLatLon(5, 5);
      assertThat(llPnt.getLatitude()).isWithin(TOL).of(latVal);
      assertThat(llPnt.getLongitude()).isWithin(TOL).of(lonVal);
    }
  }

  @Test
  public void testRtofsGrib() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmUnitTestDir + "ft/fmrc/rtofs/ofs.20091122/ofs_atl.t00z.F024.grb.grib2";
    final String gridName = "Sea_Surface_Height_Relative_to_Geoid_surface";

    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      assertThat(gds).isNotNull();
      Grid grid = gds.findGrid(gridName).orElseThrow();
      assertThat(grid).isNotNull();

      GridCoordinateSystem cs = grid.getCoordinateSystem();
      assertThat(cs).isNotNull();
      assertThat(cs.getFeatureType()).isEqualTo(FeatureType.CURVILINEAR);
      GridHorizCoordinateSystem hcs = cs.getHorizCoordinateSystem();
      assertThat(hcs).isNotNull();
      assertThat(hcs.isCurvilinear()).isTrue();

      // find the x,y point for a specific lat/lon position
      double lat = -1.7272;
      double lon = -18.505;
      ProjectionPoint pp = hcs.getProjection().latLonToProj(lat, lon);

      GridHorizCoordinateSystem.CoordReturn cr = hcs.findXYindexFromCoord(pp.getX(), pp.getY()).orElseThrow();
      System.out.printf("Value at %s%n", cr);
      assertThat(cr.xindex).isEqualTo(165);
      assertThat(cr.yindex).isEqualTo(482);

      GridReferencedArray gra = grid.getReader().setProjectionPoint(pp).read();
      float val = gra.data().getScalar().floatValue();
      assertThat(val).isWithin(1.e-5f).of(.329f);
    }
  }

}
