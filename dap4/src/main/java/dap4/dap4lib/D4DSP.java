/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.dmr.*;
import dap4.core.dmr.parser.DOM4Parser;
import dap4.core.dmr.parser.Dap4Parser;
import dap4.core.util.ChecksumMode;
import dap4.core.util.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dap4.core.util.DapConstants.*;

/**
 * This Class takes a DAP4 serialization (as chunked input) stream
 * and exports access to a compiled DMR, a compiled DAP4 data stream,
 * and various parameters such as byteorder of the data stream.
 *
 * Most of the work is done by this class. Its subclasses need
 * only provide a raw byte stream to DAP4 chunked data taken
 * from e.g. a raw data file or an HTTP connection.
 *
 * It cannot be used standalone.
 */

public abstract class D4DSP {

  //////////////////////////////////////////////////
  // Constants

  public static boolean DEBUG = false;
  protected static final boolean PARSEDEBUG = true;

  //////////////////////////////////////////////////
  // Instance variables

  protected String dmrtext = null;
  protected DapDataset dmr = null;
  protected String location = null;

  // Input stream
  protected DeChunkedInputStream stream = null;
  protected boolean streamclosed = false;

  protected XURI xuri = null;

  // Exported information
  protected ByteOrder remoteorder = null;
  protected ChecksumMode checksummode = null;
  protected RequestMode mode = null;

  // DAP stream compilation
  D4DataCompiler d4compiler = null;

  protected Map<DapVariable, D4Cursor> datamap = new HashMap<>();

  //////////////////////////////////////////////////
  // Constructor(s)

  public D4DSP() {}

  //////////////////////////////////////////////////

  /**
   * "open" a reference to a data source and return the DSP wrapper.
   *
   * @param location - Object that defines the data source
   * @param cmode
   * @return = wrapping dsp
   * @throws DapException
   */
  public D4DSP open(String location, ChecksumMode cmode) throws DapException {
    this.location = location;
    this.checksummode = cmode;
    parseURL(location);
    return this;
  }


  //////////////////////////////////////////////////
  // Accessors

  public DeChunkedInputStream getStream() {
    return this.stream;
  }

  public ChecksumMode getChecksumMode() {
    return this.checksummode;
  }

  public Map<DapVariable, D4Cursor> getVariableDataMap() {
    return this.datamap;
  }

  public Map<DapVariable, Long> getChecksumMap(ChecksumSource src) {
    if (src == ChecksumSource.LOCAL)
      return this.d4compiler.localchecksummap;
    else
      return this.d4compiler.remotechecksummap;
  }

  protected D4DSP setStream(InputStream input, RequestMode mode) throws IOException {
    this.mode = mode;
    this.stream = new DeChunkedInputStream(input, mode); // side effect: read DMR &/or error
    this.streamclosed = false;
    if (this.stream.getState() == DeChunkedInputStream.State.ERROR)
      reportError(this.stream.getErrorText());
    this.dmrtext = this.stream.getDMRText();
    // This is the definitive remote byte order
    this.remoteorder = this.stream.getRemoteOrder();
    return this;
  }

  protected void addVariableData(DapVariable var, D4Cursor cursor) {
    this.datamap.put(var, cursor);
  }

  public DapDataset getDMR() {
    return this.dmr;
  }

  protected void setDMR(DapDataset dmr) {
    this.dmr = dmr;
    if (this.dmr != null) {
      // Add some canonical attributes to the <Dataset>
      this.dmr.setDataset(this.dmr);
      this.dmr.setDapVersion(DapConstants.X_DAP_VERSION);
      this.dmr.setDMRVersion(DapConstants.X_DMR_VERSION);
      this.dmr.setNS(DapConstants.X_DAP_NS);
    }
  }

