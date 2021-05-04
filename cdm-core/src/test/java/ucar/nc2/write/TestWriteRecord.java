/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.test.Assert2;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Simple example to create a new netCDF file corresponding to the following CDL:
 *
 * <pre>
 *  netcdf example {
 *  dimensions:
 *    lat = 3 ;
 *    lon = 4 ;
 *    time = UNLIMITED ;
 *  variables:
 *    int rh(time, lat, lon) ;
 *              rh:long_name="relative humidity" ;
 *      rh:units = "percent" ;
 *    double T(time, lat, lon) ;
 *              T:long_name="surface temperature" ;
 *      T:units = "degC" ;
 *    float lat(lat) ;
 *      lat:units = "degrees_north" ;
 *    float lon(lon) ;
 *      lon:units = "degrees_east" ;
 *    int time(time) ;
 *      time:units = "hours" ;
 *  // global attributes:
 *      :title = "Example Data" ;
 *  data:
 *   rh =
 *     1, 2, 3, 4,
 *     5, 6, 7, 8,
 *     9, 10, 11, 12,
 *     21, 22, 23, 24,
 *     25, 26, 27, 28,
 *     29, 30, 31, 32 ;
 *   T =
 *     1, 2, 3, 4,
 *     2, 4, 6, 8,
 *     3, 6, 9, 12,
 *     2.5, 5, 7.5, 10,
 *     5, 10, 15, 20,
 *     7.5, 15, 22.5, 30 ;
 *   lat = 41, 40, 39 ;
 *   lon = -109, -107, -105, -103 ;
 *   time = 6, 18 ;
 *  }
 * </pre>
 *
 * @author : Russ Rew
 * @author : John Caron
 */
