/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.unidata.util.test.UtilsTestStructureArray;
import java.lang.invoke.MethodHandles;

public class TestArrayStructureW {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /*
   * <pre>
   * Structure {
   * float f1;
   * short f2(3);
   * 
   * Structure {
   * int g1;
   * double(2) g2;
   * double(3,4) g3;
   * 
   * Structure {
   * int h1;
   * double(2) h2;
   * } nested2(7);
   * 
   * } nested1(9);
   * } s(4);
   * </pre>
   * 
   * <ul>
   * <li>For f1, you need an ArrayFloat of shape {4}
   * <li>For f2, you need an ArrayShort of shape {4, 3} .
   * <li>For nested1, you need an ArrayStructure of shape {4, 9}.
   * Use an ArrayStructureMA that has 3 members:
   * <ul><li>For g1, you need an ArrayInt of shape (4, 9}
   * <li>For g2, you need an ArrayDouble of shape {4, 9, 2}.
   * <li>For g3, you need an ArrayDouble of shape {4, 9, 3, 4}.
   * </ul>
   * <li>For nested2, you need an ArrayStructure of shape {4, 9, 7}.
   * Use an ArrayStructureMA that has 2 members:
   * <ul><li>For h1, you need an ArrayInt of shape (4, 9, 7}
   * <li>For h2, you need an ArrayDouble of shape {4, 9, 7, 2}.
   * </ul>
   * </ul>
   */
  @Test
  public void testW() {
    StructureMembers members = new StructureMembers("s");

    StructureMembers.Member f1 = members.addMember("f1", "desc", "units", DataType.FLOAT, new int[] {1});

    StructureMembers.Member f2 = members.addMember("f2", "desc", "units", DataType.SHORT, new int[] {3});

    StructureMembers.Member nested1 = members.addMember("nested1", "desc", "units", DataType.STRUCTURE, new int[] {9});

    int size = 4;
    StructureData[] sdata = new StructureData[size];
    for (int i = 0; i < size; i++) {
      StructureDataW sdw = new StructureDataW(members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.FLOAT, new int[] {1});
      sdw.setMemberData(f1, data);
      fill(data, i);

      data = Array.factory(DataType.SHORT, new int[] {3});
      sdw.setMemberData(f2, data);
      fill(data, i * 2);

      data = makeNested1(nested1, 9, 7);
      sdw.setMemberData(nested1, data);
    }

    ArrayStructureW as = new ArrayStructureW(members, new int[] {4}, sdata);
    new UtilsTestStructureArray().testArrayStructure(as);

    // get f2 out of the 2nd "s"
    short[] f2data = as.getJavaArrayShort(1, f2);
    assertThat(f2data[0]).isEqualTo(2);
    assertThat(f2data[1]).isEqualTo(3);
    assertThat(f2data[2]).isEqualTo(4);

    // get nested1 out of the 3rd "s"
    ArrayStructure nested1Data = as.getArrayStructure(2, nested1);

    // get g1 out of the 4th "nested1"
    StructureMembers.Member g1 = nested1Data.getStructureMembers().findMember("g1");
    int g1data = nested1Data.getScalarInt(3, g1);
    assertThat(g1data).isEqualTo(66);

    // get g3 out of the 4th "nested1"
    StructureMembers.Member g3 = nested1Data.getStructureMembers().findMember("g3");
    double[] g3data = nested1Data.getJavaArrayDouble(3, g3);
    assertThat(g3data[0]).isEqualTo(73326.0);

    // get nested2 out of the 7th "nested1"
    StructureMembers.Member nested2 = nested1Data.getStructureMembers().findMember("nested2");
    ArrayStructure nested2Data = nested1Data.getArrayStructure(6, nested2);

    // get h1 out of the 5th "nested2"
    StructureMembers.Member h1 = nested2Data.getStructureMembers().findMember("h1");
    int val = nested2Data.getScalarInt(4, h1);
    assertThat(val).isEqualTo(1218);

    // get h2 out of the 5th "nested2"
    StructureMembers.Member h2 = nested2Data.getStructureMembers().findMember("h2");
    double[] h2data = nested2Data.getJavaArrayDouble(4, h2);
    assertThat(h2data[0]).isEqualTo(12018);
    assertThat(h2data[1]).isEqualTo(12019);
  }

  public Array makeNested1(StructureMembers.Member nested1, int size1, int size2) {
    StructureMembers members = new StructureMembers(nested1.getName());
    nested1.setStructureMembers(members);

    StructureMembers.Member g1 = members.addMember("g1", "desc", "units", DataType.INT, new int[] {1});
    StructureMembers.Member g2 = members.addMember("g2", "desc", "units", DataType.DOUBLE, new int[] {2});
    StructureMembers.Member g3 = members.addMember("g3", "desc", "units", DataType.DOUBLE, new int[] {3, 4});
    StructureMembers.Member nested2 = members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[] {7});

    StructureData[] sdata = new StructureData[size1];
    for (int i = 0; i < size1; i++) {
      StructureDataW sdw = new StructureDataW(members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.INT, new int[] {1});
      sdw.setMemberData(g1, data);
      fill(data, i * 22);

      data = Array.factory(DataType.DOUBLE, new int[] {2});
      sdw.setMemberData(g2, data);
      fill(data, i * 222);

      data = Array.factory(DataType.DOUBLE, new int[] {3, 4});
      sdw.setMemberData(g3, data);
      fill(data, i * 2222);

      data = makeNested2(nested2, i, size2);
      sdw.setMemberData(nested2, data);
    }

    return new ArrayStructureW(members, new int[] {size1}, sdata);
  }

  public Array makeNested2(StructureMembers.Member nested, int who, int size) {
    StructureMembers members = new StructureMembers(nested.getName());
    nested.setStructureMembers(members);

    StructureMembers.Member h1 = members.addMember("h1", "desc", "units", DataType.INT, new int[] {1});
    StructureMembers.Member h2 = members.addMember("h2", "desc", "units", DataType.DOUBLE, new int[] {2});

    StructureData[] sdata = new StructureData[size];
    for (int i = 0; i < size; i++) {
      StructureDataW sdw = new StructureDataW(members);
      sdata[i] = sdw;

      Array data = Array.factory(DataType.INT, new int[] {1});
      sdw.setMemberData(h1, data);
      fill(data, i * 303 + who);

      data = Array.factory(DataType.DOUBLE, new int[] {2});
      sdw.setMemberData(h2, data);
      fill(data, i * 3003 + who);
    }

    return new ArrayStructureW(members, new int[] {size}, sdata);
  }

  private void fill(Array a, int start) {
    IndexIterator ii = a.getIndexIterator();
    while (ii.hasNext()) {
      ii.getIntNext();
      int[] counter = ii.getCurrentCounter();
      int value = 0;
      for (int i = 0; i < counter.length; i++)
        value = start + value * 10 + counter[i];
      ii.setIntCurrent(value);
    }
  }
}

