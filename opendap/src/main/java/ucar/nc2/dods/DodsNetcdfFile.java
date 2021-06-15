/*
 * (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
 */
package ucar.nc2.dods;

import com.google.common.base.Preconditions;
import java.nio.charset.StandardCharsets;
import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import ucar.array.ArraysConvert;
import ucar.ma2.*;
import ucar.nc2.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import ucar.nc2.util.CancelTask;

/** A DODS dataset as seen through the Netcdf API. */
@NotThreadSafe
public class DodsNetcdfFile extends ucar.nc2.NetcdfFile {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DodsNetcdfFile.class);

  static boolean accept_compress = false;
  static private boolean preload = true;
  static private int preloadCoordVarSize = 50000;

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
   * 
   * @param b true or false.
   */
  public static void setAllowCompression(boolean b) {
    accept_compress = b;
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

  //////////////////////////////////////////////////////////////////////////////////
  private final ConvertD2N convertD2N = new ConvertD2N();
  private final DConnect2 dodsConnection;
  private final DDS dds;
  private final DAS das;

  DodsNetcdfFile(DodsBuilder builder) throws IOException {
    super(builder);
    Preconditions.checkNotNull(builder.dodsConnection);
    Preconditions.checkNotNull(builder.dds);
    Preconditions.checkNotNull(builder.das);
    this.dodsConnection = builder.dodsConnection;
    this.dds = builder.dds;
    this.das = builder.das;

    // preload scalers, coordinate variables, strings, small arrays
    if (DodsNetcdfFile.preload) {
      List<Variable> preloadList = new ArrayList<>();
      for (Variable dodsVar : getVariables()) {
        long size = dodsVar.getSize() * dodsVar.getElementSize();
        if ((dodsVar.isCoordinateVariable() && size < DodsNetcdfFile.preloadCoordVarSize) || dodsVar.isCaching()
            || dodsVar.getDataType() == DataType.STRING) {
          dodsVar.setCaching(true);
          preloadList.add(dodsVar);
          if (DodsNetcdfFiles.debugPreload)
            System.out.printf("  preload (%6d) %s%n", size, dodsVar.getFullName());
        }
      }
      preloadData(preloadList);
    }
  }

  @Override
  public synchronized void close() throws java.io.IOException {
    if (cache != null) {
      if (cache.release(this))
        return;
    }
    dodsConnection.close();
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
    if (DodsNetcdfFiles.debugServerCall)
      System.out.println("DODSNetcdfFile.readDataDDSfromServer = <" + CE + ">");

    long start = 0;
    if (DodsNetcdfFiles.debugTime)
      start = System.currentTimeMillis();

    if (!CE.startsWith("?"))
      CE = "?" + CE;
    DataDDS data;
    synchronized (this) {
      data = dodsConnection.getData(CE, null);
    }
    if (DodsNetcdfFiles.debugTime)
      System.out
          .println("DODSNetcdfFile.readDataDDSfromServer took = " + (System.currentTimeMillis() - start) / 1000.0);

    if (DodsNetcdfFiles.debugDataResult) {
      System.out.println(" dataDDS return:");
      data.print(System.out);
    }

    return data;
  }

  ///////////////////////////////////////////////////////////////////
  // ALL the I/O goes through these routines
  // called from ucar.nc2.Variable

  /**
   * Make a single call to the DODS Server to read and cache all the named Variables
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
            if (DodsNetcdfFiles.debugConvertData) {
              System.out.println("readArray found dataV= " + DodsNetcdfFiles.makeDODSname(ddsV));
            }
            dataV.isDone = true;
            map.put(ddsV, dataV); // thread safe!
          } else {
            logger.error("ERROR findDataV cant find " + DodsNetcdfFiles.makeDODSname(ddsV) + " on " + getLocation());
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
          logger.error("DODSNetcdfFile.readArrays cant find " + DodsNetcdfFiles.makeDODSname(ddsV) + " in dataDDS; "
              + getLocation());
        } else {
          if (DodsNetcdfFiles.debugConvertData) {
            System.out.println("readArray converting " + DodsNetcdfFiles.makeDODSname(ddsV));
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
            this.setCachedData(var, ucar.array.ArraysConvert.convertToArray(data));
            if (DodsNetcdfFiles.debugCached) {
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

  protected ucar.array.Array<?> readArrayData(Variable v, ucar.array.Section section)
      throws IOException, ucar.array.InvalidRangeException {
    ucar.ma2.Section sectionOld = ArraysConvert.convertSection(section);
    try {
      ucar.ma2.Array result = readData(v, sectionOld);
      return ArraysConvert.convertToArray(result);
    } catch (InvalidRangeException e) {
      throw new ucar.array.InvalidRangeException(e);
    }
  }

  @Override
  protected ucar.ma2.Array readData(ucar.nc2.Variable v, ucar.ma2.Section section)
      throws IOException, InvalidRangeException {
    // if (unlocked)
    // throw new IllegalStateException("File is unlocked - cannot use");

    // LOOK: what if theres already a CE !!!!
    // create the constraint expression
    StringBuilder buff = new StringBuilder(100);
    buff.setLength(0);
    buff.append(DodsNetcdfFiles.getDODSConstraintName(v));

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
      DodsV want = null;
      // Find the child node matching the requested variable
      for (int i = 0; i < root.children.size(); i++) {
        DodsV element = root.children.get(i);
        if (element.getFullName().equals(v.getFullName())) {
          want = element;
          break;
        }
      }

      if (want == null) {
        throw new ParseException("Variable " + v.getFullName() + " not found in DDS.");
      }

      dataArray = convertD2N.convertTopVariable(v, section.getRanges(), want);
      // if reading from a server response, we have exactly the section of data
      // requested. If reading from a file, we need to make sure we are only returning
      // the section. What's not-so-good is that we've already read the entire array into
      // memory when parsing the binary file.
      if (getLocation().startsWith("file:")) {
        dataArray = dataArray.section(section.getRanges());
      }
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

    buff.append(DodsNetcdfFiles.getDODSConstraintName(s));

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

    public DodsNetcdfFile build(String datasetUrl, CancelTask cancelTask) throws IOException {
      return super.build(datasetUrl, cancelTask);
    }
  }

}
