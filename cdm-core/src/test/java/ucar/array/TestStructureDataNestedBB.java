/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.nc2.internal.util.CompareArrayToArray;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

/** Test {@link StructureData} and {@link StructureDataArray} with StructureDataSrorageBB, structsOnHeap = false */
public class TestStructureDataNestedBB {

  @Test
  public void testBasics() {
    StructureDataArray array = makeStructureArray(21);
    for (StructureData val : array) {
      assertThat(val.getName()).isEqualTo("myname");
    }

    assertThrows(IllegalArgumentException.class, () -> array.get(0, 2, 2));
    assertThrows(IllegalArgumentException.class, () -> array.get(0, 1));
    assertThrows(IllegalArgumentException.class, () -> array.get(99));

    StructureMembers members = array.getStructureMembers();
    assertThat(array.getStructureSize()).isEqualTo(members.getStorageSizeBytes());
    assertThat(array.storage().length()).isEqualTo(21);

    StructureMembers.Member m = array.getStructureMembers().findMember("mstring");
    assertThat(m).isNotNull();

    StructureData sdata0 = array.get(0);
    assertThat(sdata0.getStructureMembers()).isEqualTo(members);
    assertThat(sdata0.getName()).isEqualTo("myname");

    StructureMembers.Member m0 = sdata0.getStructureMembers().findMember("mstring");
    assertThat(m0).isNotNull();
    assertThat(m0).isEqualTo(m);
    assertThat(sdata0.getMemberData("mstring")).isEqualTo(sdata0.getMemberData(m0));
    assertThrows(IllegalArgumentException.class, () -> sdata0.getMemberData("bad"));

    // top level strings
    int recno = 0;
    for (StructureData sdata : array) {
      Array<String> ssdata = (Array<String>) sdata.getMemberData(m);
      int idx = 0;
      for (String s : ssdata) {
        assertThat(s).isEqualTo("Gimme Shelter" + idx + "-" + recno);
        idx++;
      }
      recno++;
    }

    // strings in nested struct
    for (StructureData sdata : array) {
      StructureMembers.Member mstruct = sdata.getStructureMembers().findMember("mstruct");
      Array<StructureData> nsdata = (Array<StructureData>) sdata.getMemberData(mstruct);

      int nrecno = 0;
      for (StructureData ndata : nsdata) {
        StructureMembers.Member nstrings = ndata.getStructureMembers().findMember("nstrings");
        Array<String> ssdata = (Array<String>) ndata.getMemberData(nstrings);
        int idx = 0;
        for (String s : ssdata) {
          assertThat(s).isEqualTo("Or else" + idx + "-" + nrecno);
          idx++;
        }
        nrecno++;
      }
    }

    // strings in nested seq
    for (StructureData sdata : array) {
      StructureMembers.Member mstruct = sdata.getStructureMembers().findMember("mseq");
      Array<StructureData> nsdata = (Array<StructureData>) sdata.getMemberData(mstruct);

      int nrecno = 0;
      for (StructureData ndata : nsdata) {
        StructureMembers.Member nstrings = ndata.getStructureMembers().findMember("nstrings");
        Array<String> ssdata = (Array<String>) ndata.getMemberData(nstrings);
        int idx = 0;
        for (String s : ssdata) {
          assertThat(s).isEqualTo("Or else fade away" + idx + "-" + nrecno);
          idx++;
        }
        nrecno++;
      }
    }

    int ntop = (int) array.storage().length();
    Array<StructureData> flipped = Arrays.flip(array, 0);
    int fcount = 0;
    Index ima = array.getIndex();
    for (StructureData val : flipped) {
      Formatter errlog = new Formatter();
      boolean ok = CompareArrayToArray.compareStructureData(errlog, val, array.get(ima.set(ntop - fcount - 1)), true);
      if (!ok) {
        System.out.printf("fail = %s%n", errlog);
      }
      fcount++;
    }
  }

  /*
   * @Test
   * public void testCopy() throws IOException {
   * StructureDataArray org = makeStructureArray(7);
   * StructureDataArray copy = copyStructureArray(org);
   * assertThat(CompareArrayToArray.compareData("arr", org, copy)).isTrue();
   * 
   * Formatter errlog = new Formatter();
   * int count = 0;
   * for (StructureData val : org) {
   * assertThat(CompareArrayToArray.compareStructureData(errlog, val, copy.get(count++), false)).isTrue();
   * }
   * }
   */

  @Test
  public void testExtract() {
    StructureDataArray top = makeStructureArray(7);
    StructureMembers.Member mstruct = top.getStructureMembers().findMember("mstruct");
    assertThat(mstruct).isNotNull();
    StructureMembers.Member nested = mstruct.getStructureMembers().findMember("nstrings");

    Array<String> extracted = (Array<String>) top.extractNestedMemberArray(mstruct, nested);
    assertThat(extracted.getSize()).isEqualTo(147);
    assertThat(extracted.getShape()).isEqualTo(new int[] {7, 7, 3});

    int count = 0;
    for (String val : extracted) {
      int recno = (count / 3) % 7;
      int idx = count % 3;
      if (!val.equals("Or else" + idx + "-" + recno)) {
        System.out.printf("HEY");
      }
      assertThat(val).isEqualTo("Or else" + idx + "-" + recno);
      count++;
    }
  }

