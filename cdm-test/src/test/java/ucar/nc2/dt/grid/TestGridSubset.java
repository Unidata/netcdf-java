/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Range;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.write.Ncdump;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;

public class TestGridSubset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRegular() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/nuwg/03061219_ruc.nc")) {

      GeoGrid grid = dataset.findGridByName("T");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      CoordinateAxis zaxis = gcs.getVerticalAxis();
      assert zaxis.getUnitsString().equals("hectopascals");

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      GridCoordSystem gcs_section = grid_section.getCoordinateSystem();
      CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
      assert zaxis2.getSize() == 7;
      assert zaxis2.getUnitsString().equals("hectopascals");
      assert gcs_section.getTimeAxis().equals(gcs.getTimeAxis());

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 2 : data.getShape()[0];
      assert data.getShape()[1] == 7 : data.getShape()[1];
      assert data.getShape()[2] == 22 : data.getShape()[2];
      assert data.getShape()[3] == 31 : data.getShape()[3];

      // check axes
      for (CoordinateAxis axis : gcs_section.getCoordinateAxes()) {
        assert axis.getAxisType() != null;
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGrib() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/grib1/AVN.wmo")) {

      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_7-0-2-11_L100"); // "Temperature_isobaric");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 3 : data.getShape()[0];
      assert data.getShape()[1] == 3 : data.getShape()[1];
      assert data.getShape()[2] == 13 : data.getShape()[2];
      assert data.getShape()[3] == 15 : data.getShape()[3];
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testWRF() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/wrf/wrfout_v2_Lambert.nc")) {

      GeoGrid grid = dataset.findGridByName("T");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      CoordinateAxis zaxis = gcs.getVerticalAxis();
      assert zaxis.getSize() == 27;

      VerticalTransform vt = gcs.getVerticalTransform();
      assert vt != null;
      assert vt.getUnitString().equals("Pa");

      GeoGrid grid_section = grid.subset(null, null, null, 3, 3, 3);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 13 : data.getShape()[0];
      assert data.getShape()[1] == 9 : data.getShape()[1];
      assert data.getShape()[2] == 20 : data.getShape()[2];
      assert data.getShape()[3] == 25 : data.getShape()[3];

      GridCoordSystem gcs_section = grid_section.getCoordinateSystem();
      CoordinateAxis zaxis2 = gcs_section.getVerticalAxis();
      assert zaxis2.getSize() == 9 : zaxis2.getSize();

      assert zaxis2.getUnitsString().equals(zaxis.getUnitsString());
      assert gcs_section.getTimeAxis().equals(gcs.getTimeAxis());

      VerticalTransform vt_section = gcs_section.getVerticalTransform();
      assert vt_section != null;
      assert vt_section.getUnitString().equals(vt.getUnitString());
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testMSG() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    try (GridDataset dataset = GridDataset.open(filename)) {
      logger.debug("open {}", filename);
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_3-0-8"); // "Pixel_scene_type");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;

      // bbox = ll: 16.79S 20.5W+ ur: 14.1N 20.09E
      LatLonRect bbox = new LatLonRect(-16.79, -20.5, 14.1, 20.9);

      Projection p = gcs.getProjection();
      ProjectionRect prect = p.latLonToProjBB(bbox); // must override default implementation
      logger.debug("{} -> {}", bbox, prect);

      ProjectionRect expected =
          new ProjectionRect(ProjectionPoint.create(-2129.5688, -1793.0041), 4297.8453, 3308.3885);
      assert prect.nearlyEquals(expected);

      LatLonRect bb2 = p.projToLatLonBB(prect);
      logger.debug("{} -> {}", prect, bb2);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getRank() == 3;
      int[] shape = data.getShape();
      assert shape[0] == 1 : shape[0] + " should be 1";
      assert shape[1] == 363 : shape[1] + " should be 363";
      assert shape[2] == 479 : shape[2] + " should be 479";
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void test2D() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "conventions/cf/mississippi.nc")) {

      GeoGrid grid = dataset.findGridByName("salt");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 4;

      GeoGrid grid_section = grid.subset(null, null, null, 5, 5, 5);

      Array data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 4 : data.getShape()[1];
      assert data.getShape()[2] == 13 : data.getShape()[2];
      assert data.getShape()[3] == 26 : data.getShape()[3];

      grid_section = grid.subset(null, new Range(0, 0), null, 0, 2, 2);
      data = grid_section.readDataSlice(-1, -1, -1, -1);
      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 1 : data.getShape()[1];
      assert data.getShape()[2] == 32 : data.getShape()[2];
      assert data.getShape()[3] == 64 : data.getShape()[3];

      logger.debug(Ncdump.printArray(data, "grid_section", null));

      LatLonPoint p0 = LatLonPoint.create(29.0, -90.0);
      LatLonRect bbox = new LatLonRect.Builder(p0, 1.0, 2.0).build();
      grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      data = grid_section.readDataSlice(-1, -1, -1, -1);

      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 20 : data.getShape()[1];
      assert data.getShape()[2] == 63 : data.getShape()[2];
      assert data.getShape()[3] == 53 : data.getShape()[3];

      gcs = grid_section.getCoordinateSystem();
      ProjectionRect rect = gcs.getBoundingBox();
      logger.debug(" rect = {}", rect);

      p0 = LatLonPoint.create(30.0, -90.0);
      bbox = new LatLonRect.Builder(p0, 1.0, 2.0).build();
      grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      data = grid_section.readDataSlice(-1, -1, -1, -1);

      assert data.getShape()[0] == 1 : data.getShape()[0];
      assert data.getShape()[1] == 20 : data.getShape()[1];
      assert data.getShape()[2] == 18 : data.getShape()[2];
      assert data.getShape()[3] == 17 : data.getShape()[3];

      gcs = grid_section.getCoordinateSystem();
      logger.debug(" rect = {}", gcs.getBoundingBox());
    }
  }


  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset() throws Exception {
    try (GridDataset dataset =
        GridDataset.open(TestDir.cdmUnitTestDir + "conventions/problem/SUPER-NATIONAL_latlon_IR_20070222_1600.nc")) {
      GeoGrid grid = dataset.findGridByName("micron11");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 2;

      logger.debug("original bbox = {}", gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      testLatLonSubset(grid, bbox, new int[] {141, 281});

      bbox = new LatLonRect.Builder(LatLonPoint.create(-40.0, -180.0), 120.0, 300.0).build();
      testLatLonSubset(grid, bbox, new int[] {800, 1300});
    }
  }

  private void testLatLonSubset(GeoGrid grid, LatLonRect bbox, int[] shape) throws Exception {
    logger.debug("grid bbox = {}", grid.getCoordinateSystem().getLatLonBoundingBox().toString2());
    logger.debug("constrain bbox = {}", bbox.toString2());

    GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);
    GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
    assert null != gcs2;
    assert grid_section.getRank() == 2;

    logger.debug("resulting bbox = {}", gcs2.getLatLonBoundingBox().toString2());

    Array data = grid_section.readDataSlice(0, 0, -1, -1);
    assert data != null;
    assert data.getRank() == 2;
    assert data.getShape()[0] == shape[0] : data.getShape()[0];
    assert data.getShape()[1] == shape[1] : data.getShape()[1];
  }

  // longitude subsetting (CoordAxis1D regular)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testLatLonSubset2() throws Exception {
    try (GridDataset dataset =
        GridDataset.open(TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2")) {
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-3-0_L1"); // "Pressure_Surface");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3 : grid.getRank();

      logger.debug("original bbox = {}", gcs.getBoundingBox());
      logger.debug("lat/lon bbox = {}", gcs.getLatLonBoundingBox().toString2());

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();
      logger.debug("constrain bbox = {}", bbox.toString2());

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == grid.getRank();

      logger.debug("resulting bbox = {}", gcs2.getLatLonBoundingBox().toString2());

      Array data = grid_section.readDataSlice(0, 0, -1, -1);
      assert data != null;
      assert data.getRank() == 2;

      int[] dataShape = data.getShape();
      assert dataShape.length == 2;
      assert dataShape[0] == 11 : data.getShape()[0];
      assert dataShape[1] == 21 : data.getShape()[1];
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testVerticalAxis() throws Exception {
    String uri = TestDir.cdmUnitTestDir + "ncml/nc/cg/CG2006158_120000h_usfc.nc";
    String varName = "CGusfc";

    try (GridDataset dataset = GridDataset.open(uri)) {
      GeoGrid grid = dataset.findGridByName(varName);
      assert null != grid;

      GridCoordSystem gcsi = grid.getCoordinateSystem();
      assert null != gcsi;
      assert (gcsi.getVerticalAxis() != null);

      GridCoordSys gcs = (GridCoordSys) grid.getCoordinateSystem();
      assert null != gcs;
      assert gcs.hasVerticalAxis(); // returns true.

      // subset geogrid
      GeoGrid subg = grid.subset(null, null, null, 1, 1, 1);
      assert null != subg;

      GridCoordSystem gcsi2 = subg.getCoordinateSystem();
      assert null != gcsi2;
      assert (gcsi2.getVerticalAxis() != null);

      GridCoordSys gcs2 = (GridCoordSys) subg.getCoordinateSystem();
      assert null != gcs2;
      assert !gcs2.hasVerticalAxis(); // fails
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubsetVP() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "transforms/Eumetsat.VerticalPerspective.grb";
    try (GridDataset dataset = GridDataset.open(filename)) {
      GeoGrid grid = dataset.findGridDatatypeByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_3-0-8"); // "Pixel_scene_type");
      assert null != grid : dataset.getLocation();
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      System.out.printf("original bbox = %s%n", gcs.getBoundingBox());
      System.out.printf("lat/lon bbox = %s%n", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect.Builder(LatLonPoint.create(0, 0), 20.0, 40.0).build();
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      System.out.printf("result lat/lon bbox = %s%n", gcs2.getLatLonBoundingBox());
      System.out.printf("result bbox = %s%n", gcs2.getBoundingBox());

      LatLonRect expectLBB = new LatLonRect.Builder("-0.043318, -0.043487, 21.202380, 44.559265").build();
      assert (expectLBB.nearlyEquals(gcs2.getLatLonBoundingBox()));

      ProjectionRect expectBB = new ProjectionRect("-4.502221, -4.570379, 3925.936303, 2148.077947");
      assert (expectBB.nearlyEquals(gcs2.getBoundingBox()));
    }
  }

  // x,y in meters
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testBBSubsetUnits() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "ncml/testBBSubsetUnits.ncml")) {
      logger.debug("file = {}", dataset.getLocation());

      GeoGrid grid = dataset.findGridByName("pr");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      System.out.printf("original bbox = %s%n", gcs.getBoundingBox());
      System.out.printf("lat/lon bbox = %s%n", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect(38, -110, 42, -90);
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      System.out.printf("result lat/lon bbox = %s%n", gcs2.getLatLonBoundingBox());
      System.out.printf("result bbox = %s%n", gcs2.getBoundingBox());

      LatLonRect expectLBB = new LatLonRect.Builder("46.992792, -103.156421, 0.540102, 14.635361").build();
      assert (expectLBB.nearlyEquals(gcs2.getLatLonBoundingBox()));

      ProjectionRect expectBB = new ProjectionRect("-25.000000, -25.000000, 1250.000000, 50.000000");
      assert (expectBB.nearlyEquals(gcs2.getBoundingBox()));

      CoordinateAxis xaxis = gcs.getXHorizAxis();
      CoordinateAxis yaxis = gcs.getYHorizAxis();
      logger.debug("(nx,ny)= {}, {}", xaxis.getSize(), yaxis.getSize());
    }
  }

  @Test
  @Ignore("Does this file exist in a shared location?")
  public void testBBSubsetVP2() throws Exception {
    String filename =
        "C:/Documents and Settings/caron/My Documents/downloads/MSG2-SEVI-MSGCLAI-0000-0000-20070522114500.000000000Z-582760.grb";
    try (GridDataset dataset = GridDataset.open(filename)) {
      GeoGrid grid = dataset.findGridByName("Pixel_scene_type");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;

      System.out.printf("original bbox = {}", gcs.getBoundingBox());
      System.out.printf("lat/lon bbox = {}", gcs.getLatLonBoundingBox());

      ucar.unidata.geoloc.LatLonRect llbb_subset = new LatLonRect.Builder(LatLonPoint.create(0, 0), 20.0, 40.0).build();
      logger.debug("subset lat/lon bbox = {}", llbb_subset);

      GeoGrid grid_section = grid.subset(null, null, llbb_subset, 1, 1, 1);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;

      System.out.printf("result lat/lon bbox = %s%n", gcs2.getLatLonBoundingBox().toString());
      System.out.printf("result bbox = %s%n", gcs2.getBoundingBox().toString());

      LatLonRect expectLBB = new LatLonRect.Builder("-0.043318, -0.043487, 21.202380, 44.559265").build();
      assert (expectLBB.nearlyEquals(gcs2.getLatLonBoundingBox()));

      ProjectionRect expectBB = new ProjectionRect("-4.502221, -4.570379, 2148.077947, 3925.936303");
      assert (expectBB.nearlyEquals(gcs2.getBoundingBox()));
    }
  }

  // this one has the coordinate bounds set in the file
  @Test
  public void testSubsetCoordEdges() throws Exception {
    try (NetcdfDataset fooDataset =
        NetcdfDatasets.openDataset(TestDir.cdmLocalFromTestDataDir + "ncml/subsetCoordEdges.ncml")) {
      System.out.printf("testSubsetCoordEdges %s%n", fooDataset.getLocation());
      CompareNetcdf2 compare = new CompareNetcdf2();
      boolean ok = true;

      GridDataset fooGridDataset = new GridDataset(fooDataset);

      GridDatatype fooGrid = fooGridDataset.findGridDatatype("foo");
      assert fooGrid != null;

      CoordinateAxis1D fooTimeAxis = fooGrid.getCoordinateSystem().getTimeAxis1D();
      CoordinateAxis1D fooLatAxis = (CoordinateAxis1D) fooGrid.getCoordinateSystem().getYHorizAxis();
      CoordinateAxis1D fooLonAxis = (CoordinateAxis1D) fooGrid.getCoordinateSystem().getXHorizAxis();

      System.out.printf("mid time = %s%n", Arrays.toString(fooTimeAxis.getCoordValues()));
      System.out.printf("edge time = %s%n", Arrays.toString(fooTimeAxis.getCoordEdges()));

      ok &= compare.compareData("time getCoordValues", fooTimeAxis.getCoordValues(),
          new double[] {15.5, 45.0, 74.5, 105.0});
      ok &= compare.compareData("time getCoordEdges", fooTimeAxis.getCoordEdges(),
          new double[] {0.0, 31.0, 59.0, 90.0, 120.0});

      System.out.printf("mid lat = %s%n", Arrays.toString(fooLatAxis.getCoordValues()));
      System.out.printf("edge lat = %s%n", Arrays.toString(fooLatAxis.getCoordEdges()));

      ok &=
          compare.compareData("lat getCoordValues", fooLatAxis.getCoordValues(), new double[] {-54.0, 9.0, 54.0, 81.0});
      ok &= compare.compareData("lat getCoordEdges", fooLatAxis.getCoordEdges(),
          new double[] {-90.0, -18.0, 36.0, 72.0, 90.0});

      System.out.printf("mid lon= %s%n", Arrays.toString(fooLonAxis.getCoordValues()));
      System.out.printf("edge lon= %s%n", Arrays.toString(fooLonAxis.getCoordEdges()));

      ok &= compare.compareData("lon getCoordValues", fooLonAxis.getCoordValues(),
          new double[] {18.0, 72.0, 162.0, 288.0});
      ok &= compare.compareData("lon getCoordEdges", fooLonAxis.getCoordEdges(),
          new double[] {0.0, 36.0, 108.0, 216.0, 360.0});

      // take mid range for all of the 3 coordinates
      Range middleRange = new Range(1, 2);
      GridDatatype fooSubGrid = fooGrid.makeSubset(null, null, middleRange, null, middleRange, middleRange);

      CoordinateAxis1D fooSubTimeAxis = fooSubGrid.getCoordinateSystem().getTimeAxis1D();
      CoordinateAxis1D fooSubLatAxis = (CoordinateAxis1D) fooSubGrid.getCoordinateSystem().getYHorizAxis();
      CoordinateAxis1D fooSubLonAxis = (CoordinateAxis1D) fooSubGrid.getCoordinateSystem().getXHorizAxis();

      System.out.printf("subset mid time = %s%n", Arrays.toString(fooSubTimeAxis.getCoordValues()));
      System.out.printf("subset edge time = %s%n", Arrays.toString(fooSubTimeAxis.getCoordEdges()));
      ok &=
          compare.compareData("subset time getCoordValues", fooSubTimeAxis.getCoordValues(), new double[] {45.0, 74.5});
      ok &= compare.compareData("subset time getCoordEdges", fooSubTimeAxis.getCoordEdges(),
          new double[] {31.0, 59.0, 90.0});

      System.out.printf("subset mid lat = %s%n", Arrays.toString(fooSubLatAxis.getCoordValues()));
      System.out.printf("subset edge lat = %s%n", Arrays.toString(fooSubLatAxis.getCoordEdges()));
      ok &= compare.compareData("subset lat getCoordValues", fooSubLatAxis.getCoordValues(), new double[] {9.0, 54.0});
      ok &= compare.compareData("subset lat getCoordEdges", fooSubLatAxis.getCoordEdges(),
          new double[] {-18.0, 36.0, 72.0});

      System.out.printf("subset mid lon = %s%n", Arrays.toString(fooSubLonAxis.getCoordValues()));
      System.out.printf("subset edge lon = %s%n", Arrays.toString(fooSubLonAxis.getCoordEdges()));
      ok &=
          compare.compareData("subset lon getCoordValues", fooSubLonAxis.getCoordValues(), new double[] {72.0, 162.0,});
      ok &= compare.compareData("subset lon getCoordEdges", fooSubLonAxis.getCoordEdges(),
          new double[] {36.0, 108.0, 216.0});

      assert ok : "not ok";
    }
  }

  // has runtime(time), time(time)
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testTPgribCollection() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "gribCollections/tp/GFSonedega.ncx4")) {
      GeoGrid grid = dataset.findGridByName("Pressure_surface");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      logger.debug("{}", gcs);
      CoordinateAxis runtime = gcs.getRunTimeAxis();
      CoordinateAxis time = gcs.getTimeAxis();
      assert runtime != null;
      assert time != null;
      assert runtime.getSize() == time.getSize();
      assert runtime.getSize() == 2;
      assert runtime.getDimension(0) == time.getDimension(0);

      GeoGrid grid2 = grid.subset(new Range(1, 1), null, null, 1, 1, 1);
      GridCoordSystem gcs2 = grid2.getCoordinateSystem();
      assert null != gcs2;
      logger.debug("{}", gcs2);
      runtime = gcs2.getRunTimeAxis();
      time = gcs2.getTimeAxis();
      assert runtime != null;
      assert time != null;
      assert runtime.getSize() == time.getSize();
      assert runtime.getSize() == 1;
      Assert.assertEquals(runtime.getDimension(0), time.getDimension(0));

      // read a random point
      Array data = grid.readDataSlice(1, 0, 10, 20);
      Array data2 = grid2.readDataSlice(0, 0, 10, 20);

      logger.debug(Ncdump.printArray(data, "org", null));
      logger.debug(Ncdump.printArray(data2, "subset", null));

      Assert.assertTrue(CompareNetcdf2.compareData("slice", data, data2));

    }
  }
}
