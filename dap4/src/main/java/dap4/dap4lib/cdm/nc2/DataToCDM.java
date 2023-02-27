/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.dap4lib.cdm.nc2;

import dap4.dap4lib.D4Array;
import dap4.dap4lib.D4DSP;
import dap4.dap4lib.cdm.NodeMap;
import dap4.core.dmr.*;
import dap4.core.util.*;
import ucar.ma2.Array;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Associate the Array objects created in D4DataCompiler
 * with the corresponding CDM Variable.
 * Class is not intended to be instantiated
 * Note: this class is rather short, so could be eliminated.
 */

abstract public class DataToCDM {

  public static boolean DEBUG = false;

  //////////////////////////////////////////////////
  // Constants

  protected static final int COUNTSIZE = 8; // databuffer as specified by the DAP4 spec

  protected static final String LBRACE = "{";
  protected static final String RBRACE = "}";

  //////////////////////////////////////////////////
  // Instance variables

  protected DapNetcdfFile ncfile = null;
  protected D4DSP dsp = null;
  // Extractions from dsp
  protected DapDataset dmr = null;
  protected ChecksumMode checksummode = null;
  protected Map<DapVariable, Long> localchecksummap = null;

  protected Group cdmroot = null;
  protected Map<DapVariable, D4Array> datamap = null;
  protected NodeMap nodemap = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  //////////////////////////////////////////////////
  // Correlate CDM Variables with ucar.ma2.Array objects

  static public Map<Variable, Array> createDataMap(D4DSP dsp, NodeMap nodemap) throws DapException {
    Map<DapVariable, D4Array> datamap = dsp.getVariableDataMap();
    DapDataset dmr = dsp.getDMR();
    Map<Variable, Array> arraymap = new HashMap<>();
    // iterate over the variables represented in the nodemap
    List<DapVariable> topvars = dmr.getTopVariables();
    Map<Variable, Array> map = null;
    for (DapVariable var : topvars) {
      D4Array cursor = datamap.get(var);
      Variable v = (Variable) nodemap.get(var);
      assert cursor != null && v != null;
      Array array = cursor.getArray();
      assert array != null;
      arraymap.put(v, array);
    }
    return arraymap;
  }
}
