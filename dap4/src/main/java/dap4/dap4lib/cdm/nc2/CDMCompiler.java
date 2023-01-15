/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */


package dap4.dap4lib.cdm.nc2;

import dap4.core.dmr.DapVariable;
import dap4.core.util.DapContext;
import dap4.dap4lib.D4Cursor;
import dap4.dap4lib.D4DSP;
import dap4.dap4lib.cdm.NodeMap;
import dap4.core.dmr.DapDataset;
import dap4.core.dmr.DapNode;
import dap4.core.util.DapException;
import ucar.ma2.Array;
import ucar.nc2.CDMNode;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import java.util.Map;

/**
 * The goal for the CDM compiler is produce a NetcdfDataset
 * whose content comes from a DSP. In a sense it is the
 * inverse of CDMDSP.
 *
 * Compilation implies two translations/wraps.
 * 1. Create a set of CDMNodes corresponding to the
 * relevant nodes in the DMR.
 * 2. Create a set of CDM ucar.ma2.array objects that wrap the
 * DataDataset object.
 * Note that this compiler has nothing to do with D4DataCompiler.
 */

public class CDMCompiler {

  public static boolean DEBUG = false;

  /*
   * ////////////////////////////////////////////////
   * Constants
   *
   * ////////////////////////////////////////////////
   * Instance variables
   */

  protected DapNetcdfFile ncfile = null;
  protected D4DSP dsp = null;
  protected DapDataset dmr = null;
  protected NodeMap<CDMNode, DapNode> nodemap = null; // Bi-directional
  protected Map<Variable, Array> arraymap = null;

  //////////////////////////////////////////////////
  // Constructor(s)

  /**
   * Constructor
   *
   * @param ncfile the target NetcdfDataset (as yet empty)
   * @param dsp
   */

  public CDMCompiler(DapNetcdfFile ncfile, D4DSP dsp) throws DapException {
    this.ncfile = ncfile;
    this.dsp = dsp;
    this.dmr = this.dsp.getDMR();
  }

  //////////////////////////////////////////////////
  // Accessors

  public NodeMap<CDMNode, DapNode> getNodeMap() {
    return this.nodemap;
  }

  public Map<Variable, Array> getArrayMap() {
    return this.arraymap;
  }

  public NetcdfFile getNetcdfFile() {
    return this.ncfile;
  }

  //////////////////////////////////////////////////
  // Compile DMR->set of CDM nodes

  /**
   * Convert a DMR to equivalent CDM meta-databuffer
   * and populate a NetcdfDataset with it.
   *
   * @throws DapException
   */

  public void compileDMR() throws DapException {
    // Convert the DMR to CDM metadata
    // and return a mapping from DapNode -> CDMNode
    this.nodemap = new DMRToCDM(this.ncfile, this.dsp).create();
  }

  //////////////////////////////////////////////////
  // Compile Data->set of CDM Array objects

  /**
   * Convert a DataDataset to equivalent CDM data (Array objects).
   * and populate a NetcdfDataset with it.
   *
   * @throws DapException
   */

  public void compileData() throws DapException {
    // Convert the Data to CDM arrays
    // and return a mapping from Variable -> Array
    this.arraymap = DataToCDM.createDataMap(this.dsp, this.nodemap);
  }

}
