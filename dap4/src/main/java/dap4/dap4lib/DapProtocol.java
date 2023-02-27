/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.util.ResponseFormat;
import dap4.core.util.XURI;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Store protocol related constants
 */

public abstract class DapProtocol implements DapCodes {

  //////////////////////////////////////////////////
  // Constants

  // protected static final String DAPVERSION = "4.0";
  // protected static final String DMRVERSION = "1.0";

  protected static final String DAP4PROTO = "dap4";

  //////////////////////////////////////////////////
  // Static variables

  static protected final Set<String> DAP4EXTENSIONS;
  static protected final Set<String> DAP4QUERYMARKERS;
  static protected final Set<String> DAP4SCHEMES;

  static {
    DAP4EXTENSIONS = new HashSet<String>();
    DAP4EXTENSIONS.add("dmr");
    DAP4EXTENSIONS.add("dap");
    DAP4EXTENSIONS.add("dst");

    DAP4QUERYMARKERS = new HashSet<String>();
    DAP4QUERYMARKERS.add("dap4.checksum");
    DAP4QUERYMARKERS.add("dap4.ce");

    DAP4SCHEMES = new HashSet<String>();
    DAP4SCHEMES.add("dap4");
    DAP4SCHEMES.add("http");
    DAP4SCHEMES.add("https");
    // Note that file: is missing
  };


  // Map RequestMode X ResponseFormat => ContentType
  public static Map<String, ContentType> contenttypes;

  static public String contentKey(RequestMode mode, ResponseFormat format) {
    return mode.id() + "." + format.id();
  }

  static {
    // Map request x response -> (contentype,mimetype)
    // If response is none, then this indicates the default
    contenttypes = new HashMap<String, ContentType>();
    contenttypes.put(contentKey(RequestMode.NONE, ResponseFormat.NONE), new ContentType(RequestMode.DSR,
        ResponseFormat.XML, "application/vnd.opendap.dap4.dataset-services+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.DMR, ResponseFormat.NONE), new ContentType(RequestMode.DMR,
        ResponseFormat.XML, "application/vnd.opendap.dap4.dataset-metadata+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.DMR, ResponseFormat.XML), new ContentType(RequestMode.DMR,
        ResponseFormat.XML, "application/vnd.opendap.dap4.dataset-metadata+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.DAP, ResponseFormat.NONE), new ContentType(RequestMode.DAP,
        ResponseFormat.NONE, "application/vnd.opendap.dap4.data", "application/octet-stream"));
    contenttypes.put(contentKey(RequestMode.DSR, ResponseFormat.NONE), new ContentType(RequestMode.DSR,
        ResponseFormat.HTML, "application/vnd.opendap.dap4.dataset-services+html", "text/html"));
    contenttypes.put(contentKey(RequestMode.DSR, ResponseFormat.XML), new ContentType(RequestMode.DSR,
        ResponseFormat.XML, "application/vnd.opendap.dap4.dataset-services+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.DSR, ResponseFormat.HTML), new ContentType(RequestMode.DSR,
        ResponseFormat.HTML, "application/vnd.opendap.dap4.dataset-services+html", "text/html"));
    contenttypes.put(contentKey(RequestMode.CAPABILITIES, ResponseFormat.NONE), new ContentType(
        RequestMode.CAPABILITIES, ResponseFormat.HTML, "application/vnd.opendap.dap4.capabilities+html", "text/html"));
    contenttypes.put(contentKey(RequestMode.CAPABILITIES, ResponseFormat.XML), new ContentType(RequestMode.CAPABILITIES,
        ResponseFormat.XML, "application/vnd.opendap.dap4.capabilities+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.CAPABILITIES, ResponseFormat.HTML), new ContentType(
        RequestMode.CAPABILITIES, ResponseFormat.HTML, "application/vnd.opendap.dap4.capabilities+html", "text/html"));
    contenttypes.put(contentKey(RequestMode.ERROR, ResponseFormat.NONE),
        new ContentType(RequestMode.ERROR, ResponseFormat.XML, "application/vnd.opendap.dap4.error+xml", "text/xml"));
    contenttypes.put(contentKey(RequestMode.ERROR, ResponseFormat.XML),
        new ContentType(RequestMode.ERROR, ResponseFormat.XML, "application/vnd.opendap.dap4.error+xml", "text/xml"));
    // Everything else is an error
  }

  //////////////////////////////////////////////////

  public static class ContentType {
    public RequestMode mode;
    public ResponseFormat format;
    public String contenttype;
    public String mimetype;

    public ContentType(RequestMode mode, ResponseFormat format, String contenttype, String mimetype) {
      this.mode = mode;
      this.format = format;
      this.contenttype = contenttype;
      this.mimetype = mimetype;
    }
  }

  //////////////////////////////////////////////////
  // Definitive test if a url looks like it is a DAP4 url

  /**
   * A path is a DAP4 path if at least one of the following is true.
   * 1. it has "dap4:" as its leading protocol
   * 2. it has protocol=dap4 | mode=dap4 | dap4 in its fragment
   * 3. it has dmr|dsr|dap as its request extension
   * 4. it has dap4|d4ts in its path
   *
   * @param xuri parsed uri
   * @return true if this uri appears to be processible by DAP4
   */
  static public boolean isDap4URI(XURI xuri) {
    boolean found = false;
    // This is definitive
    if ("dap4".equalsIgnoreCase(xuri.getScheme()))
      return true;
    // Necessary but not sufficient
    if (!DAP4SCHEMES.contains(xuri.getScheme().toLowerCase()))
      return false;
    for (Map.Entry entry : xuri.getQueryFields().entrySet()) {
      if (DAP4QUERYMARKERS.contains(entry.getKey()))
        return true; // definitive
    }
    // Fragment checking is a bit more complex
    String d4 = xuri.getFragFields().get("dap4");
    if (d4 != null)
      return true; // definitive
    String p = xuri.getFragFields().get("protocol");
    if (p != null) {
      if (p.equalsIgnoreCase("dap4"))
        return true; // definitive
    }
    String modes = xuri.getFragFields().get("mode");
    if (modes != null) {
      String[] modelist = modes.split("[,]");
      for (String mode : modelist) {
        if (mode.equalsIgnoreCase("dap4"))
          return true; // definitive
      }
    }
    return false;
  }

  static public boolean isDap4URI(String uri) {
    try {
      XURI xuri = new XURI(uri);
      return isDap4URI(xuri);
    } catch (URISyntaxException use) {
      return false;
    }
  }

}
