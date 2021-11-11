/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.nc2.internal.util.CompareArrayToArray;

import java.nio.ByteBuffer;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link StructureData} and {@link StructureDataArray} with StructureDataSrorageBB */
public class TestStructureDataBB {

  @Test
  public void testBasics() {
    StructureDataArray array = makeStructureArray(11);
    for (StructureData val : array) {
      assertThat(val.getName()).isEqualTo("myname");
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));
    assertThrows(IllegalArgumentException.class, () -> array.get(99));

    StructureMembers members = array.getStructureMembers();
    assertThat(array.getStructureSize()).isEqualTo(members.getStorageSizeBytes());
    assertThat(array.storage().length()).isEqualTo(11);

    StructureData sdata = array.get(0);
    assertThat(sdata.getStructureMembers()).isEqualTo(members);
    assertThat(sdata.getName()).isEqualTo("myname");

    StructureMembers.Member m = sdata.getStructureMembers().findMember("mstrings");
    assertThat(sdata.getMemberData("mstrings")).isEqualTo(sdata.getMemberData(m));
    assertThrows(IllegalArgumentException.class, () -> sdata.getMemberData("bad"));

    Array<StructureData> flipped = Arrays.flip(array, 0);
    Formatter errlog = new Formatter();
    int count = 0;
    Index ima = array.getIndex();
    for (StructureData val : flipped) {
      assertThat(CompareArrayToArray.compareStructureData(errlog, val, array.get(ima.set(10 - count)), false)).isTrue();
    }
  }

  @Test
  public void testCopy() {
    StructureDataArray org = makeStructureArray(7);
    StructureDataArray copy = copyStructureArray(org);
    assertThat(CompareArrayToArray.compareData("arr", org, copy)).isTrue();

    Formatter errlog = new Formatter();
    int count = 0;
    for (StructureData val : org) {
      assertThat(CompareArrayToArray.compareStructureData(errlog, val, copy.get(count++), false)).isTrue();
    }
  }

  @Test
  public void testCombine() {
    StructureDataArray array1 = makeStructureArray(1);
    StructureDataArray array2 = makeStructureArray(2);

    int[] shape = new int[] {3};
    Array<StructureData> array = Arrays.combine(ArrayType.STRUCTURE, shape, ImmutableList.of(array1, array2));

    Formatter errlog = new Formatter();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(0), array1.get(0), false)).isTrue();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(1), array2.get(0), false)).isTrue();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(2), array2.get(1), false)).isTrue();
  }

  @Test
  public void testExtractMemberArrayString() {
    StructureDataArray sdarray = makeStructureArray(7);
    StructureMembers.Member member = sdarray.getStructureMembers().findMember("mstrings");
    Array<String> extracted = (Array<String>) sdarray.extractMemberArray(member);
    assertThat(extracted.getSize()).isEqualTo(21);
    assertThat(extracted.getShape()).isEqualTo(new int[] {7, 3});

    int count = 0;
    for (String val : extracted) {
      int idx = count % 3;
      assertThat(val).isEqualTo("Gimme Shelter" + idx);
      count++;
    }
  }

  private StructureDataArray makeStructureArray(int nelems) {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("myname");
    builder.addMember("mbyte", "mdesc1", "munits1", ArrayType.BYTE, new int[] {11, 11});
    builder.addMember("mdouble", "mdesc2", "munits2", ArrayType.DOUBLE, new int[] {1});
    builder.addMember("mfloat", "mdesc2", "munits2", ArrayType.FLOAT, new int[] {2});
    builder.addMember("mint", "mdesc2", "munits2", ArrayType.INT, new int[] {2, 2});
    builder.addMember("mshort", "mdesc2", "munits2", ArrayType.SHORT, new int[] {2, 1});
    builder.addMember("mlong", "mdesc2", "munits2", ArrayType.LONG, new int[] {1, 1});
    builder.addMember("mstring", "mdesc2", "munits2", ArrayType.STRING, new int[0]);
    builder.addMember("mstrings", "mdesc2", "munits2", ArrayType.STRING, new int[] {3});
    StructureMembers members = builder.setStandardOffsets().build();

    byte[] result = new byte[(int) (nelems * members.getStorageSizeBytes())];
    ByteBuffer bb = ByteBuffer.wrap(result);
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, nelems);
    for (int recno = 0; recno < nelems; recno++) {
      int recstart = recno * members.getStorageSizeBytes();
      for (StructureMembers.Member m : members) {
        int pos = recstart + m.getOffset();
        switch (m.getArrayType()) {
          case BYTE:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.put(pos + idx, (byte) (idx + 100));
            }
            break;
          case DOUBLE:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putDouble(pos + idx, (idx + 3.14));
            }
            break;
          case FLOAT:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putFloat(pos + idx, (float) (idx - 100));
            }
            break;
          case INT:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putInt(pos + idx, (idx - 100));
            }
            break;
          case SHORT:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putShort(pos + idx, (short) (idx + 100));
            }
            break;
          case LONG:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putLong(pos + idx, (idx + 100000000100L));
            }
            break;
          case STRING:
            String[] data = new String[m.length()];
            for (int idx = 0; idx < m.length(); idx++) {
              data[idx] = "Gimme Shelter" + idx;
            }
            bb.putInt(pos, storage.putOnHeap(data));
            break;
        }
      }
    }
    return new StructureDataArray(members, new int[] {nelems}, storage);
  }

  private StructureDataArray copyStructureArray(StructureDataArray org) {
    StructureMembers members = org.getStructureMembers().toBuilder().build();

    int nelems = (int) org.length();
    byte[] result = new byte[(int) (nelems * members.getStorageSizeBytes())];
    ByteBuffer bb = ByteBuffer.wrap(result);
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, nelems);
    for (int recno = 0; recno < nelems; recno++) {
      int recstart = recno * members.getStorageSizeBytes();
      for (StructureMembers.Member m : members) {
        storage.setMemberData(recstart, m, org.get(recno).getMemberData(m));
      }
    }
    return new StructureDataArray(members, new int[] {nelems}, storage);
  }

}
