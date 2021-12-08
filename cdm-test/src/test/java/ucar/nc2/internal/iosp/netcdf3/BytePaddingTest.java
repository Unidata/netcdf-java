/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.netcdf3;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.*;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/**
 * testing netcdf3 byte padding
 *
 * @author edavis
 * @since 4.1
 */
public class BytePaddingTest {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();
  public static String testDir = TestDir.cdmUnitTestDir + "formats/netcdf3/";

  @Test
  public void checkReadOfFileWrittenWithIncorrectPaddingOfOneDimByteArrayOnlyRecordVar() throws IOException {
    // File testDataDir = new File( TestDir.cdmLocalTestDataDir, "ucar/nc2/iosp/netcdf3");
    File testFile = new File(TestDir.cdmLocalTestDataDir, "byteArrayRecordVarPaddingTest-bad.nc");
    assertThat(testFile.exists()).isTrue();
    assertThat(testFile.canRead()).isTrue();

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("V");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(readVar.getElementSize()).isEqualTo(1);
      assertThat(readVar.getSize()).isEqualTo(18);

      Array<Byte> byteData = (Array<Byte>) readVar.readArray();

      // File was created with the following data
      // byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
      // But extra padding (see issue CDM-52) caused each byte to be padded out to 4 bytes.

      byte[] expected = {1, 0, 0, 0, 2, 0, 0, 0, 3, 0, 0, 0, 4, 0, 0, 0, 5, 0, 0, 0};

      int count = 0;
      for (byte b : byteData) {
        assertThat(b).isEqualTo(expected[count++]);
      }

      assertThrows(IllegalArgumentException.class, () -> byteData.get(18));
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOnlyRecordVar() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.BYTE, "v");
    assertThat(v.dataType).isEqualTo(ArrayType.BYTE);
    assertThat(v.getElementSize()).isEqualTo(1);
    assertThat(v.getSize()).isEqualTo(0);

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array<?> dataArray = Arrays.factory(ArrayType.BYTE, new int[] {data.length}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      assertThat(var).isNotNull();
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(readVar.getElementSize()).isEqualTo(1);
      assertThat(readVar.getSize()).isEqualTo(18);

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimByteArrayOneOfTwoRecordVars() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.BYTE, "v");
    assertThat(v.dataType).isEqualTo(ArrayType.BYTE);
    writerb.addVariable("v2", ArrayType.BYTE, "v");

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array<?> dataArray = Arrays.factory(ArrayType.BYTE, new int[] {data.length}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      assertThat(var).isNotNull();

      Variable var2 = ncfWriteable.findVariable("v2");
      assertThat(var2).isNotNull();

      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(readVar.getElementSize());

      Variable readVar2 = ncf.findVariable("v2");
      assertThat(readVar2).isNotNull();
      assertThat(readVar2.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(readVar2.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOnlyRecordVar() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.BYTE, "v s");
    assertThat(v.dataType).isEqualTo(ArrayType.BYTE);
    writerb.addVariable("v2", ArrayType.BYTE, "v");

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};
    Array<?> dataArray = Arrays.factory(ArrayType.BYTE, new int[] {4, 3}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(readVar.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadTwoDimByteArrayOneOfTwoRecordVars() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.BYTE, "v s");
    assertThat(v.dataType).isEqualTo(ArrayType.BYTE);
    writerb.addVariable("v2", ArrayType.BYTE, "v");

    byte[] data = {1, 2, 3, 11, 12, 13, 21, 22, 23, -1, -2, -3};
    Array<?> dataArray = Arrays.factory(ArrayType.BYTE, new int[] {4, 3}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(readVar.getElementSize());
      Variable readVar2 = ncf.findVariable("v2");
      assertThat(readVar2.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(readVar2.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOnlyRecordVar() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.CHAR, "v");
    assertThat(v.dataType).isEqualTo(ArrayType.CHAR);

    char[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};
    Array<?> dataArray = Arrays.factory(ArrayType.CHAR, new int[] {data.length}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.CHAR);
      assertThat(1).isEqualTo(readVar.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimCharArrayOneOfTwoRecordVars() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.CHAR, "v s");
    assertThat(v.dataType).isEqualTo(ArrayType.CHAR);
    writerb.addVariable("v2", ArrayType.CHAR, "v");

    char[] data = {1, 2, 3, 40, 41, 42, 50, 51, 52, 60, 61, 62};
    Array<?> dataArray = Arrays.factory(ArrayType.CHAR, new int[] {4, 3}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");

      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.CHAR);
      assertThat(1).isEqualTo(readVar.getElementSize());

      Variable readVar2 = ncf.findVariable("v2");
      assertThat(readVar2).isNotNull();
      assertThat(readVar2.getArrayType()).isEqualTo(ArrayType.CHAR);
      assertThat(1).isEqualTo(readVar2.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOnlyRecordVar() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.SHORT, "v");

    short[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array<?> dataArray = Arrays.factory(ArrayType.SHORT, new int[] {data.length}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(2).isEqualTo(readVar.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOneDimShortArrayOneOfTwoRecordVars() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("v").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("s", 3).build());

    Variable.Builder<?> v = writerb.addVariable("v", ArrayType.SHORT, "v s");
    writerb.addVariable("v2", ArrayType.SHORT, "v");

    short[] data = {1, 2, 3, 10, 11, 12, -1, -2, -3, -7, -8, -9};
    Array<?> dataArray = Arrays.factory(ArrayType.SHORT, new int[] {4, 3}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("v");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable readVar = ncf.findVariable("v");
      assertThat(readVar).isNotNull();
      assertThat(readVar.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(2).isEqualTo(readVar.getElementSize());

      Variable readVar2 = ncf.findVariable("v2");
      assertThat(readVar2).isNotNull();
      assertThat(readVar2.getArrayType()).isEqualTo(ArrayType.SHORT);
      assertThat(2).isEqualTo(readVar2.getElementSize());

      Array<?> readdata = readVar.readArray(readVar.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  public void checkPaddingOnWriteReadOriginalByteArrayPaddingTest() throws Exception {
    File tmpDataDir = tempFolder.newFolder();
    File testFile = new File(tmpDataDir, "file.nc");

    NetcdfFormatWriter.Builder<?> writerb = NetcdfFormatWriter.createNewNetcdf3(testFile.getPath());
    writerb.addDimension(Dimension.builder().setName("D").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder("X", 5).build());

    Variable.Builder<?> x = writerb.addVariable("X", ArrayType.DOUBLE, "X");
    Variable.Builder<?> v = writerb.addVariable("V", ArrayType.BYTE, "D");

    byte[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -2, -3, -4, -5, -6, -7, -8, -9};
    Array<?> dataArray = Arrays.factory(ArrayType.BYTE, new int[] {data.length}, data);

    try (NetcdfFormatWriter ncfWriteable = writerb.build()) {
      Variable var = ncfWriteable.findVariable("V");
      ncfWriteable.write(var, dataArray.getIndex(), dataArray);
    }

    try (NetcdfFile ncf = NetcdfFiles.open(testFile.getPath())) {
      Variable inv = ncf.findVariable("V");
      assertThat(inv).isNotNull();
      assertThat(inv.getArrayType()).isEqualTo(ArrayType.BYTE);
      assertThat(1).isEqualTo(inv.getElementSize());

      Array<?> readdata = inv.readArray(inv.getSection());
      CompareArrayToArray.compareData("v", dataArray, readdata);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithPaddingInVsize() throws IOException {
    File dataFile = new File(testDir, "files/tst_small.nc");
    try (NetcdfFile ncFile = NetcdfFiles.open(dataFile.getPath(), null)) {
      Variable readVar = ncFile.findVariable("Times");
      assertThat(readVar).isNotNull();
      assertDataAsExpected(readVar);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void checkReadWithoutPaddingInVsize() throws IOException {
    File dataFile = new File(testDir, "files/tst_small_withoutPaddingInVsize.nc");
    try (NetcdfFile ncFile = NetcdfFiles.open(dataFile.getPath(), null)) {
      Variable readVar = ncFile.findVariable("Times");
      assertThat(readVar).isNotNull();
      assertDataAsExpected(readVar);
    }
  }

  private void assertDataAsExpected(Variable var) throws IOException {
    Array<Byte> cdata = (Array<Byte>) var.readArray();
    Array<String> sdata = Arrays.makeStringsFromChar(cdata);
    assertThat(sdata.get(0)).isEqualTo("2005-04-11_12:00:00");
    assertThat(sdata.get(1)).isEqualTo("2005-04-11_13:00:00");
  }
}
