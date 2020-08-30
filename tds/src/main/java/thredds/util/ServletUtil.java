/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ucar.nc2.internal.util.EscapeStrings;
import ucar.nc2.util.IO;
import ucar.unidata.io.RandomAccessFile;

public class ServletUtil {
  public static final org.slf4j.Logger logServerStartup = org.slf4j.LoggerFactory.getLogger("serverStartup");
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServletUtil.class);

  /**
   * Return the file path dealing with leading and trailing path
   * seperators (which must be a slash ("/")) for the given directory
   * and file paths.
   * <p/>
   * Note: Dealing with path strings is fragile.
   * ToDo: Switch from using path strings to java.io.Files.
   *
   * @param dirPath the directory path.
   * @param filePath the file path.
   * @return a full file path with the given directory and file paths.
   */
  public static String formFilename(String dirPath, String filePath) {
    if ((dirPath == null) || (filePath == null))
      return null;

    if (filePath.startsWith("/"))
      filePath = filePath.substring(1);

    return dirPath.endsWith("/") ? dirPath + filePath : dirPath + "/" + filePath;
  }

  /**
   * Write a file to the response stream. Handles Range requests.
   *
   * @param req request
   * @param res response
   * @param file must exist and not be a directory
   * @param contentType must not be null
   * @throws IOException or error
   */
  public static void returnFile(HttpServletRequest req, HttpServletResponse res, File file, String contentType)
      throws IOException {
    res.setContentType(contentType);
    res.addDateHeader("Last-Modified", file.lastModified());
    // res.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");

    // see if its a Range Request
    boolean isRangeRequest = false;
    long startPos = 0, endPos = Long.MAX_VALUE;
    String rangeRequest = req.getHeader("Range");
    if (rangeRequest != null) { // bytes=12-34 or bytes=12-
      int pos = rangeRequest.indexOf("=");
      if (pos > 0) {
        int pos2 = rangeRequest.indexOf("-");
        if (pos2 > 0) {
          String startString = rangeRequest.substring(pos + 1, pos2);
          String endString = rangeRequest.substring(pos2 + 1);
          startPos = Long.parseLong(startString);
          if (endString.length() > 0)
            endPos = Long.parseLong(endString) + 1;
          isRangeRequest = true;
        }
      }
    }

    // set content length
    long fileSize = file.length();
    long contentLength = fileSize;
    if (isRangeRequest) {
      endPos = Math.min(endPos, fileSize);
      contentLength = endPos - startPos;
    }

    // when compression is turned on, ContentLength has to be overridden
    // this is also true for HEAD, since this must be the same as GET without the body
    if (contentLength > Integer.MAX_VALUE)
      res.addHeader("Content-Length", Long.toString(contentLength)); // allow content length > MAX_INT
    else
      res.setContentLength((int) contentLength);

    String filename = file.getPath();
    // indicate we allow Range Requests
    res.addHeader("Accept-Ranges", "bytes");

    if (req.getMethod().equals("HEAD")) {
      return;
    }

    try {

      if (isRangeRequest) {
        // set before content is sent
        res.addHeader("Content-Range", "bytes " + startPos + "-" + (endPos - 1) + "/" + fileSize);
        res.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

        try (RandomAccessFile craf = RandomAccessFile.acquire(filename)) {
          IO.copyRafB(craf, startPos, contentLength, res.getOutputStream(), new byte[60000]);
          return;
        }
      }

      // Return the file
      ServletOutputStream out = res.getOutputStream();
      IO.copyFileB(file, out, 60 * 1000);
      /*
       * try (WritableByteChannel cOut = Channels.newChannel(out)) {
       * IO.copyFileWithChannels(file, cOut);
       * res.flushBuffer();
       * }
       */
    }

    // @todo Split up this exception handling: those from file access vs those from dealing with response
    // File access: catch and res.sendError()
    // response: don't catch (let bubble up out of doGet() etc)
    catch (FileNotFoundException e) {
      log.error("returnFile(): FileNotFoundException= " + filename);
      if (!res.isCommitted())
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
    } catch (java.net.SocketException e) {
      log.info("returnFile(): SocketException sending file: " + filename + " " + e.getMessage());
    } catch (IOException e) {
      String eName = e.getClass().getName(); // dont want compile time dependency on ClientAbortException
      if (eName.equals("org.apache.catalina.connector.ClientAbortException")) {
        log.debug("returnFile(): ClientAbortException while sending file: " + filename + " " + e.getMessage());
        return;
      }

      if (e.getMessage().startsWith("File transfer not complete")) { // coming from FileTransfer.transferTo()
        log.debug("returnFile() " + e.getMessage());
        return;
      }

      log.error("returnFile(): IOException (" + e.getClass().getName() + ") sending file ", e);
      if (!res.isCommitted())
        res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending file: " + e.getMessage());
    }
  }

  /**
   * Send given content string as the HTTP response.
   *
   * @param contents the string to return as the HTTP response.
   * @param res the HttpServletResponse
   * @throws IOException if an I/O error occurs while writing the response.
   */
  public static void returnString(String contents, HttpServletResponse res) throws IOException {

    try {
      ServletOutputStream out = res.getOutputStream();
      IO.copy(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), out);
    } catch (IOException e) {
      log.error(" IOException sending string: ", e);
      res.sendError(HttpServletResponse.SC_NOT_FOUND, "Problem sending string: " + e.getMessage());
    }
  }

  /**
   * Set the proper content length for the string
   *
   * @param response the HttpServletResponse to act upon
   * @param s the string that will be returned
   * @return the number of bytes
   * @throws UnsupportedEncodingException on bad character encoding
   */
  public static int setResponseContentLength(HttpServletResponse response, String s)
      throws UnsupportedEncodingException {
    int length = s.getBytes(response.getCharacterEncoding()).length;
    response.setContentLength(length);
    return length;
  }

  /**
   * Return the request URL relative to the server (i.e., starting with the context path).
   *
   * @param req request
   * @return URL relative to the server
   */
  public static String getReletiveURL(HttpServletRequest req) {
    return req.getContextPath() + req.getServletPath() + req.getPathInfo();
  }

  /**
   * Forward this request to the CatalogServices servlet ("/catalog.html").
   *
   * @param req request
   * @param res response
   * @throws IOException on IO error
   * @throws ServletException other error
   */
  public static void forwardToCatalogServices(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {

    String reqs = "catalog=" + getReletiveURL(req);
    String query = req.getQueryString();
    if (query != null)
      reqs = reqs + "&" + query;
    log.info("forwardToCatalogServices(): request string = \"/catalog.html?" + reqs + "\"");

    // dispatch to CatalogHtml servlet
    RequestForwardUtils.forwardRequestRelativeToCurrentContext("/catalog.html?" + reqs, req, res);
  }

  static public void showSystemProperties(PrintStream out) {

    Properties sysp = System.getProperties();
    Enumeration e = sysp.propertyNames();
    List<String> list = Collections.list(e);
    Collections.sort(list);

    out.println("System Properties:");
    for (String name : list) {
      String value = System.getProperty(name);
      out.println("  " + name + " = " + value);
    }
    out.println();
  }

  /**
   * This is the server part, eg http://motherlode:8080
   *
   * @param req the HttpServletRequest
   * @return request server
   */
  public static String getRequestServer(HttpServletRequest req) {
    return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
  }

  /**
   * This is everything except the query string
   *
   * @param req the HttpServletRequest
   * @return parsed request base
   */
  public static String getRequestBase(HttpServletRequest req) {
    // return "http://"+req.getServerName()+":"+ req.getServerPort()+req.getRequestURI();
    return req.getRequestURL().toString();
  }

  /**
   * The request base as a URI
   *
   * @param req the HttpServletRequest
   * @return parsed request as a URI
   */
  public static URI getRequestURI(HttpServletRequest req) {
    try {
      return new URI(getRequestBase(req));
    } catch (URISyntaxException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * servletPath + pathInfo
   *
   * @param req the HttpServletRequest
   * @return parsed request servletPath + pathInfo
   */
  public static String getRequestPath(HttpServletRequest req) {
    StringBuilder buff = new StringBuilder();
    if (req.getServletPath() != null)
      buff.append(req.getServletPath());
    if (req.getPathInfo() != null)
      buff.append(req.getPathInfo());
    return buff.toString();
  }

  /**
   * The entire request including query string
   *
   * @param req the HttpServletRequest
   * @return entire parsed request
   */
  public static String getRequest(HttpServletRequest req) {
    String query = req.getQueryString();
    return getRequestBase(req) + (query == null ? "" : "?" + query);
  }

  /**
   * Return the value of the given parameter for the given request. Should
   * only be used if the parameter is known to only have one value. If used
   * on a multi-valued parameter, the first value is returned.
   *
   * @param req the HttpServletRequest
   * @param paramName the name of the parameter to find.
   * @return the value of the given parameter for the given request.
   */
  public static String getParameterIgnoreCase(HttpServletRequest req, String paramName) {
    Enumeration e = req.getParameterNames();
    while (e.hasMoreElements()) {
      String s = (String) e.nextElement();
      if (s.equalsIgnoreCase(paramName))
        return req.getParameter(s);
    }
    return null;
  }


  /**
   * Show details about the request
   *
   * @param req the request
   * @return string showing the details of the request.
   */
  static public String showRequestDetail(HttpServletRequest req) {
    StringBuilder sbuff = new StringBuilder();

    sbuff.append("Request Info\n");
    sbuff.append(" req.getServerName(): ").append(req.getServerName()).append("\n");
    sbuff.append(" req.getServerPort(): ").append(req.getServerPort()).append("\n");
    sbuff.append(" req.getContextPath:").append(req.getContextPath()).append("\n");
    sbuff.append(" req.getServletPath:").append(req.getServletPath()).append("\n");
    sbuff.append(" req.getPathInfo:").append(req.getPathInfo()).append("\n");
    sbuff.append(" req.getQueryString:").append(req.getQueryString()).append("\n");
    sbuff.append(" getQueryStringDecoded:").append(EscapeStrings.urlDecode(req.getQueryString())).append("\n");
    /*
     * try {
     * sbuff.append(" getQueryStringDecoded:").append(URLDecoder.decode(req.getQueryString(), "UTF-8")).append("\n");
     * } catch (UnsupportedEncodingException e1) {
     * e1.printStackTrace();
     * }
     */
    sbuff.append(" req.getRequestURI:").append(req.getRequestURI()).append("\n");
    sbuff.append(" getRequestBase:").append(getRequestBase(req)).append("\n");
    sbuff.append(" getRequestServer:").append(getRequestServer(req)).append("\n");
    sbuff.append(" getRequest:").append(getRequest(req)).append("\n");
    sbuff.append("\n");

    sbuff.append(" req.getPathTranslated:").append(req.getPathTranslated()).append("\n");
    sbuff.append("\n");
    sbuff.append(" req.getScheme:").append(req.getScheme()).append("\n");
    sbuff.append(" req.getProtocol:").append(req.getProtocol()).append("\n");
    sbuff.append(" req.getMethod:").append(req.getMethod()).append("\n");
    sbuff.append("\n");
    sbuff.append(" req.getContentType:").append(req.getContentType()).append("\n");
    sbuff.append(" req.getContentLength:").append(req.getContentLength()).append("\n");

    sbuff.append(" req.getRemoteAddr():").append(req.getRemoteAddr());
    try {
      sbuff.append(" getRemoteHost():").append(java.net.InetAddress.getByName(req.getRemoteHost()).getHostName())
          .append("\n");
    } catch (java.net.UnknownHostException e) {
      sbuff.append(" getRemoteHost():").append(e.getMessage()).append("\n");
    }
    sbuff.append(" getRemoteUser():").append(req.getRemoteUser()).append("\n");

    sbuff.append("\n");
    sbuff.append("Request Parameters:\n");
    Enumeration params = req.getParameterNames();
    while (params.hasMoreElements()) {
      String name = (String) params.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          sbuff.append("  ").append(name).append("  (").append(i).append("): ").append(values[i]).append("\n");
        }
      }
    }
    sbuff.append("\n");

    sbuff.append("Request Headers:\n");
    Enumeration names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      Enumeration values = req.getHeaders(name); // support multiple values
      if (values != null) {
        while (values.hasMoreElements()) {
          String value = (String) values.nextElement();
          sbuff.append("  ").append(name).append(": ").append(value).append("\n");
        }
      }
    }
    sbuff.append(" ------------------\n");

    return sbuff.toString();
  }

  static public String showRequestHeaders(HttpServletRequest req) {
    StringBuilder sbuff = new StringBuilder();
    sbuff.append("Request Headers:\n");
    Enumeration names = req.getHeaderNames();
    while (names.hasMoreElements()) {
      String name = (String) names.nextElement();
      Enumeration values = req.getHeaders(name); // support multiple values
      if (values != null) {
        while (values.hasMoreElements()) {
          String value = (String) values.nextElement();
          sbuff.append("  ").append(name).append(": ").append(value).append("\n");
        }
      }
    }
    return sbuff.toString();
  }

}
