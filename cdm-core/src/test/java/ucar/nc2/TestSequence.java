/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.TestUtils.makeDummyGroup;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Section;
import ucar.array.StructureData;

/** Test {@link ucar.nc2.Sequence} */
public class TestSequence {

  @Test
  public void testSequence() throws IOException, InvalidRangeException {
    Sequence.Builder<?> structb = Sequence.builder().setName("seq").addMemberVariable("one", ArrayType.BYTE, "")
        .addMemberVariable("two", ArrayType.STRING, "").addMemberVariable("tres", ArrayType.FLOAT, "");

    structb.setSourceData(makeStructureDataArray());
    Structure struct = structb.build(makeDummyGroup());

    Array data = struct.readArray();
    assertThat(data).isNotNull();
    assertThat(data).isInstanceOf(StructureDataArray.class);
    StructureDataArray as = (StructureDataArray) data;
    int count = 0;
    for (StructureData sd : as) {
      assertThat(compare(sd, count)).isTrue();
      count++;
    }
    assertThat(count).isEqualTo(4);
  }

  @Test
  public void testSequenceArray() throws IOException, InvalidRangeException {
    Sequence.Builder<?> structb = Sequence.builder().setName("seq").addMemberVariable("one", ArrayType.BYTE, "")
        .addMemberVariable("two", ArrayType.STRING, "").addMemberVariable("tres", ArrayType.FLOAT, "");

    structb.setSourceData(makeStructureDataArray());
    Structure struct = structb.build(makeDummyGroup());

    ucar.array.Array<?> data = struct.readArray();
    assertThat(data).isNotNull();
    assertThat(data).isInstanceOf(ucar.array.StructureDataArray.class);
    ucar.array.StructureDataArray as = (ucar.array.StructureDataArray) data;
    int count = 0;
    for (ucar.array.StructureData sd : as) {
      count++;
    }
    assertThat(count).isEqualTo(4);
  }

  private StructureDataArray makeStructureDataArray() {
    StructureMembers members = makeStructureMembers();
    int nrecords = 4;

    ByteBuffer bb = ByteBuffer.allocate(nrecords * members.getStorageSizeBytes());
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, nrecords);
    for (int i = 0; i < nrecords; i++) {
      makeStructureData(storage, bb, i);
    }
    return new StructureDataArray(members, new int[] {2, 2}, storage);
  }

  @Test
  public void testUnsupportedMethods() {
    Sequence.Builder<?> structb = Sequence.builder().setName("seq").addMemberVariable("one", ArrayType.BYTE, "")
        .addMemberVariable("two", ArrayType.STRING, "").addMemberVariable("tres", ArrayType.FLOAT, "");

    structb.setSourceData(makeStructureDataArray());
    Structure struct = structb.build(makeDummyGroup());

    try {
      struct.read(new int[] {0}, new int[] {0});
      fail();
    } catch (Exception e) {
      // expected
    }

    try {
      struct.read("0:0");
      fail();
    } catch (Exception e) {
      // expected
    }

    try {
      struct.readRecord(0);
      fail();
    } catch (Exception e) {
      // expected
    }

    try {
      struct.slice(0, 1);
      fail();
    } catch (Exception e) {
      // expected
    }

    try {
      struct.section(new Section());
      fail();
    } catch (Exception e) {
      // expected
    }
  }

  private StructureMembers makeStructureMembers() {
    StructureMembers.Builder builder = StructureMembers.builder().setName("struct");
    builder.addMember("one", "desc1", "units1", ArrayType.BYTE, new int[0]);
    builder.addMember("two", "desc2", "units2", ArrayType.STRING, new int[0]);
    builder.addMember("tres", "desc3", "units4", ArrayType.FLOAT, new int[0]);
    builder.setStandardOffsets(false);
    return builder.build();
  }

  private void makeStructureData(StructureDataStorageBB storage, ByteBuffer bb, int elem) {
    bb.put((byte) elem);
    int heapIdx = storage.putOnHeap(new String[] {"s" + elem});
    bb.putInt(heapIdx);
    bb.putFloat((float) elem);
  }

  private boolean compare(StructureData sdata1, int elem) {
    Array<Byte> m1 = (Array<Byte>) sdata1.getMemberData("one");
    assertThat(m1.getScalar()).isEqualTo(elem);
    Array<String> m2 = (Array<String>) sdata1.getMemberData("two");
    assertThat(m2.getScalar()).isEqualTo("s" + elem);
    Array<Float> m3 = (Array<Float>) sdata1.getMemberData("tres");
    assertThat(m3.getScalar()).isEqualTo(elem);
    return true;
  }

}
