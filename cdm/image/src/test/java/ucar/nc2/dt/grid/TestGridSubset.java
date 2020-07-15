/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.grid;

import java.lang.invoke.MethodHandles;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGridSubset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGiniSubsetStride() throws Exception {
    try (GridDataset dataset =
        GridDataset.open(TestDir.cdmUnitTestDir + "formats/gini/WEST-CONUS_4km_IR_20070216_1500.gini")) {
      GeoGrid grid = dataset.findGridByName("IR");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;
      int[] org_shape = grid.getShape();

      Array data_org = grid.readDataSlice(0, 0, -1, -1);
      assert data_org != null;
      assert data_org.getRank() == 2;
      int[] data_shape = data_org.getShape();
      assert org_shape[1] == data_shape[0];
      assert org_shape[2] == data_shape[1];

      logger.debug("original bbox = {}" + gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();

      LatLonProjection llproj = new LatLonProjection();
      ProjectionRect[] prect = llproj.latLonToProjRect(bbox);
      logger.debug("constrain bbox = {}", prect[0]);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 2, 3);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == 3;

      ProjectionRect subset_prect = gcs2.getBoundingBox();
      logger.debug("resulting bbox = {}", subset_prect);

      // test stride
      grid_section = grid.subset(null, null, null, 1, 2, 3);
      Array data = grid_section.readDataSlice(0, 0, -1, -1);
      assert data != null;
      assert data.getRank() == 2;

      int[] shape = data.getShape();
      assert Math.abs(org_shape[1] - 2 * shape[0]) < 2 : org_shape[2] + " != " + (2 * shape[0]);
      assert Math.abs(org_shape[2] - 3 * shape[1]) < 3 : org_shape[2] + " != " + (3 * shape[1]);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAggByteGiniSubsetStride() throws Exception {
    try (GridDataset dataset = GridDataset.open(TestDir.cdmUnitTestDir + "formats/gini/giniAggByte.ncml")) {
      logger.debug("Test {}", dataset.getLocation());
      GeoGrid grid = dataset.findGridByName("IR");
      assert null != grid;
      GridCoordSystem gcs = grid.getCoordinateSystem();
      assert null != gcs;
      assert grid.getRank() == 3;
      int[] org_shape = grid.getShape();
      assert grid.getDataType() == DataType.UINT;

      Array data_org = grid.readDataSlice(0, 0, -1, -1);
      assert data_org != null;
      assert data_org.getRank() == 2;
      int[] data_shape = data_org.getShape();
      assert org_shape[1] == data_shape[0];
      assert org_shape[2] == data_shape[1];
      assert data_org.getElementType() == int.class : data_org.getElementType();

      logger.debug("original bbox = {}", gcs.getBoundingBox());

      LatLonRect bbox = new LatLonRect.Builder(LatLonPoint.create(40.0, -100.0), 10.0, 20.0).build();

      LatLonProjection llproj = new LatLonProjection();
      ProjectionRect[] prect = llproj.latLonToProjRect(bbox);
      logger.debug("constrain bbox = {}", prect[0]);

      GeoGrid grid_section = grid.subset(null, null, bbox, 1, 2, 3);
      GridCoordSystem gcs2 = grid_section.getCoordinateSystem();
      assert null != gcs2;
      assert grid_section.getRank() == 3;
      assert grid_section.getDataType() == DataType.UINT;

      ProjectionRect subset_prect = gcs2.getBoundingBox();
      logger.debug("resulting bbox = {}", subset_prect);

      // test stride
      grid_section = grid.subset(null, null, null, 2, 2, 3);
      Array data = grid_section.readVolumeData(1);
      assert data != null;
      assert data.getRank() == 2;
      assert data.getElementType() == int.class;

      int[] shape = data.getShape();
      assert Math.abs(org_shape[1] - 2 * shape[0]) < 2 : org_shape[2] + " != " + (2 * shape[0]);
      assert Math.abs(org_shape[2] - 3 * shape[1]) < 3 : org_shape[2] + " != " + (3 * shape[1]);
    }
  }

}
