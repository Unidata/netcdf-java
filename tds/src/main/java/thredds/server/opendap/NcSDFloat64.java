/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDFloat64.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import java.io.DataOutputStream;
import java.io.IOException;
import thredds.server.opendap.servers.SDFloat64;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/**
 * Wraps a netcdf scalar double variable.
 *
 * @author jcaron
 */
public class NcSDFloat64 extends SDFloat64 implements HasNetcdfVariable {
  private Variable ncVar;

  /**
   * Constructor
   *
   * @param v : the netcdf Variable
   */
  NcSDFloat64(Variable v) {
    super(v.getShortName());
    this.ncVar = v;
  }

  public Variable getVariable() {
    return ncVar;
  }

  /**
   * Read the value (parameters are ignored).
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    setData(ncVar.read());
    return (false);
  }

  public void setData(Array data) {
    setValue(data.getDouble(data.getIndex()));
    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setValue(sdata.getScalarDouble(m));
    externalize(sink);
  }
}
