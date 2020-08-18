/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ui.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import ucar.nc2.util.IO;

/** Network utilities */
public class HttpUtils {

  /**
   * use HTTP PUT to send the contents to the named URL.
   *
   * @param urlString the URL to read from. must be http:
   * @param contents String holding the contents
   * @return a Result object; generally 0 <= code <=400 is ok
   */
  public static HttpResult putToURL(String urlString, String contents) {
    URL url;
    try {
      url = new URL(urlString);
    } catch (MalformedURLException e) {
      return new HttpResult(-1, "** MalformedURLException on URL (" + urlString + ")\n" + e.getMessage());
    }

    try {
      java.net.HttpURLConnection c = (HttpURLConnection) url.openConnection();
      c.setDoOutput(true);
      c.setRequestMethod("PUT");

      // write it
      try (OutputStream out = c.getOutputStream()) {
        BufferedOutputStream bout = new BufferedOutputStream(out);
        IO.copy(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)), bout);
      }

      int code = c.getResponseCode();
      String mess = c.getResponseMessage();
      return new HttpResult(code, mess);

    } catch (java.net.ConnectException e) {
      return new HttpResult(-2,
          "** ConnectException on URL: <" + urlString + ">\n" + e.getMessage() + "\nServer probably not running");

    } catch (IOException e) {
      return new HttpResult(-3, "** IOException on URL: (" + urlString + ")\n" + e.getMessage());
    }

  }

  /**
   * Holds the result of an HTTP action.
   */
  public static class HttpResult {
    public int statusCode;
    public String message;

    HttpResult(int code, String message) {
      this.statusCode = code;
      this.message = message;
    }
  }

}
