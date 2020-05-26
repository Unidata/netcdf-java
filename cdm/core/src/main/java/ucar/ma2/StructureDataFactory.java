/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ma2;

import java.util.Arrays;

/**
 * Convenience routines for constructing one-off StructureData objects
 *
 * @author caron
 * @since Jan 19, 2009
 */
public class StructureDataFactory {

  /** Make a StructureData with a single member of given name and value. */
  public static StructureData make(String name, Object value) {
    StructureMembers.Builder builder = StructureMembers.builder();
    DataType dtype = DataType.getType(value.getClass(), false); // LOOK unsigned
    builder.addMember(name, null, null, dtype, new int[] {1});
    StructureMembers sm = builder.build();
    StructureDataW sw = new StructureDataW(sm);
    Array dataArray = Array.factory(dtype, new int[] {1});
    dataArray.setObject(dataArray.getIndex(), value);
    sw.setMemberData(sm.findMember(name), dataArray);
    return sw;
  }

  public static StructureData make(StructureData... sdatas) {
    if (sdatas.length == 1)
      return sdatas[0];

    // look for sole
    int count = 0;
    StructureData result = null;
    for (StructureData sdata : sdatas) {
      if (sdata != null) {
        count++;
        result = sdata;
      }
    }
    if (count == 1)
      return result;

    // combine
    return StructureDataComposite.create(Arrays.asList(sdatas));
  }

  public static StructureData make(Iterable<StructureData> sdatas) {
    // look for singleton
    int count = 0;
    StructureData result = null;
    for (StructureData sdata : sdatas) {
      if (sdata != null) {
        count++;
        result = sdata;
      }
    }
    if (count == 1)
      return result;

    // if multiple, combine into StructureDataComposite
    return StructureDataComposite.create(sdatas);
  }


}
