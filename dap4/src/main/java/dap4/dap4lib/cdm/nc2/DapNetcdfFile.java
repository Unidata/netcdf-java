/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib.cdm.nc2;

import dap4.core.dmr.*;
import dap4.core.util.*;
import dap4.dap4lib.*;
import dap4.dap4lib.cdm.CDMUtil;
import dap4.dap4lib.cdm.NodeMap;
import ucar.ma2.*;
import ucar.nc2.CDMNode;
import ucar.nc2.NetcdfFile;
import ucar.nc2.ParsedSectionSpec;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.iosp.IospHelper;
import ucar.nc2.util.CancelTask;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.WritableByteChannel;
import java.util.*;


/**
 * This class is the work-horse of the client side.
 * It uses a D4DSP instance to obtain access to a DMR and
 * (optionally) a compiled DAP4 data stream.
 *
 * Given that, it constructs a translation to CDM.
 */

public class DapNetcdfFile extends NetcdfFile {

  static final boolean DEBUG = false;
  static final boolean PARSEDEBUG = false;
  static final boolean MERGE = false;

  // NetcdfDataset enhancement to use: need only coord systems
  static Set<NetcdfDataset.Enhance> ENHANCEMENT = EnumSet.of(NetcdfDataset.Enhance.CoordSystems);

  //////////////////////////////////////////////////
  // Constants

  //////////////////////////////////////////////////
  // Type Declarations

  protected static class NullCancelTask implements CancelTask {
    public boolean isCancel() {
      return false;
    }

    public boolean isDone() {
      return false;
    }

    public void setDone(boolean done) {}

    public void setError(String msg) {}

    public void setProgress(String msg, int progress) {}
  }

  //////////////////////////////////////////////////
  // Static variables

  protected static final NullCancelTask nullcancel = new NullCancelTask();

  /**
   * Define a map of known DSP classes.
   */
  protected static DSPRegistry dspregistry = new DSPRegistry();

  static {
    dspregistry.register(RawDSP.class, DSPRegistry.FIRST);
    dspregistry.register(HttpDSP.class, DSPRegistry.FIRST);
  }

  //////////////////////////////////////////////////
  // Instance Variables

  protected boolean allowCompression = true;
  protected boolean closed = false;
  protected String location = null; // original argument passed to open
  protected CancelTask cancel = null;

  protected String dsplocation = null; // what is passed to DSP
  protected XURI xuri = null;

  protected DapContext cxt = null;
  protected D4DSP dsp = null;
  // Extractions from dsp
  protected DapDataset dmr = null;

  protected CDMCompiler cdmCompiler = null;

  protected ChecksumMode checksummode = null;

  protected boolean daploaded = false; // avoid multiple loads

  // Variable map
  protected Map<Variable, Array> arraymap = null;

  /////////////////////////////////////////////////
  // Constructor(s)

  /**
   * Open a Dap4 connection or file via a D4DSP.
   * Warning: we do not use a Builder because this object is mutable over time.
   *
   * @param location URL for the request. Note that if this is
   *        intended to send to a file oriented
   *        DSP, then if must be converted to an absolute path.
   *        Note also that the URL path should not have any .dap or .dmr
   *        extension since using those is the purview of this class.
   * @param cancelTask check if task is cancelled; may be null.
   * @throws IOException
   */
  public DapNetcdfFile(String location, CancelTask cancelTask) throws IOException {
    super();
    this.location = location;
    // Figure out the location: url vs path
    XURI xuri;
    try {
      xuri = new XURI(location);
    } catch (URISyntaxException use) {
      throw new IOException(use);
    }
    this.dsplocation = xuri.assemble(XURI.URLQUERY);
    cancel = (cancelTask == null ? nullcancel : cancelTask);

    // The DapContext object is the primary means of passing information
    // between various parts of the DAP4 system.
    this.cxt = new DapContext();
    // Insert fragment as (key,value) pairs into the context
    cxt.insert(xuri.getFragFields(), true);
    // Query takes precedence over fragment
    cxt.insert(xuri.getQueryFields(), true);
    String csummode = (String) cxt.get(DapConstants.CHECKSUMTAG);
    this.checksummode = ChecksumMode.modeFor(csummode);
    this.checksummode = ChecksumMode.asTrueFalse(this.checksummode); // Fix the checksum mode to be only TRUE or FALSE
    this.cxt.put(ChecksumMode.class, this.checksummode);

    // Find the D4DSP class that can process this URL location.
    this.dsp = dspregistry.findMatchingDSP(this.location, cxt); // find relevant D4DSP subclass
    if (this.dsp == null)
      throw new IOException("No matching DSP: " + this.location);
    this.dsp.open(this.dsplocation, this.checksummode); // side effect: get DMR
    ensuredmr();
    this.dmr = this.dsp.getDMR(); // get DMR
    this.cxt.put(DapDataset.class, this.dmr);
    // set the pseudo-location, otherwise we get a name that is full path.
    setLocation(this.dmr.getDataset().getShortName());
    finish();
    this.dsp.loadContext(this.cxt, RequestMode.DMR);
  }

