/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDUInt32.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import java.io.DataOutputStream;
import java.io.IOException;
import thredds.server.opendap.servers.SDUInt32;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/** Wraps a netcdf scalar (unsigned) int variable. */
public class NcSDUInt32 extends SDUInt32 implements HasNetcdfVariable {
  private final Variable ncVar;

  NcSDUInt32(Variable v) {
    super(v.getShortName());
    this.ncVar = v;
  }

  /** Read the value (parameters are ignored). */
  public boolean read(String datasetName, Object specialO) throws IOException {
    setData(ncVar.read());
    return (false);
  }

  public void setData(Array data) {
    setValue(data.getInt(data.getIndex()));
    setRead(true);
  }

  public Variable getVariable() {
    return ncVar;
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setValue(sdata.getScalarInt(m));
    externalize(sink);
  }

}
