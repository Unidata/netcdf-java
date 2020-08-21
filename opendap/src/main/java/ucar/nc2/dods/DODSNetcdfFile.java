/*
 * (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.dods;

import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import ucar.ma2.*;
import ucar.nc2.*;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import ucar.nc2.util.CancelTask;

/** A DODS dataset as seen through the Netcdf API. */
@NotThreadSafe
public class DODSNetcdfFile extends ucar.nc2.NetcdfFile {
  static boolean debugCE = false;
  static boolean debugServerCall = false;
  static boolean debugOpenResult = false;
  static boolean debugDataResult = false;
  static boolean debugCharArray = false;
  static boolean debugConvertData = false;
  static boolean debugConstruct = false;
  static boolean debugPreload = false;
  static boolean debugTime = false;
  static boolean showNCfile = false;
  static boolean debugAttributes = false;
  static boolean debugCached = false;
  static boolean debugOpenTime = false;

  static boolean accept_compress = false;
  static boolean preload = true;
  static int preloadCoordVarSize = 50000;

  /**
   * Set whether to allow sessions by allowing cookies. This only affects requests to the TDS.
   * Setting this to true can eliminate consistency problems for datasets that are being updated.
   *
   * @param b true or false. default is false.
   */
  public static void setAllowSessions(boolean b) {
    DConnect2.setAllowSessions(b);
  }

  /**
   * Set whether to allow messages to be compressed.
   * LOOK why not true as default?
   * @param b true or false.
   */
  public static void setAllowCompression(boolean b) {
    accept_compress = b;
  }

