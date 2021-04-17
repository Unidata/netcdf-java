/*
 *
 * * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * * See LICENSE for license information.
 *
 */

package ucar.nc2.write;

import com.google.common.base.Stopwatch;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Section;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.iosp.NetcdfFileFormat;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.NetcdfFile.IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT;

/** Test write a Netcdf-3 file larger than 2 Gb. Takes a few minutes, so leave out of cdm unit tests. */
public class TestNetcdfFormatWriterBig {
  private static Random random = new Random();

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  String varName = "example";
  int timeSize = 8;
  int latSize = 8022;
  int lonSize = 10627;
  String timeUnits = "hours since 2008-06-06 12:00:0.0";
  String coordUnits = "degrees";

  /** test writing > 2G with unlimited dimension */
  @Test
  public void testBigUnlimited() throws IOException, InvalidRangeException, ucar.array.InvalidRangeException {
    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    System.out.println("File size  (B)  = " + approxSize);
    System.out.println("File size~ (MB) = " + Math.round(approxSize / Math.pow(2, 20)));

    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false).setPreallocateSize(approxSize);
    writerb.addUnlimitedDimension("time");

    writerb.addVariable("time", ArrayType.FLOAT, "time").addAttribute(new Attribute("units", timeUnits));

    setDimension(writerb, "lat", coordUnits, latSize);
    setDimension(writerb, "lon", coordUnits, lonSize);

    writerb.addVariable(varName, ArrayType.FLOAT, "time lat lon").addAttribute(new Attribute("_FillValue", -9999))
        .addAttribute(new Attribute(CDM.MISSING_VALUE, -9999));

    // added after the big unlimited one, but not at the end, so wont fail
    writerb.addVariable("time2", ArrayType.FLOAT, "time");

