/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDString.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import com.google.common.base.Preconditions;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;
import thredds.server.opendap.servers.SDString;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/** Wraps a netcdf scalar or 1D char variable */
public class NcSDString extends SDString implements HasNetcdfVariable {
  private final Variable ncVar;
  private String localVal;

  NcSDString(Variable v) {
    super(v.getShortName());
    this.ncVar = v;
  }

  NcSDString(String name, String val) {
    super(name);
    this.localVal = val;
    this.ncVar = null;
    if (val != null) {
      setValue(val);
    }
  }

  /** Read the value (parameters are ignored). */
  public boolean read(String datasetName, Object specialO) throws IOException {
    if (localVal == null) { // read first time
      setData(ncVar.read());
    }
    setValue(localVal);
    setRead(true);
    return (false);
  }

  public void setData(Array data) {
    Preconditions.checkNotNull(ncVar);
    if (ncVar.getDataType() == DataType.STRING) {
      localVal = (String) data.getObject(data.getIndex());

    } else { // gotta be a CHAR

      if (ncVar.getRank() == 0) {
        // scalar char - convert to a String
        ArrayChar a = (ArrayChar) data;
        byte[] b = new byte[1];
        b[0] = (byte) a.getChar(0);
        localVal = new String(b, StandardCharsets.UTF_8);
      } else {
        // 1D
        ArrayChar a = (ArrayChar) data;
        localVal = a.getString(a.getIndex()); // fetches the entire String
      }
    }

    setValue(localVal);
    setRead(true);
  }

  @Nullable
  public Variable getVariable() {
    return ncVar;
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    localVal = sdata.getScalarString(m);
    setValue(localVal);
    externalize(sink);
  }
}
