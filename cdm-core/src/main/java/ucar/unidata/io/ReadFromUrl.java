/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import ucar.nc2.util.IO;

/**
 * Note: checks for gzip. Shouldnt that be handled automatically?
 * Static utility methods that read from URLs, using URLConnection.
 * Could replace with java.net.http.HttpClient. Only used in ToolUI?
 */
public class ReadFromUrl {
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

      // get response
      if (httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode + "\n"
              + httpConnection.getResponseMessage() + "\n");
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

    } catch (Exception e) {
      throw new IOException("** Exception on URL:" + urlString, e);
    }

    return count;
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

      // get response
      if (httpConnection != null) {
        int responseCode = httpConnection.getResponseCode();
        if (responseCode / 100 != 2)
          throw new IOException("** Cant open URL <" + urlString + ">\n Response code = " + responseCode + "\n"
              + httpConnection.getResponseMessage() + "\n");
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

    } catch (Exception e) {
      throw new IOException("** Exception on URL:" + urlString, e);
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