  @Test
  public void testCombine() throws IOException {
    StructureDataArray array1 = makeStructureArray(1);
    StructureDataArray array2 = makeStructureArray(2);

    int[] shape = new int[] {3};
    Array<StructureData> array = Arrays.combine(ArrayType.STRUCTURE, shape, ImmutableList.of(array1, array2));

    Formatter errlog = new Formatter();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(0), array1.get(0), false)).isTrue();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(1), array2.get(0), false)).isTrue();
    assertThat(CompareArrayToArray.compareStructureData(errlog, array.get(2), array2.get(1), false)).isTrue();
  }

  private StructureDataArray makeStructureArray(int nelems) {
    StructureMembers.Builder nbuilder = StructureMembers.builder();
    nbuilder.setName("nested");
    nbuilder.addMember("nbyte", "mdesc1", "munits1", ArrayType.BYTE, new int[] {11, 11});
    nbuilder.addMember("ndouble", "mdesc2", "munits2", ArrayType.DOUBLE, new int[] {1});
    nbuilder.addMember("nstrings", "mdesc2", "munits2", ArrayType.STRING, new int[] {3});
    StructureMembers nestedMembers = nbuilder.setStandardOffsets().build();
    System.out.printf("nested members = %s%n", nestedMembers.showOffsets());

    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("myname");
    builder.addMember("mstring", "mdesc1", "munits1", ArrayType.STRING, new int[] {11, 11});
    builder.addMember("mopaque", "mdesc1", "munits1", ArrayType.OPAQUE, new int[] {3});
    builder.addMember("mstruct", "mdesc2", "munits2", ArrayType.STRUCTURE, new int[] {7})
        .setStructureMembers(nestedMembers.toBuilder());
    builder.addMember("mseq", "mdesc2", "munits2", ArrayType.SEQUENCE, new int[0])
        .setStructureMembers(nestedMembers.toBuilder());
    StructureMembers members = builder.setStandardOffsets().build();
    System.out.printf("top members = %s%n", members.showOffsets());

    System.out.printf("bb elems = %d size = %s%n", nelems, members.getStorageSizeBytes());
    byte[] result = new byte[(int) (nelems * members.getStorageSizeBytes())];
    ByteBuffer bb = ByteBuffer.wrap(result);
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, nelems);
    for (int recno = 0; recno < nelems; recno++) {
      int recstart = recno * members.getStorageSizeBytes();
      for (StructureMembers.Member m : members) {
        int pos = recstart + m.getOffset();
        switch (m.getArrayType()) {
          case STRING:
            String[] data = new String[m.length()];
            for (int idx = 0; idx < m.length(); idx++) {
              data[idx] = "Gimme Shelter" + idx + "-" + recno;
            }
            bb.putInt(pos, storage.putOnHeap(data));
            break;
          case OPAQUE:
            int[] shape = new int[] {1, 2, -1};
            short[] arr1 = new short[] {1, 2, 3, 4, 5};
            short[] arr2 = new short[] {6, 7};
            short[][] ragged = new short[][] {arr1, arr2};
            ArrayVlen<Short> vlen = ArrayVlen.factory(ArrayType.SHORT, shape, ragged);
            bb.putInt(pos, storage.putOnHeap(vlen));
            break;
          case SEQUENCE:
            Array<?> seq = makeNestedSequence(11, nestedMembers);
            bb.putInt(pos, storage.putOnHeap(seq));
            break;
          case STRUCTURE:
            makeNestedStructureArray(storage, bb, pos, m.length(), nestedMembers);
            break;
        }
      }
    }
    return new StructureDataArray(members, new int[] {nelems}, storage);
  }

  private void makeNestedStructureArray(StructureDataStorageBB storage, ByteBuffer bb, int start, int nelems,
      StructureMembers members) {
    System.out.printf("nested start = %d end = %s%n", start, start + nelems * members.getStorageSizeBytes());
    if (start + nelems * members.getStorageSizeBytes() > bb.limit()) {
      System.out.printf("HEY");
    }
    for (int recno = 0; recno < nelems; recno++) {
      int recstart = start + recno * members.getStorageSizeBytes();
      for (StructureMembers.Member m : members) {
        int pos = recstart + m.getOffset();
        bb.position(pos);

        switch (m.getArrayType()) {
          case BYTE:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.put((byte) (idx + 100));
            }
            break;
          case DOUBLE:
            for (int idx = 0; idx < m.length(); idx++) {
              bb.putDouble((idx + 3.14));
            }
            break;
          case STRING:
            String[] data = new String[m.length()];
            for (int idx = 0; idx < m.length(); idx++) {
              data[idx] = "Or else" + idx + "-" + recno;
            }
            bb.putInt(storage.putOnHeap(data));
            break;
        }
      }
    }
  }

  private StructureDataArray makeNestedSequence(int nelems, StructureMembers members) {
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
          case STRING:
            String[] data = new String[m.length()];
            for (int idx = 0; idx < m.length(); idx++) {
              data[idx] = "Or else fade away" + idx + "-" + recno;
            }
            bb.putInt(pos, storage.putOnHeap(data));
            break;
        }
      }
    }
    return new StructureDataArray(members, new int[] {nelems}, storage);
  }

  private static StructureDataArray copyStructureArray(StructureDataArray org) {
    StructureMembers members = org.getStructureMembers().toBuilder().setStructuresOnHeap(true).build();

    int nelems = (int) org.length();
    byte[] result = new byte[(int) (nelems * members.getStorageSizeBytes())];
    ByteBuffer bb = ByteBuffer.wrap(result);
    StructureDataStorageBB storage = new StructureDataStorageBB(members, bb, nelems);
    for (int recno = 0; recno < nelems; recno++) {
      int recstart = recno * members.getStorageSizeBytes();
      for (StructureMembers.Member m : members) {
        Array mdata = org.get(recno).getMemberData(m);
        if (mdata instanceof StructureDataArray) {
          mdata = org.get(recno).getMemberData(m);
          mdata = copyStructureArray((StructureDataArray) mdata);
        }
        storage.setMemberData(recstart, m, mdata);
      }
    }
    return new StructureDataArray(members, new int[] {nelems}, storage);
  }

}
