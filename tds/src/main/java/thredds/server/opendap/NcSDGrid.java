/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDGrid.java 51 2006-07-12 17:13:13Z caron $

package thredds.server.opendap;

import java.io.IOException;
import java.util.List;
import opendap.dap.BaseType;
import opendap.dap.DGrid;
import opendap.dap.NoSuchVariableException;
import thredds.server.opendap.servers.SDArray;
import thredds.server.opendap.servers.SDGrid;

/** Wraps a netcdf variable with rank > 0, whose dimensions all have coordinate variables, as an SDGrid. */
public class NcSDGrid extends SDGrid {

  /**
   * Constructor.
   * 
   * @param name of the Grid
   * @param list of the variables, first data then maps
   */
  public NcSDGrid(String name, List<BaseType> list) {
    super(name);
    addVariable(list.get(0), DGrid.ARRAY);

    for (int i = 1; i < list.size(); i++) {
      addVariable(list.get(i), DGrid.MAPS);
    }
  }

  public boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException {
    for (BaseType bt : getVariables()) {
      SDArray array = (SDArray) bt;
      array.read(datasetName, specialO);
    }

    setRead(true);
    return (false);
  }
}
