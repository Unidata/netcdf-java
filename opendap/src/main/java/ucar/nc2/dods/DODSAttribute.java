/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import java.util.*;

/**
 * Adapter for dods.dap.Attribute into a ucar.nc2.Attribute.
 * Byte attributes are widened to short because DODS has Bytes as unsigned,
 * but in Java they are signed.
 */
class DODSAttribute {

  static ucar.nc2.Attribute create(String rawName, opendap.dap.Attribute att) {
    // LOOK dont know if attribute is unsigned byte
    DataType ncType = DodsNetcdfFiles.convertToNCType(att.getType(), false);

    // count number
    int nvals = 0;
    Iterator iter = att.getValuesIterator();
    while (iter.hasNext()) {
      iter.next();
      nvals++;
    }

    // need String[]
    String[] vals = new String[nvals];
    iter = att.getValuesIterator();
    int count = 0;
    while (iter.hasNext()) {
      vals[count++] = (String) iter.next();
    }

    Array data;
    if (ncType == DataType.STRING)
      data = Array.factory(ncType, new int[] {nvals}, vals);
    else {
      try {
        // create an Array of the correct type
        data = Array.factory(ncType, new int[] {nvals});
        Index ima = data.getIndex();
        for (int i = 0; i < nvals; i++) {
          double dval = Double.parseDouble(vals[i]);
          data.setDouble(ima.set(i), dval);
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Illegal Numeric Value for Attribute Value for " + rawName);
      }
    }

    String name = DodsNetcdfFiles.makeShortName(rawName);
    String dodsName = DodsNetcdfFiles.makeDODSName(rawName); // LOOK
    return ucar.nc2.Attribute.builder(name).setValues(data).build();
  }

}
