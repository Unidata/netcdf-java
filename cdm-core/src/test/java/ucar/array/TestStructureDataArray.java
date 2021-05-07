/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;

import java.nio.ByteBuffer;

/** Test {@link StructureDataArray} */
public class TestStructureDataArray {

  @Test
  public void testBasics() {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("name");
    builder.addMember("mname1", "mdesc1", "munits1", ArrayType.BYTE, new int[] {11, 11});
    builder.addMember("mname2", "mdesc2", "munits1", ArrayType.FLOAT, new int[] {});
    StructureMembers members = builder.build();

    StructureData[] parr = new StructureData[2];
    parr[0] = new StructureDataRow(members);
    parr[1] = new StructureDataRow(members);
    StructureDataArray array = new StructureDataArray(members, new int[] {2}, parr);

    assertThat(array.get(0)).isEqualTo(parr[0]);
    assertThat(array.get(1)).isEqualTo(parr[1]);

    int count = 0;
    for (StructureData val : array) {
      assertThat(val).isEqualTo(parr[count]);
      count++;
    }

    // Note that this does fail
    try {
      assertThat(array.get(0, 2, 2)).isEqualTo(8);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    try {
      assertThat(array.get(0, 1)).isEqualTo(4);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class);
    }

    assertThat(array.getStructureMembers()).isEqualTo(members);
    assertThat(array.getStructureMemberNames()).isEqualTo(ImmutableList.of("mname1", "mname2"));
    assertThat(array.getStructureSize()).isEqualTo(121 + 4);
    assertThat(array.storage().length()).isEqualTo(2);

    StructureData sdata = array.get(0);
    assertThat(sdata.getStructureMembers()).isEqualTo(members);
    assertThat(sdata.getName()).isEqualTo("name");
  }

  /** Experimental, used in Cdmr */
  class StructureDataRow extends StructureData {
    private final ByteBuffer bbuffer;

    public StructureDataRow(StructureMembers members) {
      super(members);
      this.bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
    }

    public Array getMemberData(StructureMembers.Member m) {
      ArrayType dataType = m.getArrayType();
      int offset = m.getOffset();
      int size = m.length();

      switch (dataType) {
        case DOUBLE:
          double[] darray = new double[size];
          for (int count = 0; count < size; count++) {
            darray[count] = bbuffer.getDouble(offset + 8 * count);
          }
          return new ArrayDouble(m.getShape(), new ArrayDouble.StorageD(darray));
        case FLOAT:
          float[] farray = new float[size];
          for (int count = 0; count < size; count++) {
            farray[count] = bbuffer.getFloat(offset + 4 * count);
          }
          return new ArrayFloat(m.getShape(), new ArrayFloat.StorageF(farray));
        default:
          throw new RuntimeException("unknown dataType " + dataType);
      }
    }

    public void setMemberData(StructureMembers.Member m, Array<?> data) {
      Preconditions.checkArgument(data.length() == m.length());
      ArrayType dataType = m.getArrayType();
      int offset = m.getOffset();
      int count = 0;

      switch (dataType) {
        case DOUBLE:
          Array<Double> ddata = (Array<Double>) data;
          for (double val : ddata) {
            bbuffer.putDouble(offset + 8 * count, val);
            count++;
          }
          break;
        case FLOAT:
          Array<Float> fdata = (Array<Float>) data;
          for (float val : fdata) {
            bbuffer.putFloat(offset + 4 * count, val);
            count++;
          }
          break;
        default:
          throw new RuntimeException("unknown dataType " + dataType);
      }
    }
  }

}