    Stopwatch stopwatch = Stopwatch.createStarted();
    testWrite(writerb, fileName, NetcdfFileFormat.NETCDF3);
    System.out.printf("testBigUnlimited took %s secs%n", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  /** test writing > 2G not unlimited dimension, setLargeFile */
  @Test
  public void testBigFormat() throws IOException, InvalidRangeException, ucar.array.InvalidRangeException {
    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    System.out.println("File size  (B)  = " + approxSize);
    System.out.println("File size~ (MB) = " + Math.round(approxSize / Math.pow(2, 20)));

    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false).setPreallocateSize(approxSize);
    writerb.setFormat(NetcdfFileFormat.NETCDF3_64BIT_OFFSET);

    setDimension(writerb, "time", timeUnits, timeSize);
    setDimension(writerb, "lat", coordUnits, latSize);
    setDimension(writerb, "lon", coordUnits, lonSize);

    writerb.addVariable(varName, ArrayType.FLOAT, "time lat lon").addAttribute(new Attribute("_FillValue", -9999))
        .addAttribute(new Attribute(CDM.MISSING_VALUE, -9999));

    // heres a variable that comes after the too big one.
    writerb.addVariable("time2", ArrayType.FLOAT, "time");

    Stopwatch stopwatch = Stopwatch.createStarted();
    testWrite(writerb, fileName, NetcdfFileFormat.NETCDF3_64BIT_OFFSET);
    System.out.printf("testBigFormat took %s secs%n", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  /** test writing > 2G not unlimited dimension, not setLargeFile */
  @Test
  public void testBigFormatFails() throws IOException {
    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    System.out.println("File size  (B)  = " + approxSize);
    System.out.println("File size~ (MB) = " + Math.round(approxSize / Math.pow(2, 20)));

    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false).setPreallocateSize(approxSize);
    setDimension(writerb, "time", timeUnits, timeSize);
    setDimension(writerb, "lat", coordUnits, latSize);
    setDimension(writerb, "lon", coordUnits, lonSize);

    writerb.addVariable(varName, ArrayType.FLOAT, "time lat lon").addAttribute(new Attribute("_FillValue", -9999))
        .addAttribute(new Attribute(CDM.MISSING_VALUE, -9999));

    // heres a variable that comes after the too big one.
    writerb.addVariable("time2", ArrayType.FLOAT, "time");

    Stopwatch stopwatch = Stopwatch.createStarted();
    try (NetcdfFormatWriter writer = writerb.build()) {
      fail();
    } catch (Throwable t) {
      assertThat(t.getMessage()).contains("Variable starting pos=2728068468 may not exceed 2147483647");
    }
    System.out.printf("testBigFormat took %s secs%n", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  /**
   * Test writing > 2G not unlimited dimension, not setLargeFile, but too large variable at end of file.
   * This is ok, because its just the int32 header offset that has a problem. Who would ever need more than 640K?
   */
  @Test
  public void testBigFormatAtEnd() throws IOException, InvalidRangeException, ucar.array.InvalidRangeException {
    long approxSize = (long) timeSize * latSize * lonSize * 4 + 4000;
    System.out.println("File size  (B)  = " + approxSize);
    System.out.println("File size~ (MB) = " + Math.round(approxSize / Math.pow(2, 20)));

    String fileName = "/home/snake/tmp/testBigFormatFails.nc"; // tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false).setPreallocateSize(approxSize);
    setDimension(writerb, "time", timeUnits, timeSize);
    setDimension(writerb, "lat", coordUnits, latSize);
    setDimension(writerb, "lon", coordUnits, lonSize);

    writerb.addVariable(varName, ArrayType.FLOAT, "time lat lon").addAttribute(new Attribute("_FillValue", -9999))
        .addAttribute(new Attribute(CDM.MISSING_VALUE, -9999));

    Stopwatch stopwatch = Stopwatch.createStarted();
    testWrite(writerb, fileName, NetcdfFileFormat.NETCDF3);
    System.out.printf("testBigFormatAtEnd took %s secs%n", stopwatch.elapsed(TimeUnit.SECONDS));
  }

  private static Dimension setDimension(NetcdfFormatWriter.Builder<?> ncFile, String name, String units, int length) {
    Dimension dimension = ncFile.addDimension(name, length);
    ncFile.addVariable(name, ArrayType.FLOAT, name).addAttribute(new Attribute("units", units));
    return dimension;
  }

  private void testWrite(NetcdfFormatWriter.Builder<?> writerb, String fileName, NetcdfFileFormat format)
      throws IOException, ucar.array.InvalidRangeException, InvalidRangeException {
    float lastVal = random.nextFloat();
    try (NetcdfFormatWriter writer = writerb.build()) {
      int[] shape = new int[] {1, 1, lonSize};
      float[] floatStorage = new float[lonSize];
      Array floatArray = Array.factory(ucar.ma2.DataType.FLOAT, shape, floatStorage);
      for (int t = 0; t < timeSize; t++) {
        for (int i = 0; i < latSize; i++) {
          int[] origin = new int[] {t, i, 0};
          writer.write(varName, origin, floatArray);
        }
      }
      // write the last value
      float[] prim = new float[] {lastVal};
      Variable v = writer.findVariable(varName);
      writer.write(v, new int[] {timeSize - 1, latSize - 1, lonSize - 1},
          Arrays.factory(ArrayType.FLOAT, new int[] {1, 1, 1}, prim));
    }

    try (NetcdfFile ncfile = NetcdfFiles.open(fileName)) {
      assertThat(ncfile.sendIospMessage(IOSP_MESSAGE_GET_NETCDF_FILE_FORMAT)).isEqualTo(format);
      Variable v = ncfile.findVariable(varName);
      assertThat(v).isNotNull();
      // read the last value
      ucar.array.Array<?> data =
          v.readArray(new Section(new int[] {timeSize - 1, latSize - 1, lonSize - 1}, new int[] {1, 1, 1}));
      assertThat(data.length()).isEqualTo(1);
      assertThat(data.getRank()).isEqualTo(3);
      assertThat(data.get(0, 0, 0)).isEqualTo(lastVal);
    }
  }

}
