/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dods;

import ucar.ma2.*;
import java.util.*;
import ucar.unidata.util.StringUtil2;

/**
 * Adapter for dods.dap.Attribute into a ucar.nc2.Attribute.
 * Byte attributes are widened to short because DODS has Bytes as unsigned,
 * but in Java they are signed.
 */
class DODSAttribute extends ucar.nc2.Attribute {

  static DODSAttribute create(String rawName, opendap.dap.Attribute att) {
    // LOOK dont know if attribute is unsigned byte
    DataType ncType = DODSNetcdfFile.convertToNCType(att.getType(), false);

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

    String name = DODSNetcdfFile.makeShortName(rawName);
    String dodsName = DODSNetcdfFile.makeDODSName(rawName);
    return new DODSAttribute(name, data, dodsName);
  }

  private DODSAttribute(String name, Array values, String dodsName) {
    super(name, values); // LOOK do we really need DODSAttribute?
    this.dodsName = dodsName;
  }

  DODSAttribute(String dodsName, String val) {
    super(DODSNetcdfFile.makeShortName(dodsName), val);
    this.dodsName = DODSNetcdfFile.makeDODSName(dodsName);
  }

  private static String[] escapeAttributeStrings = {"\\", "\""};
  private static String[] substAttributeStrings = {"\\\\", "\\\""};

  private String unescapeAttributeStringValues(String value) {
    return StringUtil2.substitute(value, substAttributeStrings, escapeAttributeStrings);
  }

  //////////////////////////////////////////////////
  // DODSNode Interface
  private String dodsName = null;

  public String getDODSName() {
    if (dodsName == null)
      return getShortName();
    else
      return this.dodsName;
  }

}