public class TestWriteRecord {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testNC3WriteWithRecordVariables() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);

    // define Variables

    // int rh(time, lat, lon) ;
    // rh:long_name="relative humidity" ;
    // rh:units = "percent" ;
    // test attribute array
    ArrayInt.D1 valid_range = new ArrayInt.D1(2, false);
    valid_range.set(0, 0);
    valid_range.set(1, 100);

    writerb.addVariable("rh", DataType.INT, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "relative humidity")).addAttribute(new Attribute("units", "percent"))
        .addAttribute(Attribute.fromArray("range", valid_range))
        .addAttribute(Attribute.fromArray(CDM.VALID_RANGE, Array.makeFromJavaArray(new double[] {0d, 100d}, false)));

    // double T(time, lat, lon) ;
    // T:long_name="surface temperature" ;
    // T:units = "degC" ;
    writerb.addVariable("T", DataType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    // float lat(lat) ;
    // lat:units = "degrees_north" ;
    writerb.addVariable("lat", DataType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    // float lon(lon) ;
    // lon:units = "degrees_east" ;
    writerb.addVariable("lon", DataType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    // int time(time) ;
    // time:units = "hours" ;
    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute("units", "hours"));

    writerb.addVariable("recordvarTest", DataType.INT, "time");

    // :title = "Example Data" ;
    writerb.addAttribute(new Attribute("title", "Example Data"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable v = writer.findVariable("rh");
      assert v != null;
      assert v.isUnlimited();

      // write the RH data one value at a time to an Array
      int[][][] rhData =
          {{{1, 2, 3, 4}, {5, 6, 7, 8}, {9, 10, 11, 12}}, {{21, 22, 23, 24}, {25, 26, 27, 28}, {29, 30, 31, 32}}};

      ArrayInt rhA = new ArrayInt.D3(2, latDim.getLength(), lonDim.getLength(), false);
      Index ima = rhA.getIndex();
      // write
      for (int i = 0; i < 2; i++) {
        for (int j = 0; j < latDim.getLength(); j++) {
          for (int k = 0; k < lonDim.getLength(); k++) {
            rhA.setInt(ima.set(i, j, k), rhData[i][j][k]);
          }
        }
      }

      // write rhData out to disk
      writer.write("rh", rhA);

      // Here's an Array approach to set the values of T all at once.
      double[][][] tData =
          {{{1., 2, 3, 4}, {2., 4, 6, 8}, {3., 6, 9, 12}}, {{2.5, 5, 7.5, 10}, {5., 10, 15, 20}, {7.5, 15, 22.5, 30}}};
      writer.write("T", Array.makeFromJavaArray(tData, false));

      // Store the rest of variable values
      writer.write("lat", Array.makeFromJavaArray(new float[] {41, 40, 39}, false));
      writer.write("lon", Array.makeFromJavaArray(new float[] {-109, -107, -105, -103}, false));
      writer.write("time", Array.makeFromJavaArray(new int[] {6, 18}, false));

      /*
       * write using scalar arrays
       * assert timeDim.getLength() == 2;
       * int[] origin = {0};
       * ArrayInt.D0 data = new ArrayInt.D0();
       * for (int time=0; time<timeDim.getLength(); time++) {
       * origin[0] = time;
       * data.set(time*10);
       * ncfile.write("recordvarTest", origin, data);
       * }
       */
    }

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      /* Get the value of the global attribute named "title" */
      Attribute title = ncfile.findGlobalAttribute("title");
      assert title != null;
      assert title.getStringValue().equals("Example Data") : title;

      /*
       * Read the latitudes into an array of double.
       * This works regardless of the external
       * type of the "lat" variable.
       */
      Variable lat = ncfile.findVariable("lat");
      Assert.assertNotNull(lat);
      assert (lat.getRank() == 1); // make sure it's 1-dimensional
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats]; // where to put them

      Array values = lat.read(); // read all into memory
      Index ima = values.getIndex(); // index array to specify which value
      for (int ilat = 0; ilat < nlats; ilat++) {
        lats[ilat] = values.getDouble(ima.set0(ilat));
      }
      /* Read units attribute of lat variable */
      Attribute latUnits = lat.findAttribute("units");
      assert latUnits != null;
      assert latUnits.getStringValue().equals("degrees_north");

      /* Read the longitudes. */
      Variable lon = ncfile.findVariable("lon");
      Assert.assertNotNull(lon);
      values = lon.read();
      assert (values instanceof ArrayFloat.D1);
      ArrayFloat.D1 fa = (ArrayFloat.D1) values;
      Assert2.assertNearlyEquals(fa.get(0), -109.0f);
      Assert2.assertNearlyEquals(fa.get(1), -107.0f);
      Assert2.assertNearlyEquals(fa.get(2), -105.0f);
      Assert2.assertNearlyEquals(fa.get(3), -103.0f);

      /*
       * Now we can just use the MultiArray to access values, or
       * we can copy the MultiArray elements to another array with
       * toArray(), or we can get access to the MultiArray storage
       * without copying. Each of these approaches to accessing
       * the data are illustrated below.
       */

      /* Whats the time dimension length ? */
      Dimension td = ncfile.findDimension("time");
      Assert.assertNotNull(td);
      assert td.getLength() == 2;

      /* Read the times: unlimited dimension */
      Variable time = ncfile.findVariable("time");
      Assert.assertNotNull(time);
      Array timeValues = time.read();
      assert (timeValues instanceof ArrayInt.D1);
      ArrayInt.D1 ta = (ArrayInt.D1) timeValues;
      assert (ta.get(0) == 6) : ta.get(0);
      assert (ta.get(1) == 18) : ta.get(1);

      /* Read the relative humidity data */
      Variable rh = ncfile.findVariable("rh");
      Assert.assertNotNull(rh);
      Array rhValues = rh.read();
      assert (rhValues instanceof ArrayInt.D3);
      ArrayInt.D3 rha = (ArrayInt.D3) rhValues;
      int[] shape = rha.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            int want = 20 * i + 4 * j + k + 1;
            int val = rha.get(i, j, k);
            assert (want == val) : val;
          }
        }
      }

      /* Read the temperature data */
      Variable t = ncfile.findVariable("T");
      Assert.assertNotNull(t);
      Array tValues = t.read();
      assert (tValues instanceof ArrayDouble.D3);
      ArrayDouble.D3 Ta = (ArrayDouble.D3) tValues;
      Assert2.assertNearlyEquals(Ta.get(0, 0, 0), 1.0f);
      Assert2.assertNearlyEquals(Ta.get(1, 1, 1), 10.0f);

      /* Read subset of the temperature data */
      tValues = t.read(new int[3], new int[] {2, 2, 2});
      assert (tValues instanceof ArrayDouble.D3);
      Ta = (ArrayDouble.D3) tValues;
      Assert2.assertNearlyEquals(Ta.get(0, 0, 0), 1.0f);
      Assert2.assertNearlyEquals(Ta.get(1, 1, 1), 10.0f);
    }
  }

  // make an example writing records
  @Test
  public void testNC3WriteWithRecord() throws IOException, InvalidRangeException {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    Dimension latDim = writerb.addDimension("lat", 64);
    Dimension lonDim = writerb.addDimension("lon", 128);

    // define Variables

    // double T(time, lat, lon) ;
    // T:long_name="surface temperature" ;
    // T:units = "degC" ;
    writerb.addVariable("T", DataType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    // float lat(lat) ;
    // lat:units = "degrees_north" ;
    writerb.addVariable("lat", DataType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    // float lon(lon) ;
    // lon:units = "degrees_east" ;
    writerb.addVariable("lon", DataType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    // int time(time) ;
    // time:units = "hours" ;
    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute("units", "hours"));

    // :title = "Example Data" ;
    writerb.addAttribute(new Attribute("title", "Example Data"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // now write one record at a time
      Variable v = writer.findVariable("T");
      Assert.assertNotNull(v);
      ArrayDouble data = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      ArrayInt timeData = new ArrayInt.D1(1, false);
      int[] origin = new int[v.getRank()];
      int[] timeOrigin = new int[1];

      for (int time = 0; time < 100; time++) {
        // fill the data array
        Index ima = data.getIndex();
        for (int j = 0; j < latDim.getLength(); j++) {
          for (int k = 0; k < lonDim.getLength(); k++) {
            data.setDouble(ima.set(0, j, k), (double) time * j * k);
          }
        }
        timeData.setInt(timeData.getIndex(), time);

        // write to file
        origin[0] = time;
        timeOrigin[0] = time;
        writer.write("T", origin, data);
        writer.write("time", timeOrigin, timeData);
      }
    }
  }
}