  /**
   * Open a Dap4 connection
   *
   * @param url URL for the request.
   * @throws IOException
   */

  public DapNetcdfFile(String url) throws IOException {
    this(url, null);
  }

  //////////////////////////////////////////////////
  // Close

  /**
   * Close all resources (files, sockets, etc) associated with this file.
   *
   * @throws java.io.IOException if error when closing
   */
  @Override
  public synchronized void close() throws java.io.IOException {
    if (closed)
      return;
    closed = true; // avoid circular calls
    dsp = null;
  }

  //////////////////////////////////////////////////
  // Accessors

  /**
   * @return true if we can ask the server to do constraint processing
   */
  public boolean isconstrainable() {
    return true;
  }

  public String getLocation() {
    return location;
  }

  public D4DSP getDSP() {
    return this.dsp;
  }

  //////////////////////////////////////////////////
  // Override NetcdfFile.readXXX Methods

  /**
   * Do a bulk read on a list of Variables and
   * return a corresponding list of Array that contains the results
   * of a full read on each Variable.
   * TODO: optimize to make only a single server call and cache the results.
   *
   * @param variables List of type Variable
   * @return List of Array, one for each Variable in the input.
   * @throws IOException if read error
   */

  @Override
  public List<Array> readArrays(List<Variable> variables) throws IOException {
    List<Array> result = new ArrayList<Array>();
    for (Variable variable : variables) {
      result.add(variable.read());
    }
    return result;
  }

  /**
   * Read databuffer from a top level Variable
   * and send databuffer to a WritableByteChannel.
   * Experimental.
   *
   * @param v a top-level Variable
   * @param section the section of databuffer to read.
   *        There must be a Range for each Dimension in the variable,
   *        in order.
   *        Note: no nulls allowed. IOSP may not modify.
   * @param channel write databuffer to this WritableByteChannel
   * @return the number of databuffer written to the channel
   * @throws java.io.IOException if read error
   * @throws ucar.ma2.InvalidRangeException if invalid section
   */

  @Override
  public long readToByteChannel(Variable v, Section section, WritableByteChannel channel)
      throws java.io.IOException, ucar.ma2.InvalidRangeException {
    Array result = readData(v, section);
    return IospHelper.transferData(result, channel);
  }

  public Array readSection(String variableSection) throws IOException, InvalidRangeException {
    ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(this, variableSection);
    return cer.v.read(cer.section);
  }

  /**
   * b * Primary read entry point.
   * This is the primary implementor of Variable.read.
   *
   * @param cdmvar A top-level variable
   * @param section the section of databuffer to read.
   *        There must be a Range for each Dimension in the variable,
   *        in order. Note: no nulls allowed.
   * @return An Array object for accessing the databuffer
   * @throws IOException if read error
   * @throws InvalidRangeException if invalid section
   */

  @Override
  protected Array readData(Variable cdmvar, Section section) throws IOException, InvalidRangeException {
    // The section is applied wrt to the DataDMR, so it
    // takes into account any constraint used in forming the dataDMR.
    // We use the Section to produce a view of the underlying variable array.

    // Read and compile the DAP4 data
    // Ensure that the DSP has data
    ensuredata();
    Array result = arraymap.get(cdmvar);
    if (result == null)
      throw new IOException("No data for variable: " + cdmvar.getFullName());
    if (section != null) {
      if (cdmvar.getRank() != section.getRank())
        throw new InvalidRangeException(String.format("Section rank != %s rank", cdmvar.getFullName()));
      List<Range> ranges = section.getRanges();
      // Case out the possibilities
      if (CDMUtil.hasVLEN(ranges)) {
        ranges = ranges.subList(0, ranges.size() - 1);// may produce empty list
      }
      if (ranges.size() > 0 && !CDMUtil.isWhole(ranges, cdmvar))
        result = result.sectionNoReduce(ranges);
    }
    return result;
  }

