/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.array;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.ma2.DataType;

/** Test {@link StructureDataArray} */
public class TestStructureDataArray {

  @Test
  public void testBasics() {
    StructureMembers.Builder builder = StructureMembers.builder();
    builder.setName("name");
    builder.addMember("mname1", "mdesc1", "munits1", DataType.BYTE, new int[] {11, 11});
    builder.addMember("mname2", "mdesc2", "munits1", DataType.FLOAT, new int[] {});
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

}
