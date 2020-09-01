/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDStructure.java 51 2006-07-12 17:13:13Z caron $

package thredds.server.opendap;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import opendap.dap.BaseType;
import opendap.dap.NoSuchVariableException;
import thredds.server.opendap.servers.CEEvaluator;
import thredds.server.opendap.servers.DAP2ServerSideException;
import thredds.server.opendap.servers.SDStructure;
import thredds.server.opendap.servers.ServerMethods;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.Structure;
import ucar.nc2.Variable;

/**
 * Wraps a netcdf Structure, as an SDStructure.2
 */
public class NcSDStructure extends SDStructure {
  private Structure ncVar = null;
  protected List<BaseType> memberBTlist;

  protected NcSDStructure org;
  protected StructureData sdata;

  /**
   * Constructor.
   * 
   * @param s the netcdf Structure
   * @param list of the member variables
   */
  public NcSDStructure(Structure s, List<BaseType> list) {
    super(s.getShortName());
    this.ncVar = s;

    for (BaseType aList : list) {
      addVariable(aList, 0);
    }
    memberBTlist = list;
  }

  public NcSDStructure(NcSDStructure org, StructureData sdata) {
    super(org.getEncodedName());
    this.org = org;
    this.sdata = sdata;
  }

  public Variable getVariable() {
    return ncVar;
  }

  // called if its scalar
  public boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException {

    // read the scalar structure into memory
    StructureData sdata;
    try {
      sdata = ncVar.readStructure(0);
    } catch (InvalidRangeException e) {
      throw new IOException(e);
    }
    setData(sdata);
    return (false);
  }

  // LOOK - should modify to use hasNetcdf.setData( StructureData) for efficiency
  public void setData(StructureData sdata) {
    int count = 0;

    StructureMembers sm = sdata.getStructureMembers();
    for (BaseType bt : org.getVariables()) {
      // loop through both structures
      HasNetcdfVariable hasNetcdf = (HasNetcdfVariable) bt;
      StructureMembers.Member m = sm.getMember(count++);

      // extract the data and set it into the dods object
      Array data = sdata.getArray(m);
      hasNetcdf.setData(data);
    }

    setRead(true);
  }

  ////////////////////////////////////////////////////////////////////////////////
  // overrride for array of Structures
  public void serialize(String dataset, DataOutputStream sink, CEEvaluator ce, Object specialO)
      throws NoSuchVariableException, DAP2ServerSideException, IOException {

    if (org == null) {
      super.serialize(dataset, sink, ce, specialO);
      return;
    }

    // use the projection info in the original
    // run through each structure member
    StructureMembers sm = sdata.getStructureMembers();

    int count = 0;
    for (BaseType bt : org.getVariables()) {
      HasNetcdfVariable sm_org = (HasNetcdfVariable) bt;
      boolean isProjected = ((ServerMethods) sm_org).isProject();
      if (isProjected) {
        StructureMembers.Member m = sm.getMember(count);
        sm_org.serialize(sink, sdata, m);
      }
      count++;
    }

  }
}
