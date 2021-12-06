/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.Array;
import ucar.array.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;

import static com.google.common.truth.Truth.assertThat;

/**
 * Simple example using {@link NetcdfFormatWriter} to create a new netCDF file corresponding to the following CDL:
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
 */
public class TestWriteRecord {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testNC3WriteWithRecordVariables() throws Exception {
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
    Array<Integer> valid_range = Arrays.factory(ArrayType.INT, new int[] {2}, new int[] {0, 100});
    Array<Double> valid_ranged = Arrays.factory(ArrayType.DOUBLE, new int[] {2}, new double[] {0., 100.});

    writerb.addVariable("rh", ArrayType.INT, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "relative humidity")).addAttribute(new Attribute("units", "percent"))
        .addAttribute(Attribute.fromArray("range", valid_range))
        .addAttribute(Attribute.fromArray(CDM.VALID_RANGE, valid_ranged));

    // double T(time, lat, lon) ;
    // T:long_name="surface temperature" ;
    // T:units = "degC" ;
    writerb.addVariable("T", ArrayType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    // float lat(lat) ;
    // lat:units = "degrees_north" ;
    writerb.addVariable("lat", ArrayType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    // float lon(lon) ;
    // lon:units = "degrees_east" ;
    writerb.addVariable("lon", ArrayType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    // int time(time) ;
    // time:units = "hours" ;
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours"));

    writerb.addVariable("recordvarTest", ArrayType.INT, "time");

    // :title = "Example Data" ;
    writerb.addAttribute(new Attribute("title", "Example Data"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable rh = writer.findVariable("rh");
      assertThat(rh).isNotNull();
      assertThat(rh.isUnlimited()).isTrue();

      int[] rhData = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32};
      Array<Integer> rhArray = Arrays.factory(ArrayType.INT, new int[] {2, 3, 4}, rhData);
      writer.write(rh, rhArray.getIndex(), rhArray);

      // Here's an Array approach to set the values of T all at once.
      Variable t = writer.findVariable("T");
      assertThat(t).isNotNull();
      assertThat(t.isUnlimited()).isTrue();

      double[] tData = {1., 2, 3, 4, 2., 4, 6, 8, 3., 6, 9, 12, 2.5, 5, 7.5, 10, 5., 10, 15, 20, 7.5, 15, 22.5, 30};
      Array<Double> tArray = Arrays.factory(ArrayType.DOUBLE, new int[] {2, 3, 4}, tData);
      writer.write(t, tArray.getIndex(), tArray);

      // Store the rest of variable values
      writer.write(writer.findVariable("lat"), Index.ofRank(1),
          Arrays.factory(ArrayType.FLOAT, new int[] {3}, new float[] {41, 40, 39}));
      writer.write(writer.findVariable("lon"), Index.ofRank(1),
          Arrays.factory(ArrayType.FLOAT, new int[] {4}, new float[] {-109, -107, -105, -103}));
      writer.write(writer.findVariable("time"), Index.ofRank(1),
          Arrays.factory(ArrayType.INT, new int[] {2}, new int[] {6, 18}));
    }

    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Attribute title = ncfile.findAttribute("title");
      assertThat(title).isNotNull();
      assertThat(title.getStringValue()).isEqualTo("Example Data");

      /*
       * Read the latitudes into an array of double.
       * This works regardless of the external
       * type of the "lat" variable.
       */
      Variable lat = ncfile.findVariable("lat");
      assertThat(lat).isNotNull();
      assertThat(lat.getRank()).isEqualTo(1);
      int nlats = lat.getShape()[0]; // number of latitudes
      double[] lats = new double[nlats]; // where to put them

      Array<Number> values = (Array<Number>) lat.readArray(); // read all into memory
      int count = 0;
      for (Number val : values) {
        lats[count++] = val.doubleValue();
      }
      /* Read units attribute of lat variable */
      Attribute latUnits = lat.findAttribute("units");
      assertThat(latUnits).isNotNull();
      assertThat(latUnits.getStringValue()).isEqualTo("degrees_north");

      /* Read the longitudes. */
      Variable lon = ncfile.findVariable("lon");
      assertThat(lon).isNotNull();
      Array<Float> fa = (Array<Float>) lon.readArray();
      assertThat(values.getArrayType()).isEqualTo(ArrayType.FLOAT);
      assertThat(Misc.nearlyEquals(fa.get(0), -109.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(1), -107.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(2), -105.0f)).isTrue();
      assertThat(Misc.nearlyEquals(fa.get(3), -103.0f)).isTrue();

      /*
       * Now we can just use the MultiArray to access values, or
       * we can copy the MultiArray elements to another array with
       * toArray(), or we can get access to the MultiArray storage
       * without copying. Each of these approaches to accessing
       * the data are illustrated below.
       */

      /* Whats the time dimension length ? */
      Dimension td = ncfile.findDimension("time");
      assertThat(td).isNotNull();
      assertThat(td.getLength()).isEqualTo(2);

      /* Read the times: unlimited dimension */
      Variable time = ncfile.findVariable("time");
      assertThat(time).isNotNull();
      Array<?> timeValues = time.readArray();
      assertThat(timeValues.getArrayType()).isEqualTo(ArrayType.INT);
      Array<Integer> ta = (Array<Integer>) timeValues;
      assertThat(ta.get(0)).isEqualTo(6);
      assertThat(ta.get(1)).isEqualTo(18);

      /* Read the relative humidity data */
      Variable rh = ncfile.findVariable("rh");
      assertThat(rh).isNotNull();
      Array<?> rhValues = rh.readArray();
      assertThat(rhValues.getArrayType()).isEqualTo(ArrayType.INT);
      Array<Integer> rha = (Array<Integer>) rhValues;
      int[] shape = rha.getShape();
      for (int i = 0; i < shape[0]; i++) {
        for (int j = 0; j < shape[1]; j++) {
          for (int k = 0; k < shape[2]; k++) {
            int want = 20 * i + 4 * j + k + 1;
            int val = rha.get(i, j, k);
            assertThat(want).isEqualTo(val);
          }
        }
      }

      /* Read the temperature data */
      Variable t = ncfile.findVariable("T");
      assertThat(t).isNotNull();
      Array<Double> Ta = (Array<Double>) t.readArray();
      assertThat(Ta.get(0, 0, 0)).isEqualTo(1.0);
      assertThat(Ta.get(1, 1, 1)).isEqualTo(10.0);

      /* Read subset of the temperature data */
      Array<Double> tSubset = (Array<Double>) t.readArray(new Section(new int[3], new int[] {2, 2, 2}));
      assertThat(tSubset.get(0, 0, 0)).isEqualTo(1.0);
      assertThat(tSubset.get(1, 1, 1)).isEqualTo(10.0);
    }
  }

  // make an example writing records
  @Test
  public void testNC3WriteWithRecord() throws Exception {
    String filename = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename).setFill(false);
    writerb.addUnlimitedDimension("time");
    Dimension latDim = writerb.addDimension("lat", 64);
    Dimension lonDim = writerb.addDimension("lon", 128);

    // define Variables

    // double T(time, lat, lon) ;
    // T:long_name="surface temperature" ;
    // T:units = "degC" ;
    writerb.addVariable("T", ArrayType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    // float lat(lat) ;
    // lat:units = "degrees_north" ;
    writerb.addVariable("lat", ArrayType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    // float lon(lon) ;
    // lon:units = "degrees_east" ;
    writerb.addVariable("lon", ArrayType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    // int time(time) ;
    // time:units = "hours" ;
    writerb.addVariable("time", ArrayType.INT, "time").addAttribute(new Attribute("units", "hours"));

    // :title = "Example Data" ;
    writerb.addAttribute(new Attribute("title", "Example Data"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // now write one record at a time
      Variable tv = writer.findVariable("T");
      assertThat(tv).isNotNull();

      Index dataIndex = Index.ofRank(3);
      Index timeIndex = Index.ofRank(1);
      int[] dataShape = new int[] {1, latDim.getLength(), lonDim.getLength()};

      double[] pdata = new double[(int) Arrays.computeSize(dataShape)];
      for (int time = 0; time < 100; time++) {
        int count = 0;
        for (int j = 0; j < latDim.getLength(); j++) {
          for (int k = 0; k < lonDim.getLength(); k++) {
            pdata[count] = (double) (time * j * k);
          }
        }
        Array<Integer> data = Arrays.factory(ArrayType.DOUBLE, dataShape, pdata);
        Array<Integer> timeData = Arrays.factory(ArrayType.INT, new int[] {1}, new int[] {time});

        // write to file
        writer.write(tv, dataIndex.set0(time), data);
        writer.write(writer.findVariable("time"), timeIndex.set(time), timeData);
      }
    }
  }
}
