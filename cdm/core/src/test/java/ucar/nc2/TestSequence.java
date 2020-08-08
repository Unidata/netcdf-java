/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.TestUtils.makeDummyGroup;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.ma2.StructureDataScalar;
import ucar.nc2.util.CompareNetcdf2;

/** Test {@link ucar.nc2.Sequence} */
public class TestSequence {

  @Test
  public void testSequence() throws IOException, InvalidRangeException {
    Sequence.Builder<?> structb = Sequence.builder().setName("seq").addMemberVariable("one", DataType.BYTE, "")
        .addMemberVariable("two", DataType.STRING, "").addMemberVariable("tres", DataType.FLOAT, "");

    StructureData sdata = makeStructureData(1);
    ArrayStructureW cacheData = new ArrayStructureW(sdata.getStructureMembers(), new int[] {2, 2});
    for (int i = 0; i < 4; i++) {
      cacheData.setStructureData(makeStructureData(i + 1), i);
    }
    structb.setCachedData(cacheData, true);
    Structure struct = structb.build(makeDummyGroup());

    try {
      struct.readStructure(0);
      fail();
    } catch (Exception e) {
      // expected
    }

    Array data = struct.read();
    assertThat(data).isNotNull();
    assertThat(data).isInstanceOf(ArrayStructure.class);
    ArrayStructure as = (ArrayStructure) data;
    try (StructureDataIterator iter = as.getStructureDataIterator()) {
      int count = 0;
      while (iter.hasNext()) {
        StructureData sd = iter.next();
        assertThat(compare(sd, makeStructureData(count + 1))).isTrue();
        count++;
      }
      assertThat(count).isEqualTo(4);
    }

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
    sdata.addMember("one", "desc1", "units1", DataType.BYTE, (byte) elem);
    sdata.addMemberString("two", "desc2", "units2", "two", 4);
    sdata.addMember("tres", "desc3", "units4", DataType.FLOAT, elem * 3.0f);
    return sdata;
  }


  private boolean compare(StructureData sdata1, StructureData sdata2) {
    Formatter f = new Formatter();
    CompareNetcdf2 compare = new CompareNetcdf2(f);
    boolean ok = compare.compareStructureData(sdata1, sdata2, false);
    if (!ok) {
      System.out.printf("%s%n", f);
    }
    return ok;
  }

}