  /*
   * public static String printDMR(DapDataset dmr) {
   * StringWriter sw = new StringWriter();
   * PrintWriter pw = new PrintWriter(sw);
   * DMRPrinter printer = new DMRPrinter(dmr, pw);
   * try {
   * printer.print();
   * pw.close();
   * sw.close();
   * } catch (IOException e) {
   * }
   * return sw.toString();
   * }
   */

  //////////////////////////////////////////////////
  // Subclass defined

  /**
   * Determine if a path refers to an object processable by this DSP
   *
   * @param path
   * @param context
   * @return true if this path can be processed by an instance of this DSP
   */
  abstract public boolean dspMatch(String path, DapContext context);

  //////////////////////////////////////////////////
  // Compilation

  /**
   * Extract the DMR from available dechunked data
   *
   * @throws DapException
   */
  public void loadDMR() throws IOException {
    String document = readDMR();
    DapDataset dmr = parseDMR(document);
    setDMR(dmr);
    if (this.mode == RequestMode.DMR) {
      assert !this.streamclosed;
      this.stream.close(); // no longer needed
      this.streamclosed = true;
    }
  }

  public void loadDAP() throws IOException {
    try {
      // "Compile" the databuffer section of the server response
      d4compiler = new D4DataCompiler(this, this.checksummode, this.remoteorder);
      d4compiler.compile();
      assert !this.streamclosed;
      this.stream.close(); // no longer needed
      this.streamclosed = true;
    } catch (IOException ioe) {
      throw new DapException(ioe);
    }
  }

  public void loadContext(DapContext cxt, RequestMode mode) {
    switch (mode) {
      case DMR:
        break;
      case DAP:
        cxt.put(ChecksumSource.REMOTE, d4compiler.getChecksumMap(ChecksumSource.REMOTE));
        cxt.put(ChecksumSource.LOCAL, d4compiler.getChecksumMap(ChecksumSource.LOCAL));
        cxt.put(D4Cursor.class, getVariableDataMap());
        break;
    }
  }

  protected String readDMR() throws IOException {
    // Get and clean up dmr
    String dmrtext = this.stream.getDMRText();
    dmrtext = dmrtext.trim();
    if (dmrtext.length() == 0)
      throw new DapException("Empty DMR");
    StringBuilder buf = new StringBuilder(dmrtext);
    // remove any trailing '\n'
    int len = buf.length();
    if (buf.charAt(len - 1) == '\n')
      buf.setLength(--len);
    // Make sure it has trailing \r"
    if (buf.charAt(len - 1) != '\r')
      buf.append('\r');
    // Make sure it has trailing \n"
    buf.append('\n');
    return buf.toString();
  }

  //////////////////////////////////////////////////
  // Misc. Utilities

  protected void parseURL(String url) throws DapException {
    try {
      this.xuri = new XURI(url);
    } catch (URISyntaxException use) {
      throw new DapException(use);
    }
  }

  protected String getMethodUrl(RequestMode mode, ChecksumMode csum) throws DapException {
    xuri.removeQueryField(DapConstants.CHECKSUMTAG);
    csum = ChecksumMode.asTrueFalse(csum);
    String scsum = ChecksumMode.toString(csum);
    xuri.insertQueryField(DapConstants.CHECKSUMTAG, scsum);
    String corepath = DapUtil.stripDap4Extensions(xuri.getPath());
    // modify url to read the dmr|dap
    if (mode == RequestMode.DMR)
      xuri.setPath(corepath + ".dmr.xml");
    else if (mode == RequestMode.DAP)
      xuri.setPath(corepath + ".dap");
    else
      throw new DapException("Unexpected mode: " + mode);
    String methodurl = xuri.assemble(XURI.URLQUERY);
    return methodurl;
  }

  /**
   * It is common to want to parse a DMR text to a DapDataset,
   * so provide this utility.
   *
   * @param document the dmr to parse
   * @return the parsed dmr
   * @throws DapException on parse errors
   */