  /** Debugging */
  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    debugCE = debugFlag.isSet("DODS/constraintExpression");
    debugServerCall = debugFlag.isSet("DODS/serverCall");
    debugOpenResult = debugFlag.isSet("DODS/debugOpenResult");
    debugDataResult = debugFlag.isSet("DODS/debugDataResult");
    debugCharArray = debugFlag.isSet("DODS/charArray");
    debugConstruct = debugFlag.isSet("DODS/constructNetcdf");
    debugPreload = debugFlag.isSet("DODS/preload");
    debugTime = debugFlag.isSet("DODS/timeCalls");
    showNCfile = debugFlag.isSet("DODS/showNCfile");
    debugAttributes = debugFlag.isSet("DODS/attributes");
    debugCached = debugFlag.isSet("DODS/cache");
  }

  /**
   * Set whether small variables are preloaded; only turn off for debugging.
   *
   * @param b true if small variables are preloaded (default true)
   */
  public static void setPreload(boolean b) {
    preload = b;
  }

  /**
   * If preloading, set maximum size of coordinate variables to be preloaded.
   *
   * @param size maximum size of coordinate variables to be preloaded.
   */
  public static void setCoordinateVariablePreloadSize(int size) {
    preloadCoordVarSize = size;
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:" or "https:", change it to start with "dods:", otherwise
   * leave it alone.
   *
   * @param urlName the url string
   * @return canonical form
   */
  public static String canonicalURL(String urlName) {
    if (urlName.startsWith("http:"))
      return "dods:" + urlName.substring(5);
    if (urlName.startsWith("https:"))
      return "dods:" + urlName.substring(6);
    return urlName;
  }

  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DODSNetcdfFile.class);

  //////////////////////////////////////////////////////////////////////////////////
  private final ConvertD2N convertD2N = new ConvertD2N();
  private final DConnect2 dodsConnection;
  private final DDS dds;
  private final DAS das;

  DODSNetcdfFile(DodsBuilder builder) throws IOException {
    super(builder);
    Preconditions.checkNotNull(builder.dodsConnection);
    Preconditions.checkNotNull(builder.dds);
    Preconditions.checkNotNull(builder.das);
    this.dodsConnection = builder.dodsConnection;
    this.dds = builder.dds;
    this.das = builder.das;
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (cache != null) {
      if (cache.release(this))
        return;
    }
    dodsConnection.close();
  }

  /**
   * Checks to see if this is netcdf char array.
   *
   * @param v must be type STRING
   * @return string length dimension, else null
   */
  Dimension getNetcdfStrlenDim(DodsVariable v) {
    AttributeTable table = das.getAttributeTableN(v.getFullName()); // LOOK this probably doesnt work for nested
                                                                    // variables
    if (table == null)
      return null;

    opendap.dap.Attribute dodsAtt = table.getAttribute("DODS");
    if (dodsAtt == null)
      return null;

    AttributeTable dodsTable = dodsAtt.getContainerN();
    if (dodsTable == null)
      return null;

    opendap.dap.Attribute att = dodsTable.getAttribute("strlen");
    if (att == null)
      return null;
    String strlen = att.getValueAtN(0);

    opendap.dap.Attribute att2 = dodsTable.getAttribute("dimName");
    String dimName = (att2 == null) ? null : att2.getValueAtN(0);
    if (debugCharArray)
      System.out.println(v.getFullName() + " has strlen= " + strlen + " dimName= " + dimName);

    int dimLength;
    try {
      dimLength = Integer.parseInt(strlen);
    } catch (NumberFormatException e) {
      logger
          .warn("DODSNetcdfFile " + getLocation() + " var = " + v.getFullName() + " error on strlen attribute = " + strlen);
      return null;
    }

    if (dimLength <= 0)
      return null; // LOOK what about unlimited ??
    return Dimension.builder(dimName, dimLength).setIsShared(dimName != null).build();
  }

  /**
   * Return a variable name suitable for use in a DAP constraint expression.
   * [Original code seemed wrong because structures can be nested and hence
   * would have to use the full name just like non-structures]
   *
   * @param var The variable whose name will appear in the CE
   * @return The name in a form suitable for use in a cE
   */
  public static String getDODSConstraintName(Variable var) {
    String vname = ((DODSNode) var).getDODSName();
    // The vname is backslash escaped, so we need to
    // modify to use DAP %xx escapes.
    return EscapeStringsDap.backslashToDAP(vname);
  }

  // full name

  private String makeDODSname(DodsV dodsV) {
    DodsV parent = dodsV.parent;
    if (parent.bt != null)
      return (makeDODSname(parent) + "." + dodsV.bt.getEncodedName());
    return dodsV.bt.getEncodedName();
  }

  static String makeShortName(String name) {
    String unescaped = EscapeStringsDap.unescapeDAPIdentifier(name);
    int index = unescaped.lastIndexOf('/');
    if (index < 0)
      index = -1;
    return unescaped.substring(index + 1, unescaped.length());
  }

  static String makeDODSName(String name) {
    return EscapeStringsDap.unescapeDAPIdentifier(name);
  }

  /**
   * Get the DODS data class corresponding to the Netcdf data type.
   * This is the inverse of convertToNCType().
   *
   * @param dataType Netcdf data type.
   * @return the corresponding DODS type enum, from opendap.dap.Attribute.XXXX.
   */
  public static int convertToDODSType(DataType dataType) {
    if (dataType == DataType.STRING)
      return opendap.dap.Attribute.STRING;
    if (dataType == DataType.BYTE)
      return opendap.dap.Attribute.BYTE;
    if (dataType == DataType.FLOAT)
      return opendap.dap.Attribute.FLOAT32;
    if (dataType == DataType.DOUBLE)
      return opendap.dap.Attribute.FLOAT64;
    if (dataType == DataType.SHORT)
      return opendap.dap.Attribute.INT16;
    if (dataType == DataType.USHORT)
      return opendap.dap.Attribute.UINT16;
    if (dataType == DataType.INT)
      return opendap.dap.Attribute.INT32;
    if (dataType == DataType.UINT)
      return opendap.dap.Attribute.UINT32;
    if (dataType == DataType.BOOLEAN)
      return opendap.dap.Attribute.BYTE;
    if (dataType == DataType.LONG)
      return opendap.dap.Attribute.INT32; // LOOK no LONG type!

    // shouldnt happen
    return opendap.dap.Attribute.STRING;
  }

  /**
   * Get the Netcdf data type corresponding to the DODS data type.
   * This is the inverse of convertToDODSType().
   *
   * @param dodsDataType DODS type enum, from dods.dap.Attribute.XXXX.
   * @return the corresponding netcdf DataType.
   * @see #isUnsigned
   */
  public static DataType convertToNCType(int dodsDataType, boolean isUnsigned) {
    switch (dodsDataType) {
      case opendap.dap.Attribute.BYTE:
        return isUnsigned ? DataType.UBYTE : DataType.BYTE;
      case opendap.dap.Attribute.FLOAT32:
        return DataType.FLOAT;
      case opendap.dap.Attribute.FLOAT64:
        return DataType.DOUBLE;
      case opendap.dap.Attribute.INT16:
        return DataType.SHORT;
      case opendap.dap.Attribute.UINT16:
        return DataType.USHORT;
      case opendap.dap.Attribute.INT32:
        return DataType.INT;
      case opendap.dap.Attribute.UINT32:
        return DataType.UINT;
      default:
        return DataType.STRING;
    }
  }

  /**
   * Get the Netcdf data type corresponding to the DODS BaseType class.
   * This is the inverse of convertToDODSType().
   *
   * @param dtype DODS BaseType.
   * @return the corresponding netcdf DataType.
   * @see #isUnsigned
   */
  public static DataType convertToNCType(opendap.dap.BaseType dtype, boolean isUnsigned) {

    if (dtype instanceof DString)
      return DataType.STRING;
    else if ((dtype instanceof DStructure) || (dtype instanceof DSequence) || (dtype instanceof DGrid))
      return DataType.STRUCTURE;
    else if (dtype instanceof DFloat32)
      return DataType.FLOAT;
    else if (dtype instanceof DFloat64)
      return DataType.DOUBLE;
    else if (dtype instanceof DUInt32)
      return DataType.UINT;
    else if (dtype instanceof DUInt16)
      return DataType.USHORT;
    else if (dtype instanceof DInt32)
      return DataType.INT;
    else if (dtype instanceof DInt16)
      return DataType.SHORT;
    else if (dtype instanceof DByte)
      return isUnsigned ? DataType.UBYTE : DataType.BYTE;
    else
      throw new IllegalArgumentException("DODSVariable illegal type = " + dtype.getTypeName());
  }

  /**
   * Get whether this is an unsigned type.
   *
   * @param dtype DODS BaseType.
   * @return true if unsigned
   */
  public static boolean isUnsigned(opendap.dap.BaseType dtype) {
    return (dtype instanceof DByte) || (dtype instanceof DUInt16) || (dtype instanceof DUInt32);
  }

  /////////////////////////////////////////////////////////////////////////////////////

  /**
   * This does the actual connection to the opendap server and reading of the data.
   * All data calls go through here so we can add debugging.
   *
   * @param CE constraint expression; use empty string if none
   * @return DataDDS
   * @throws java.io.IOException on io error
   * @throws opendap.dap.parsers.ParseException
   *         if error parsing return
   * @throws opendap.dap.DAP2Exception if you have otherwise been bad
   */
  DataDDS readDataDDSfromServer(String CE) throws IOException, opendap.dap.DAP2Exception {
    if (debugServerCall)
      System.out.println("DODSNetcdfFile.readDataDDSfromServer = <" + CE + ">");

    long start = 0;
    if (debugTime)
      start = System.currentTimeMillis();

    if (!CE.startsWith("?"))
      CE = "?" + CE;
    DataDDS data;
    synchronized (this) {
      data = dodsConnection.getData(CE, null);
    }
    if (debugTime)
      System.out
          .println("DODSNetcdfFile.readDataDDSfromServer took = " + (System.currentTimeMillis() - start) / 1000.0);

    if (debugDataResult) {
      System.out.println(" dataDDS return:");
      data.print(System.out);
    }

    return data;
  }

  ///////////////////////////////////////////////////////////////////
  // ALL the I/O goes through these routines
  // called from ucar.nc2.Variable

  /**
   * Make a single call to the DODS Server to read and cache all the named v\Variable's
   * in one client/server roundtrip.
   *
   * @param preloadVariables list of type Variable
   * @throws IOException on error
   */
  private void preloadData(List<Variable> preloadVariables) throws IOException {
    // For performance tests:
    // if (true) return super.readArrays (variables);
    if (preloadVariables.size() == 0)
      return;

    // construct the list of variables, skipping ones with cached data
    List<DodsV> reqDodsVlist = new ArrayList<DodsV>();
    DodsV root;
    for (Variable var : preloadVariables) {
      if (var.hasCachedData())
        continue;
      reqDodsVlist.add((DodsV) var.getSPobject());
    }
    Collections.sort(reqDodsVlist); // "depth first" order

    // read the data
    DataDDS dataDDS;
    Map<DodsV, DodsV> map = new HashMap<DodsV, DodsV>(2 * reqDodsVlist.size() + 1);
    // As we build the request URL, we need to keep in mind that there is a limit on the length of a GET request
    // URL, otherwise we will run into a 414 (https://github.com/Unidata/netcdf-java/issues/413)
    // According to stackoverflow lore, the 414 does not always happen, so sometimes things will fail silently.
    // This limit is configurable on the server side, and there does not appear to be a "one size fits all web server
    // stacks" solution here. RFC 2616 says "Note: Servers ought to be cautious about depending on URI lengths
    // above 255 bytes, because some older client or proxy implementations might not properly support these lengths."
    // 255 byte URIs feels too small for DAP (i.e. I'd assume the maintainers of a DAP server with that limitation
    // would hear about it quickly, and adjust the settings higher, so we won't go with that as the max uri size).
    // Apache 2.4 has a default limit of 8190 bytes (https://httpd.apache.org/docs/2.4/mod/core.html#limitrequestline)
    // * Nginx has a default of 8K bytes:
    // http://nginx.org/en/docs/http/ngx_http_core_module.html#large_client_header_buffers
    // * Microsoft IIS has a default of 4096 bytes:
    // https://docs.microsoft.com/en-us/iis/configuration/system.webserver/security/requestfiltering/requestlimits/#attributes
    // * Tomcat has a default header size of 8192 bytes - not quite the same thing as the url size defined above, but
    // effectively much less than the limits above because the header will contain the various parts of the request
    // URI. Again, stackoverflow lore suggest tomcat issue start to creep in when the URL gets above 4kB (as that's
    // only part of the header size).
    // Given the above defaults, we'll go with 4kb for now.
    // If we find this gives us trouble in the future, we should just go with the limit set by Internet Explorer,
    // since we know web servers will likely at least support that, which is 2048 bytes.
    // https://support.microsoft.com/en-us/help/208427/maximum-url-length-is-2-083-characters-in-internet-explorer
    int maxQueryLength = 4096 - this.getLocation().length(); // just keep track of the query size
    // Track where we are in reqDodsVlist
    int lastRequestedVariableIndex = 0;
    if (reqDodsVlist.size() > 0) {
      // keep preloading until we get all the variables
      while (lastRequestedVariableIndex < reqDodsVlist.size()) {
        // current length of the query for this round of prefetching
        int queryLength = 0;
        // track number of variables being requested in this round of prefetching
        short numberOfVarsInRequest = 0;
        // Create the request
        StringBuilder requestString = new StringBuilder();
        // keep the length of the query under the maxUriSize
        while (queryLength <= maxQueryLength && lastRequestedVariableIndex < reqDodsVlist.size()) {
          DodsV dodsV = reqDodsVlist.get(lastRequestedVariableIndex);
          // will this take us over our query length limit?
          int newQueryLength = queryLength + dodsV.getEncodedName().length() + 1; // +1 for var separator
          if (newQueryLength >= maxQueryLength) {
            break;
          } else {
            // we're good on size - add the variable to the query
            requestString.append(numberOfVarsInRequest == 0 ? "?" : ",");
            requestString.append(dodsV.getEncodedName());
            // bump up the query length, increment to next request variable
            queryLength = newQueryLength;
            lastRequestedVariableIndex += 1;
            numberOfVarsInRequest += 1;
          }
        }

        try {
          dataDDS = readDataDDSfromServer(requestString.toString());
          root = DodsV.parseDataDDS(dataDDS);

        } catch (Exception exc) {
          logger.error("ERROR readDataDDSfromServer on " + requestString, exc);
          throw new IOException(exc.getMessage());
        }

        // gotta find the corresponding data from this round of prefetching in "depth first" order
        for (int i = lastRequestedVariableIndex - numberOfVarsInRequest; i < lastRequestedVariableIndex; i++) {
          // variable that was requested
          DodsV ddsV = reqDodsVlist.get(i);
          // requested variable, but from the parsed dds
          DodsV dataV = root.findDataV(ddsV);
          if (dataV != null) {
            if (debugConvertData) {
              System.out.println("readArray found dataV= " + makeDODSname(ddsV));
            }
            dataV.isDone = true;
            map.put(ddsV, dataV); // thread safe!
          } else {
            logger.error("ERROR findDataV cant find " + makeDODSname(ddsV) + " on " + getLocation());
          }
        }
      }
    }

    // For each variable either extract the data or use cached data.
    for (Variable var : preloadVariables) {
      if (!var.hasCachedData()) {
        Array data;
        DodsV ddsV = (DodsV) var.getSPobject();

        DodsV dataV = map.get(ddsV);
        if (dataV == null) {
          logger.error("DODSNetcdfFile.readArrays cant find " + makeDODSname(ddsV) + " in dataDDS; " + getLocation());
        } else {
          if (debugConvertData) {
            System.out.println("readArray converting " + makeDODSname(ddsV));
          }
          dataV.isDone = true;

          try {
            if (var.isMemberOfStructure()) {
              // we want the top structure this variable is contained in.
              while ((dataV.parent != null) && (dataV.parent.bt != null)) {
                dataV = dataV.parent;
              }
              data = convertD2N.convertNestedVariable(var, null, dataV, true);
            } else
              data = convertD2N.convertTopVariable(var, null, dataV);
          } catch (DAP2Exception de) {
            logger.error("ERROR convertVariable on " + var.getFullName(), de);
            throw new IOException(de.getMessage());
          }

          if (var.isCaching()) {
            var.setCachedData(data);
            if (debugCached) {
              System.out.println(" cache for <" + var.getFullName() + "> length =" + data.getSize());
            }
          }
        }
      }
    }
  }

  @Override
  public Array readSection(String variableSection) throws IOException, InvalidRangeException {
    ParsedSectionSpec cer = ParsedSectionSpec.parseVariableSection(this, variableSection);
    return readData(cer.getVariable(), cer.getSection());
  }

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException, InvalidRangeException {
    // if (unlocked)
    // throw new IllegalStateException("File is unlocked - cannot use");

    // LOOK: what if theres already a CE !!!!
    // create the constraint expression
    StringBuilder buff = new StringBuilder(100);
    buff.setLength(0);
    buff.append(getDODSConstraintName(v));

    // add the selector if not a Sequence
    if (!v.isVariableLength()) {
      List<Range> dodsSection = section.getRanges();
      if ((v.getDataType() == DataType.CHAR)) { // CHAR is mapped to DString
        int n = section.getRank();
        if (n == v.getRank()) // remove last section if present
          dodsSection = dodsSection.subList(0, n - 1);
      }
      makeSelector(buff, dodsSection);
    }

    Array dataArray;
    try {
      // DodsV root = DodsV.parseDDS( readDataDDSfromServer(buff.toString()));
      // data = convertD2N( (DodsV) root.children.get(0), v, section, false); // can only be one

      DataDDS dataDDS = readDataDDSfromServer(buff.toString());
      DodsV root = DodsV.parseDataDDS(dataDDS);
      DodsV want = root.children.get(0); // can only be one
      dataArray = convertD2N.convertTopVariable(v, section.getRanges(), want);

    } catch (DAP2Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage() + "; " + v.getShortName() + " -- " + section);

    } catch (ParseException ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    }

    return dataArray;
  }

  public Array readWithCE(ucar.nc2.Variable v, String CE) throws IOException {

    Array dataArray;
    try {

      DataDDS dataDDS = readDataDDSfromServer(CE);
      DodsV root = DodsV.parseDataDDS(dataDDS);
      DodsV want = root.children.get(0); // can only be one

      if (v.isMemberOfStructure())
        dataArray = convertD2N.convertNestedVariable(v, null, want, true);
      else
        dataArray = convertD2N.convertTopVariable(v, null, want);
    } catch (DAP2Exception ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    } catch (ParseException ex) {
      ex.printStackTrace();
      throw new IOException(ex.getMessage());
    }

    return dataArray;
  }

  private int addParents(StringBuilder buff, Variable s, List<Range> section, int start) {
    Structure parent = s.getParentStructure();
    if (parent != null) {
      start = addParents(buff, parent, section, start);
      buff.append(".");
    }

    List<Range> subSection = section.subList(start, start + s.getRank());

    buff.append(getDODSConstraintName(s));

    if (!s.isVariableLength()) // have to get the whole thing for a sequence !!
      makeSelector(buff, subSection);

    return start + s.getRank();
  }

  private void makeSelector(StringBuilder buff, List<Range> section) {
    for (Range r : section) {
      buff.append("[");
      buff.append(r.first());
      buff.append(':');
      buff.append(r.stride());
      buff.append(':');
      buff.append(r.last());
      buff.append("]");
    }
  }

  ////////////////////////////////////////////////////////////////////////////////
  // debugging

  public void getDetailInfo(Formatter f) {
    super.getDetailInfo(f);

    f.format("DDS = %n");
    ByteArrayOutputStream buffOS = new ByteArrayOutputStream(8000);
    dds.print(buffOS);
    f.format("%s%n", new String(buffOS.toByteArray(), StandardCharsets.UTF_8));

    f.format("%nDAS = %n");
    buffOS = new ByteArrayOutputStream(8000);
    das.print(buffOS);
    f.format("%s%n", new String(buffOS.toByteArray(), StandardCharsets.UTF_8));
  }

  public String getFileTypeId() {
    return "OPeNDAP";
  }

  public String getFileTypeDescription() {
    return "Open-source Project for a Network Data Access Protocol";
  }

  public static Builder2 builder() {
    return new Builder2();
  }

  public static class Builder2 extends DodsBuilder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }

    public DODSNetcdfFile build(String datasetUrl, CancelTask cancelTask) throws IOException {
      return super.build(datasetUrl, cancelTask);
    }
  }

}
