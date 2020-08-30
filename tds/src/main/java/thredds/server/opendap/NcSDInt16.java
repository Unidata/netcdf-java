/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDInt16.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import java.io.DataOutputStream;
import java.io.IOException;
import thredds.server.opendap.servers.SDInt16;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/**
 * Wraps a netcdf scalar short variable.
 *
 * @author jcaron
 */
public class NcSDInt16 extends SDInt16 implements HasNetcdfVariable {
  private Variable ncVar;

  /**
   * Constructor
   *
   * @param v : the netcdf Variable
   */
  NcSDInt16(Variable v) {
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
    setValue(data.getShort(data.getIndex()));
    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setValue(sdata.getScalarShort(m));
    externalize(sink);
  }
}