  protected void loadContext() {
    this.cxt.put(DapConstants.ChecksumSource.REMOTE, this.dsp.getChecksumMap(DapConstants.ChecksumSource.REMOTE));
    this.cxt.put(DapConstants.ChecksumSource.LOCAL, this.dsp.getChecksumMap(DapConstants.ChecksumSource.LOCAL));
    this.cxt.put(D4Array.class, this.dsp.getVariableDataMap());
  }

  protected void verifyChecksums() throws DapException {
    ChecksumMode cmode = (ChecksumMode) this.cxt.get(ChecksumMode.class);
    Map<DapVariable, Long> remotechecksummap = (Map<DapVariable, Long>) cxt.get(DapConstants.ChecksumSource.REMOTE);
    Map<DapVariable, Long> localchecksummap = (Map<DapVariable, Long>) cxt.get(DapConstants.ChecksumSource.LOCAL);

    if (cmode != ChecksumMode.TRUE)
      return;
    for (DapVariable dvar : dmr.getTopVariables()) {
      // Verify the calculated checksums
      Long remotechecksum = remotechecksummap.get(dvar);
      Long localchecksum = localchecksummap.get(dvar);
      assert ((localchecksum != null) && (remotechecksum != null));
      if (!cxt.containsKey("hyrax")) {// Suppress the check for Hyrax, for now
        if (localchecksum.longValue() != remotechecksum.longValue())
          throw new DapException("Checksum mismatch: local=" + localchecksum + " remote=" + remotechecksum);
      }
      // Verify the checksum Attribute, if any
      DapAttribute csumattr = dvar.getChecksumAttribute();
      if (csumattr != null) {
        assert (csumattr.getValues().length == 1 && csumattr.getBaseType() == DapType.INT32);
        Long attrcsum = (long) 0;
        try {
          attrcsum = Long.parseLong(csumattr.getValues()[0]);
        } catch (NumberFormatException nfe) {
          throw new DapException("Illegal Checksum attribute value", nfe);
        }
        if (!cxt.containsKey("hyrax")) { // Suppress the check for Hyrax, for now
          if (localchecksum.longValue() != attrcsum.longValue())
            throw new DapException("Checksum mismatch: local=" + localchecksum + " attribute=" + attrcsum);
        }
      }
    }
  }

  /**
   * Do what is necessary to ensure that DMR and DAP compilation will work
   */
  public void ensuredmr() throws IOException {
    if (this.dmr == null) { // do not call twice
      this.dsp.loadDMR();
      this.dmr = this.dsp.getDMR();
      if (this.cdmCompiler == null)
        this.cdmCompiler = new CDMCompiler(this, this.dsp);
      this.cdmCompiler.compileDMR();
    }
  }

  public void ensuredata() throws IOException {
    if (!this.daploaded) { // do not call twice
      this.daploaded = true;
      this.dsp.loadDAP();
      loadContext();
      verifyChecksums();
      this.dsp.loadContext(this.cxt, RequestMode.DAP);
      if (this.cdmCompiler == null)
        this.cdmCompiler = new CDMCompiler(this, this.dsp);
      this.cdmCompiler.compileData();
      // Prepare the array map
      assert this.arraymap == null;
      this.arraymap = new HashMap<Variable, Array>();
      Map<DapVariable, D4Array> datamap = this.dsp.getVariableDataMap();
      NodeMap<CDMNode, DapNode> nodemap = this.cdmCompiler.getNodeMap();
      for (Map.Entry<DapVariable, D4Array> entry : datamap.entrySet()) {
        DapVariable dv = entry.getKey();
        D4Array dc = entry.getValue();
        Variable v = (Variable) nodemap.get(entry.getKey());
        assert (dc.getArray() != null);
        arraymap.put(v, dc.getArray());
      }
    }
  }
}
