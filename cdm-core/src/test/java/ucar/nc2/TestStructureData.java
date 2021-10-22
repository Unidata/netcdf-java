/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.StructureData;
import ucar.nc2.internal.util.CompareArrayToArray;

/** Test {@link ucar.nc2.Structure} and {@link ucar.ma2.StructureData} */
public class TestStructureData {

  @Test
  public void testScalarStructure() throws IOException, InvalidRangeException {
    Group.Builder parent = Group.builder();
    Structure.Builder<?> structb =
        Structure.builder().setName("struct").setParentGroupBuilder(parent).addMemberVariable("one", ArrayType.BYTE, "")
            .addMemberVariable("two", ArrayType.STRING, "").addMemberVariable("tres", ArrayType.FLOAT, "");

    StructureData sdata = makeStructureData(1);
    ArrayStructureW cacheData = new ArrayStructureW(sdata.getStructureMembers(), new int[] {1});
    cacheData.setStructureData(sdata, 0);
    structb.setSourceData(cacheData);
    Structure struct = structb.build(makeDummyGroup());

    StructureData scalarData = struct.readStructure(0);
    assertThat(scalarData).isNotNull();
    assertThat(compare(scalarData, sdata)).isTrue();

    try (StructureData iter = struct.getStructureIterator()) {
      int count = 0;
      while (iter.hasNext()) {
        StructureData sd = iter.next();
        assertThat(compare(sd, sdata)).isTrue();
        count++;
      }
      assertThat(count).isEqualTo(1);
    }
  }

  @Test
  public void testMultidimStructure() throws IOException, InvalidRangeException {
    Group.Builder parent = Group.builder();
    Structure.Builder<?> structb =
        Structure.builder().setName("struct").addDimension(Dimension.builder().setLength(2).setIsShared(false).build())
            .addDimension(Dimension.builder().setLength(2).setIsShared(false).build()).setParentGroupBuilder(parent)
            .addMemberVariable("one", ArrayType.BYTE, "").addMemberVariable("two", ArrayType.STRING, "")
            .addMemberVariable("tres", ArrayType.FLOAT, "");

    StructureData sdata = makeStructureData(1);
    ArrayStructureW cacheData = new ArrayStructureW(sdata.getStructureMembers(), new int[] {2, 2});
    for (int i = 0; i < 4; i++) {
      cacheData.setStructureData(makeStructureData(i + 1), i);
    }
    structb.setSourceData(cacheData);
    structb.setIsCaching(true);
    Structure struct = structb.build(makeDummyGroup());

    StructureData scalarData = struct.readStructure(0);
    assertThat(scalarData).isNotNull();
    assertThat(compare(scalarData, sdata)).isTrue();

    try (StructureDataIterator iter = struct.getStructureIterator()) {
      int count = 0;
      while (iter.hasNext()) {
        StructureData sd = iter.next();
        assertThat(compare(sd, makeStructureData(count + 1))).isTrue();
        count++;
      }
      assertThat(count).isEqualTo(4);
    }
  }

  private StructureData makeStructureData(int elem) {
    StructureDataScalar sdata = new StructureDataScalar("struct");
    sdata.addMember("one", "desc1", "units1", ArrayType.BYTE, (byte) elem);
    sdata.addMemberString("two", "desc2", "units2", "two", 4);
    sdata.addMember("tres", "desc3", "units4", ArrayType.FLOAT, elem * 3.0f);
    return sdata;
  }


  private boolean compare(StructureData sdata1, StructureData sdata2) throws IOException {
    Formatter f = new Formatter();
    CompareArrayToArray compare = new CompareArrayToArray();
    boolean ok = compare.compareStructureData(sdata1, sdata2);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    return ok;
  }


}
