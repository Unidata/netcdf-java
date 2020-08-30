/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.server.opendap.servlet;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * Writes MIME type headers to the passed streams.
 *
 * @author ndp
 * @version $Revision: 15901 $
 * @see opendap.dap.BaseType
 */

public class MimeUtil {

  // Send string to set the transfer (mime) type and server version
  // Note that the content description field is used to indicate whether valid
  // information of an error message is contained in the document and the
  // content-encoding field is used to indicate whether the data is compressed.


  public static final int unknown = 0;
  public static final int dods_das = 1;
  public static final int dods_dds = 2;
  public static final int dods_data = 3;
  public static final int dods_error = 4;
  public static final int web_error = 5;

  public static final int deflate = 1;
  public static final int x_plain = 2;

  static String contentDescription[] = {"unknown", "dods_das", "dods_dds", "dods_data", "dods_error", "web_error"};

  static String encoding[] = {"unknown", "deflate", "x-plain"};


  public static void setMimeText(OutputStream os, int desc, String version, int enc) {
    PrintWriter ps = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));

    setMimeText(ps, desc, version, enc);
  }

  public static void setMimeText(PrintWriter ps, int desc, String version, int enc) {
    ps.println("HTTP/1.0 200 OK");
    ps.println("XDODS-Server: " + version);
    ps.println("Content-type: text/plain");
    ps.println("Content-Description: " + contentDescription[desc]);
    // Don't write a Content-Encoding header for x-plain since that breaks
    // Netscape on NT. jhrg 3/23/97
    if (enc != x_plain)
      ps.println("Content-Encoding: " + encoding[enc]);
    ps.println("");
  }

  public static void setMimeBinary(OutputStream os, int desc, String version, int enc) {
    PrintWriter ps = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    setMimeBinary(ps, desc, version, enc);
  }

  public static void setMimeBinary(PrintWriter ps, int desc, String version, int enc) {
    ps.println("HTTP/1.0 200 OK");
    ps.println("XDODS-Server: " + version);
    ps.println("Content-type: application/octet-stream");
    ps.println("Content-Description: " + contentDescription[desc]);
    // Don't write a Content-Encoding header for x-plain since that breaks
    // Netscape on NT. jhrg 3/23/97
    if (enc != x_plain)
      ps.println("Content-Encoding: " + encoding[enc]);
    ps.println("");
  }

  public static void setMimeError(OutputStream os, int code, String reason, String version) {
    PrintWriter ps = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    setMimeError(ps, code, reason, version);
  }

  public static void setMimeError(PrintWriter ps, int code, String reason, String version) {
    ps.println("HTTP/1.0 " + code + " " + reason);
    ps.println("XDODS-Server: " + version);
    ps.println("");
  }

}