  protected DapDataset parseDMR(String document) throws DapException {
    // Parse the dmr
    Dap4Parser parser;
    // if(USEDOM)
    parser = new DOM4Parser(null);
    // else
    // parser = new DOM4Parser(new DefaultDMRFactory());
    if (PARSEDEBUG)
      parser.setDebugLevel(1);
    try {
      if (!parser.parse(document))
        throw new DapException("DMR Parse failed");
    } catch (SAXException se) {
      throw new DapException(se);
    }
    if (parser.getErrorResponse() != null)
      throw new DapException("Error Response Document not supported");
    DapDataset result = parser.getDMR();
    processAttributes(result);
    processMaps(result);
    return result;
  }

  /**
   * Some attributes that are added by the NetcdfDataset
   * need to be kept out of the DMR. This function
   * defines that set.
   *
   * @param attrname
   * @return true if the attribute should be suppressed, false otherwise.
   */
  protected boolean suppressAttributes(String attrname) {
    if (attrname.startsWith("_Coord"))
      return true;
    if (attrname.equals("_Unsigned"))
      return true;
    return false;
  }

  void getEndianAttribute(DapDataset dataset) {
    DapAttribute a = dataset.findAttribute(DapConstants.LITTLEENDIANATTRNAME);
    if (a == null)
      return;
    Object v = a.getValues();
    int len = java.lang.reflect.Array.getLength(v);
    if (len == 0)
      this.remoteorder = ByteOrder.LITTLE_ENDIAN;
    else {
      String onezero = java.lang.reflect.Array.get(v, 0).toString();
      int islittle = 1;
      try {
        islittle = Integer.parseInt(onezero);
      } catch (NumberFormatException e) {
        islittle = 1;
      }
      if (islittle == 0)
        this.remoteorder = ByteOrder.BIG_ENDIAN;
      else
        this.remoteorder = ByteOrder.LITTLE_ENDIAN;
    }
  }

  /**
   * Walk the dataset tree and remove selected attributes
   * such as _Unsigned
   *
   * @param dataset
   */
  protected void processAttributes(DapDataset dataset) throws DapException {
    List<DapNode> nodes = dataset.getNodeList();
    for (DapNode node : nodes) {
      switch (node.getSort()) {
        case GROUP:
        case DATASET:
        case VARIABLE:
          Map<String, DapAttribute> attrs = node.getAttributes();
          if (attrs.size() > 0) {
            List<DapAttribute> suppressed = new ArrayList<>();
            for (DapAttribute dattr : attrs.values()) {
              if (suppressAttributes(dattr.getShortName()))
                suppressed.add(dattr);
            }
            for (DapAttribute dattr : suppressed) {
              node.removeAttribute(dattr);
            }
          }
          break;
        default:
          break; /* ignore */
      }
    }
    // Try to extract the byte order
    getEndianAttribute(dataset);
  }

  /**
   * Walk the dataset tree and link <Map targets to the actual variable
   *
   * @param dataset
   */
  protected void processMaps(DapDataset dataset) throws DapException {
    List<DapNode> nodes = dataset.getNodeList();
    for (DapNode node : nodes) {
      switch (node.getSort()) {
        case MAP:
          DapMap map = (DapMap) node;
          String targetname = map.getTargetName();
          DapVariable target;
          target = (DapVariable) dataset.findByFQN(targetname, DapSort.VARIABLE, DapSort.SEQUENCE, DapSort.STRUCTURE);
          if (target == null)
            throw new DapException("Mapref: undefined target variable: " + targetname);
          // Verify that this is a legal map =>
          // 1. it is outside the scope of its parent if the parent
          // is a structure.
          DapNode container = target.getContainer();
          if ((container.getSort() == DapSort.STRUCTURE || container.getSort() == DapSort.SEQUENCE))
            throw new DapException("Mapref: map target variable not in outer scope: " + targetname);
          map.setVariable(target);
          break;
        default:
          break; /* ignore */
      }
    }
  }

  protected void reportError(String errmsg) throws IOException {
    throw new DapException(errmsg);
  }

}
