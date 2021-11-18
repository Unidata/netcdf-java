/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io.http;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import ucar.nc2.util.IO;

/**
 * Note: checks for gzip. Shouldnt that be handled automatically?
 * Static utility methods that read from URLs, using URLConnection.
 * Could replace with java.net.http.HttpClient. Only used in ToolUI?
 */
public class ReadFromUrl {
  private static final boolean showStackTrace = false;
  private static final boolean showHeaders = false;

  /**
   * copy contents of URL to output stream, specify internal buffer size. request gzip encoding
   *
   * @param urlString copy the contents of this URL
   * @param out copy to this stream. If null, throw bytes away
   * @param bufferSize internal buffer size.
   * @return number of bytes copied
   * @throws java.io.IOException on io error
   */
  public static long copyUrlB(String urlString, OutputStream out, int bufferSize) throws IOException {
    long count;
    URL url;

    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("** MalformedURLException on URL <" + urlString + ">\n" + e.getMessage() + "\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();
      java.net.HttpURLConnection httpConnection = null;
      if (connection instanceof java.net.HttpURLConnection) {
        httpConnection = (java.net.HttpURLConnection) connection;
        httpConnection.addRequestProperty("Accept-Encoding", "gzip");
      }

      if (showHeaders) {
        showRequestHeaders(urlString, connection);
      }

      // get response
      if (httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode + "\n"
              + httpConnection.getResponseMessage() + "\n");
      }

      if (showHeaders && (httpConnection != null)) {
        int code = httpConnection.getResponseCode();
        String response = httpConnection.getResponseMessage();

        // response headers
        System.out.println("\nRESPONSE for " + urlString + ": ");
        System.out.println(" HTTP/1.x " + code + " " + response);
        System.out.println("Headers: ");

        for (int j = 1;; j++) {
          String header = connection.getHeaderField(j);
          String key = connection.getHeaderFieldKey(j);
          if (header == null || key == null)
            break;
          System.out.println(" " + key + ": " + header);
        }
      }

      // read it
      try (InputStream is = connection.getInputStream()) {
        BufferedInputStream bis;

        // check if its gzipped
        if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
          bis = new BufferedInputStream(new GZIPInputStream(is), 8000);
        } else {
          bis = new BufferedInputStream(is, 8000);
        }

        if (out == null)
          count = IO.copy2null(bis, bufferSize);
        else
          count = IO.copyB(bis, out, bufferSize);
      }

    } catch (java.net.ConnectException e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException(
          "** ConnectException on URL: <" + urlString + ">\n" + e.getMessage() + "\nServer probably not running");

    } catch (java.net.UnknownHostException e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException("** UnknownHostException on URL: <" + urlString + ">\n");

    } catch (Exception e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException("** Exception on URL: <" + urlString + ">\n" + e);
    }

    return count;
  }

  private static void showRequestHeaders(String urlString, java.net.URLConnection connection) {
    System.out.println("\nREQUEST Properties for " + urlString + ": ");
    Map<String, List<String>> reqs = connection.getRequestProperties();
    for (Map.Entry<String, List<String>> entry : reqs.entrySet()) {
      System.out.printf(" %s:", entry.getKey());
      for (String v : entry.getValue())
        System.out.printf("%s,", v);
      System.out.printf("%n");
    }
  }

  /**
   * get input stream from URL
   *
   * @param urlString URL
   * @return input stream, unzipped if needed
   * @throws java.io.IOException on io error
   */
  public static InputStream getInputStreamFromUrl(String urlString) throws IOException {
    URL url;

    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      throw new IOException("** MalformedURLException on URL <" + urlString + ">\n" + e.getMessage() + "\n");
    }

    try {
      java.net.URLConnection connection = url.openConnection();
      java.net.HttpURLConnection httpConnection = null;
      if (connection instanceof java.net.HttpURLConnection) {
        httpConnection = (java.net.HttpURLConnection) connection;
        httpConnection.addRequestProperty("Accept-Encoding", "gzip");
      }

      if (showHeaders) {
        showRequestHeaders(urlString, connection);
      }

      // get response
      if (httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode + "\n"
              + httpConnection.getResponseMessage() + "\n");
      }

      if (showHeaders && (httpConnection != null)) {
        int code = httpConnection.getResponseCode();
        String response = httpConnection.getResponseMessage();

        // response headers
        System.out.println("\nRESPONSE for " + urlString + ": ");
        System.out.println(" HTTP/1.x " + code + " " + response);
        System.out.println("Headers: ");

        for (int j = 1;; j++) {
          String header = connection.getHeaderField(j);
          String key = connection.getHeaderFieldKey(j);
          if (header == null || key == null)
            break;
          System.out.println(" " + key + ": " + header);
        }
      }

      java.io.InputStream is = null;
      try {
        // read it
        is = connection.getInputStream();

        // check if its gzipped
        if ("gzip".equalsIgnoreCase(connection.getContentEncoding())) {
          is = new BufferedInputStream(new GZIPInputStream(is), 1000);
        }
      } catch (Throwable t) {
        if (is != null)
          is.close();
      }
      return is;

    } catch (java.net.ConnectException e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException(
          "** ConnectException on URL: <" + urlString + ">\n" + e.getMessage() + "\nServer probably not running");

    } catch (java.net.UnknownHostException e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException("** UnknownHostException on URL: <" + urlString + ">\n");

    } catch (Exception e) {
      if (showStackTrace)
        e.printStackTrace();
      throw new IOException("** Exception on URL: <" + urlString + ">\n" + e);
    }
  }

  /**
   * Read the contents from the named URL and place into a String.
   *
   * @param urlString the URL to read from.
   * @return String holding the contents.
   * @throws IOException if fails
   */
  public static String readURLcontents(String urlString) throws IOException {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
    copyUrlB(urlString, bout, 20000);
    return bout.toString(StandardCharsets.UTF_8);
  }

}
