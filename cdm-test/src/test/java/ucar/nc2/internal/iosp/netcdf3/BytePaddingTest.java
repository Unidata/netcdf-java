/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.netcdf3;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import static org.junit.Assert.*;

/**
 * testing netcdf3 byte padding
 *
 * @author edavis
 * @since 4.1
 */
public class BytePaddingTest {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf3/";

  @Test
  public void checkReadOfFileWrittenWithIncorrectPaddingOfOneDimByteArrayOnlyRecordVar() throws IOException {
    // File testDataDir = new File( TestDir.cdmLocalTestDataDir, "ucar/nc2/iosp/netcdf3");
    File testFile = new File(TestDir.cdmLocalFromTestDataDir, "byteArrayRecordVarPaddingTest-bad.nc");
    assertTrue(testFile.exists());
    assertTrue(testFile.canRead());

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("V");
      assertEquals(readVar.getDataType(), DataType.BYTE);
      assertEquals(1, readVar.getElementSize());

      Array byteData = readVar.read();

      // File was created with the following data
      // byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
      // But extra padding (see issue CDM-52) caused each byte to be padded out to 4 bytes.
      assertEquals(1, byteData.getByte(0));
      assertEquals(0, byteData.getByte(1));
      assertEquals(0, byteData.getByte(2));
      assertEquals(0, byteData.getByte(3));
      assertEquals(2, byteData.getByte(4));
      assertEquals(0, byteData.getByte(5));
      assertEquals(0, byteData.getByte(6));
      assertEquals(0, byteData.getByte(7));
      assertEquals(3, byteData.getByte(8));
      assertEquals(0, byteData.getByte(9));
      assertEquals(0, byteData.getByte(10));
      assertEquals(0, byteData.getByte(11));
      assertEquals(4, byteData.getByte(12));
      assertEquals(0, byteData.getByte(13));
      assertEquals(0, byteData.getByte(14));
      assertEquals(0, byteData.getByte(15));
      assertEquals(5, byteData.getByte(16));
      assertEquals(0, byteData.getByte(17));

      try {
        byteData.getByte(18);
      } catch (ArrayIndexOutOfBoundsException e) {
        return;
      } catch (Exception e) {
        fail("Unexpected exception: " + e.getMessage());
        return;
      }
      fail("Failed to throw expected ArrayIndexOutOfBoundsException.");
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOnlyRecordVar() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    Variable.Builder v = writerb.addVariable("v", DataType.BYTE, "v");
    assertEquals(1, v.getElementSize());
    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      assert var != null;

      Array dataArray = Array.factory(DataType.BYTE, new int[] {data.length}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.BYTE);
      assertEquals(1, readVar.getElementSize());

      int[] org = {0};
      byte[] readdata = (byte[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();
      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOneOfTwoRecordVars() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    Variable.Builder v = writerb.addVariable("v", DataType.BYTE, "v");
    assertEquals(1, v.getElementSize());
    writerb.addVariable("v2", DataType.BYTE, "v");

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      assert var != null;

      Variable var2 = ncfWriteable.findVariable("v2");
      assert var2 != null;

      Array dataArray = Array.factory(DataType.BYTE, new int[] {data.length}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.BYTE);
      assertEquals(1, readVar.getElementSize());

      Variable readVar2 = ncf.findVariable("v2");
      assertEquals(readVar2.getDataType(), DataType.BYTE);
      assertEquals(1, readVar2.getElementSize());

      int[] org = {0};
      byte[] readdata = (byte[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOnlyRecordVar() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder v = writerb.addVariable("v", DataType.BYTE, "v s");
    assertEquals(1, v.getElementSize());
    writerb.addVariable("v2", DataType.BYTE, "v");

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      Array dataArray = Array.factory(DataType.BYTE, new int[] {4, 3}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.BYTE);
      assertEquals(1, readVar.getElementSize());

      int[] org = {0, 0};
      byte[] readdata = (byte[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOneOfTwoRecordVars() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder v = writerb.addVariable("v", DataType.BYTE, "v s");
    assertEquals(1, v.getElementSize());
    writerb.addVariable("v2", DataType.BYTE, "v");

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      Array dataArray = Array.factory(DataType.BYTE, new int[] {4, 3}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.BYTE);
      assertEquals(1, readVar.getElementSize());
      Variable readVar2 = ncf.findVariable("v2");
      assertEquals(readVar2.getDataType(), DataType.BYTE);
      assertEquals(1, readVar2.getElementSize());

      int[] org = {0, 0};
      byte[] readdata = (byte[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOnlyRecordVar() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());

    Variable.Builder v = writerb.addVariable("v", DataType.CHAR, "v");
    assertEquals(1, v.getElementSize());

    char[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      Array dataArray = Array.factory(DataType.CHAR, new int[] {data.length}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.CHAR);
      assertEquals(1, readVar.getElementSize());

      int[] org = {0};
      char[] readdata = (char[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOneOfTwoRecordVars() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder v = writerb.addVariable("v", DataType.CHAR, "v s");
    assertEquals(1, v.getElementSize());
    writerb.addVariable("v2", DataType.CHAR, "v");

    char[] data = {1, 2, 3, 40, 41, 42, 50, 51, 52, 60, 61, 62};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");

      Array dataArray = Array.factory(DataType.CHAR, new int[] {4, 3}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.CHAR);
      assertEquals(1, readVar.getElementSize());
      Variable readVar2 = ncf.findVariable("v2");
      assertEquals(readVar2.getDataType(), DataType.CHAR);
      assertEquals(1, readVar2.getElementSize());

      int[] org = {0, 0};
      char[] readdata = (char[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOnlyRecordVar() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());

    Variable.Builder v = writerb.addVariable("v", DataType.SHORT, "v");
    assertEquals(2, v.getElementSize());

    short[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      Array dataArray = Array.factory(DataType.SHORT, new int[] {data.length}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.SHORT);
      assertEquals(2, readVar.getElementSize());

      int[] org = {0};
      short[] readdata = (short[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOneOfTwoRecordVars() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder v = writerb.addVariable("v", DataType.SHORT, "v s");
    assertEquals(2, v.getElementSize());
    writerb.addVariable("v2", DataType.SHORT, "v");

    short[] data = {1, 2, 3, 10, 11, 12, -1, -2, -3, -7, -8, -9};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      Array dataArray = Array.factory(DataType.SHORT, new int[] {4, 3}, data);
      ncfWriteable.write(var.getFullNameEscaped(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertEquals(readVar.getDataType(), DataType.SHORT);
      assertEquals(2, readVar.getElementSize());

      Variable readVar2 = ncf.findVariable("v2");
      assertEquals(readVar2.getDataType(), DataType.SHORT);
      assertEquals(2, readVar2.getElementSize());

      int[] org = {0, 0};
      short[] readdata = (short[]) readVar.read(org, readVar.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOriginalByteArrayPaddingTest() throws IOException, InvalidRangeException {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("D").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("X", 5).build());

    Variable.Builder x = writerb.addVariable("X", DataType.DOUBLE, "X");
    assertEquals(8, x.getElementSize());
    Variable.Builder v = writerb.addVariable("V", DataType.BYTE, "D");
    assertEquals(1, v.getElementSize());

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("V");
      Array arr = Array.factory(DataType.BYTE, new int[] {data.length}, data);
      ncfWriteable.write(var.getFullNameEscaped(), arr);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable inv = ncf.findVariable("V");
      assertEquals(inv.getDataType(), DataType.BYTE);
      assertEquals(1, inv.getElementSize());

      int[] org = {0};
      byte[] readdata = (byte[]) inv.read(org, inv.getShape()).copyTo1DJavaArray();

      assertArrayEquals(data, readdata);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithPaddingInVsize() throws IOException {
    File dataFile = new File(testDir, "files/tst_small.nc");
    try (NetcdfFile ncFile = NetcdfFiles.open(dataFile.getPath(), null)) {
      Variable readVar = ncFile.findVariable("Times");
      assertDataAsExpected(readVar);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithoutPaddingInVsize() throws IOException {
    File dataFile = new File(testDir, "files/tst_small_withoutPaddingInVsize.nc");
    try (NetcdfFile ncFile = NetcdfFiles.open(dataFile.getPath(), null)) {
      Variable readVar = ncFile.findVariable("Times");

      assertDataAsExpected(readVar);
    }
  }

  private void assertDataAsExpected(Variable var) throws IOException {
    ArrayChar cdata = (ArrayChar) var.read();
    assert cdata.getString(0).equals("2005-04-11_12:00:00");
    assert cdata.getString(1).equals("2005-04-11_13:00:00");
  }
}
