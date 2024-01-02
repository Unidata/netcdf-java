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
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class TestStructureArrayBB {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  /*
   * <pre>
   * Structure {
   * int f1;
   * int f2(9);
   * 
   * Structure {
   * int g1;
   * int(2) g2;
   * int(3,4) g3;
   * 
   * Structure {
   * int(3) h1;
   * int(2) h2;
   * } nested2(17);
   * 
   * } nested1(10);
   * } s(4);
   * </pre>
   * so
   * <pre>
   * each s record 1010 * 4 = 4040
   * 10 ints
   * 10 nested1 10 * 100 = 1000
   * 15 ints
   * 17 nested2 17 * 5 = 85
   * 5 ints
   */
  @Test
  public void testBB() {
    StructureMembers members = new StructureMembers("s");
    members.addMember("f1", "desc", "units", DataType.INT, new int[] {1});
    members.addMember("f2", "desc", "units", DataType.INT, new int[] {9}); // 10

    StructureMembers.Member nested1 = members.addMember("nested1", "desc", "units", DataType.STRUCTURE, new int[] {10});
    StructureMembers nested1_members = new StructureMembers("nested1");
    nested1_members.addMember("g1", "desc", "units", DataType.INT, new int[] {1});
    nested1_members.addMember("g2", "desc", "units", DataType.INT, new int[] {2});
    nested1_members.addMember("g3", "desc", "units", DataType.INT, new int[] {3, 4}); // (15 + 85) * 10 = 1000

    StructureMembers.Member nested2 =
        nested1_members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[] {17}); // 5 * 17 = 85
    nested1.setStructureMembers(nested1_members);

    StructureMembers nested2_members = new StructureMembers("nested2");
    nested2_members.addMember("h1", "desc", "units", DataType.INT, new int[] {3});
    nested2_members.addMember("h2", "desc", "units", DataType.INT, new int[] {2}); // 5
    nested2.setStructureMembers(nested2_members);

    ArrayStructureBB.setOffsets(members);
    int[] offs = {0, 4, 40};
    for (int i = 0; i < offs.length; ++i) {
      StructureMembers.Member m = members.getMember(i);
      assertThat(m.getDataParam()).isEqualTo(offs[i]);
    }

    int[] offs2 = {0, 4, 12, 60};
    for (int i = 0; i < offs2.length; ++i) {
      StructureMembers.Member m = nested1_members.getMember(i);
      assertThat(m.getDataParam()).isEqualTo(offs2[i]);
    }

    ArrayStructureBB bb = new ArrayStructureBB(members, new int[] {4});
    fillStructureArray(bb);

    new UtilsTestStructureArray().testArrayStructure(bb);

    int sreclen = 1010;
    int n1reclen = 100;
    int n2reclen = 5;

    // get f2 out of the 3rd "s"
    int srecno = 2;
    StructureMembers.Member f2 = bb.getStructureMembers().findMember("f2");
    int[] f2data = bb.getJavaArrayInt(srecno, f2);
    assertThat(f2data[0]).isEqualTo(srecno * sreclen + 1);
    assertThat(f2data[1]).isEqualTo(srecno * sreclen + 2);
    assertThat(f2data[2]).isEqualTo(srecno * sreclen + 3);

    // get nested1 out of the 3rd "s"
    ArrayStructure nested1Data = bb.getArrayStructure(srecno, nested1);
    // get g1 out of the 7th "nested1"
    int n1recno = 6;
    StructureMembers.Member g1 = nested1Data.getStructureMembers().findMember("g1");
    int g1data = nested1Data.getScalarInt(n1recno, g1);
    assertThat(g1data).isEqualTo(srecno * sreclen + n1recno * n1reclen + 10);

    // get nested2 out of the 7th "nested1"
    ArrayStructure nested2Data = nested1Data.getArrayStructure(n1recno, nested2);
    // get h1 out of the 4th "nested2"
    int n2recno = 3;
    StructureMembers.Member h1 = nested2Data.getStructureMembers().findMember("h1");
    int val = nested2Data.getScalarInt(n2recno, h1);
    assertThat(val).isEqualTo(srecno * sreclen + n1recno * n1reclen + n2recno * n2reclen + 15 + 10);
  }

  private void fillStructureArray(ArrayStructureBB sa) {
    ByteBuffer bb = sa.getByteBuffer();
    IntBuffer ibb = bb.asIntBuffer();
    int count = 0;
    for (int i = 0; i < ibb.capacity(); i++)
      ibb.put(i, count++);
  }


  private void fill(Array a) {
    IndexIterator ii = a.getIndexIterator();
    while (ii.hasNext()) {
      ii.getIntNext();
      int[] counter = ii.getCurrentCounter();
      int value = 0;
      for (int i = 0; i < counter.length; i++)
        value = value * 10 + counter[i];
      ii.setIntCurrent(value);
    }
  }


  public ArrayStructure makeNested1(StructureMembers.Member parent) {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("g1", "desc", "units", DataType.INT, new int[] {1});
    Array data = Array.factory(DataType.INT, new int[] {4, 9});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g2", "desc", "units", DataType.DOUBLE, new int[] {2});
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 2});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("g3", "desc", "units", DataType.DOUBLE, new int[] {3, 4});
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 3, 4});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("nested2", "desc", "units", DataType.STRUCTURE, new int[] {7});
    data = makeNested2(m);
    m.setDataArray(data);

    return new ArrayStructureBB(members, new int[] {4, 9});
  }

  public ArrayStructure makeNested2(StructureMembers.Member parent) {
    StructureMembers members = new StructureMembers(parent.getName());
    parent.setStructureMembers(members);

    StructureMembers.Member m = members.addMember("h1", "desc", "units", DataType.INT, new int[] {1});
    Array data = Array.factory(DataType.INT, new int[] {4, 9, 7});
    m.setDataArray(data);
    fill(data);

    m = members.addMember("h2", "desc", "units", DataType.DOUBLE, new int[] {2});
    data = Array.factory(DataType.DOUBLE, new int[] {4, 9, 7, 2});
    m.setDataArray(data);
    fill(data);

    return new ArrayStructureBB(members, new int[] {4, 9, 7});
  }
}

