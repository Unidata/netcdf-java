/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Formatter;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataScalar;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareArrayToMa2;
import ucar.nc2.internal.util.CompareNetcdf2;

/** Test {@link ArraysConvert} */
public class TestArraysConvert {

  private Array<Double> array;
  private ArrayStructure ma2Struct;
  private StructureDataArray structureDataArray;

  @Before
  public void setup() {
    int[] shape = new int[] {1, 2, 3};
    double[] data = new double[] {1, 2, 3, 4, 5, 6};
    array = Arrays.factory(ArrayType.DOUBLE, shape, data);

    StructureData sdata = makeStructureDataMa2(1);
    ma2Struct = new ArrayStructureW(sdata.getStructureMembers(), new int[] {1}, new StructureData[] {sdata});

    structureDataArray = makeStructureDataArray();
  }

  @Test
  public void testConvert() {
    ucar.ma2.Array ma2 = ArraysConvert.convertFromArray(array);
    assertThat(ma2.getDataType()).isEqualTo(array.getArrayType().getDataType());
    assertThat(ma2.getShape()).isEqualTo(array.getShape());
    assertThat(ma2.getSize()).isEqualTo(array.length());

    Iterator<Double> iter = array.iterator();
    IndexIterator indexIter = ma2.getIndexIterator();
    while (indexIter.hasNext() && iter.hasNext()) {
      assertThat(indexIter.getDoubleNext()).isEqualTo(iter.next());
    }
  }

  @Test
  public void testConvertStructureDataToArray() throws IOException {
    Array<?> sda = ArraysConvert.convertToArray(ma2Struct);
    assertThat(sda.getArrayType().getDataType()).isEqualTo(ma2Struct.getDataType());
    assertThat(sda.getShape()).isEqualTo(ma2Struct.getShape());
    assertThat(sda.length()).isEqualTo(ma2Struct.getSize());

    Formatter f = new Formatter();
    boolean ok = CompareArrayToMa2.compareData(f, "testConvertStructureData", ma2Struct, sda, false, true);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    assertThat(ok).isTrue();
  }

  @Test
  public void testConvertStructureDataRoundtrip() throws IOException {
    Array<?> sda = ArraysConvert.convertToArray(ma2Struct);
    ucar.ma2.Array converted = ArraysConvert.convertFromArray(sda);
    assertThat(converted.getDataType()).isEqualTo(ma2Struct.getDataType());
    assertThat(converted.getShape()).isEqualTo(ma2Struct.getShape());
    assertThat(converted.getSize()).isEqualTo(ma2Struct.getSize());

    Formatter f = new Formatter();
    CompareNetcdf2 compare = new CompareNetcdf2(f);
    boolean ok = compare.compareData("testConvertStructureDataRoundtrip", ma2Struct, converted);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    assertThat(ok).isTrue();
  }

  @Test
  public void testConvertStructureDataToMa2() throws IOException {
    ucar.ma2.Array ma2 = ArraysConvert.convertFromArray(structureDataArray);
    assertThat(ma2.getDataType()).isEqualTo(structureDataArray.getArrayType().getDataType());
    assertThat(ma2.getShape()).isEqualTo(structureDataArray.getShape());
    assertThat(ma2.getSize()).isEqualTo(structureDataArray.length());

    Formatter f = new Formatter();
    boolean ok =
        CompareArrayToMa2.compareData(f, "testConvertStructureDataToMa2", ma2, structureDataArray, false, true);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    assertThat(ok).isTrue();
  }

  @Test
  public void testConvertStructureDataToMa2Roundtrip() throws IOException {
    ucar.ma2.Array ma2 = ArraysConvert.convertFromArray(structureDataArray);
    Array<?> converted = ArraysConvert.convertToArray(ma2);
    assertThat(converted.getArrayType()).isEqualTo(structureDataArray.getArrayType());
    assertThat(converted.getShape()).isEqualTo(structureDataArray.getShape());
    assertThat(converted.length()).isEqualTo(structureDataArray.length());

    Formatter f = new Formatter();
    boolean ok = CompareArrayToArray.compareData(f, "testConvertStructureDataToMa2Roundtrip", converted,
        structureDataArray, false, true);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    assertThat(ok).isTrue();
  }

  private StructureData makeStructureDataMa2(int elem) {
    StructureDataScalar sdata = new StructureDataScalar("struct");
    sdata.addMember("one", "desc1", "units1", DataType.BYTE, (byte) elem);
    sdata.addMemberString("two", "desc2", "units2", "two", 4);
    sdata.addMember("tres", "desc3", "units4", DataType.FLOAT, elem * 3.0f);
    return sdata;
  }

  private StructureDataArray makeStructureDataArray() {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("name");
    builder.addMember("mbyte", "mdesc1", "munits1", ArrayType.BYTE, new int[] {11, 11});
    builder.addMember("mfloat", "mdesc2", "munits1", ArrayType.FLOAT, new int[] {});
    builder.setStandardOffsets(false);
    StructureMembers members = builder.build();

    int nrows = 2;
    ByteBuffer bbuffer = ByteBuffer.allocate(nrows * members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, nrows);
    for (int row = 0; row < nrows; row++) {
      for (StructureMembers.Member m : members) {
        if (m.getName().equals("mbyte")) {
          for (int i = 0; i < m.length(); i++) {
            bbuffer.put((byte) i);
          }
        } else if (m.getName().equals("mfloat")) {
          bbuffer.putFloat(99.5f);
        }
      }
    }
    return new StructureDataArray(members, new int[] {nrows}, storage);
  }

  @Test
  public void testConvertSection() throws InvalidRangeException {
    Section sa = Section.builder().appendRange(2).appendRange(null).appendRange(Range.EMPTY).appendRange(Range.SCALAR)
        .appendRange(Range.VLEN).appendRange("test", 3, 4, 2).build();
    ucar.ma2.Section sb = ArraysConvert.convertSection(sa);
    assertThat(ArraysConvert.convertSection(sb)).isEqualTo(sa);
  }

}
