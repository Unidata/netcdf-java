/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: HasNetcdfVariable.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import java.io.DataOutputStream;
import java.io.IOException;
import ucar.ma2.Array;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Variable;

/** An Object that has a netcdf variable, and the data can be set externally by an Array. */
public interface HasNetcdfVariable {

  /** reset the underlying proxy */
  void setData(Array data);

  /** get the underlying proxy */
  Variable getVariable();

  // for structure members
  void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException;
}
