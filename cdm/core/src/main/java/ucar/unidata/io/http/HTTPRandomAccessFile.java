/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.io.http;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;
import ucar.httpservices.HTTPSession;
import ucar.unidata.util.Urlencoded;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Gives access to files over HTTP, using "Accept-Ranges" HTTP header to do random access.
 *
 * @author John Caron, based on work by Donald Denbo
 */

public class HTTPRandomAccessFile extends ucar.unidata.io.RandomAccessFile {
  public static final int defaultHTTPBufferSize = 20 * 1000; // 20K
  public static final int maxHTTPBufferSize = // was 10 * 1000 * 1000 - 10 M
      Integer.parseInt(System.getProperty("ucar.unidata.io.http.maxHttpBufferSize", "10000000"));
  private static final boolean debug = false, debugDetails = false;

  ///////////////////////////////////////////////////////////////////////////////////

  private String url;
  private HTTPSession session;
  private long total_length;

  public HTTPRandomAccessFile(String url) throws IOException {
    this(url, defaultHTTPBufferSize);
    location = url;
  }

  @Urlencoded
  public HTTPRandomAccessFile(String url, int bufferSize) throws IOException {
    super(bufferSize);
    file = null;
    this.url = url;
    location = url;

    if (debugLeaks)
      allFiles.add(location);

    session = HTTPFactory.newSession(url);

    boolean needtest = true;

    try (HTTPMethod method = HTTPFactory.Head(session, url)) {

      doConnect(method);

      Optional<String> acceptRangesOpt = method.getResponseHeaderValue("Accept-Ranges");
      if (!acceptRangesOpt.isPresent()) {
        needtest = true; // header is optional - need more testing

      } else {
        String acceptRanges = acceptRangesOpt.get();
        if (acceptRanges.equalsIgnoreCase("bytes")) {
          needtest = false;

        } else if (acceptRanges.equalsIgnoreCase("none")) {
          throw new IOException("Server does not support byte Ranges");
        }
      }

      try {
        this.total_length = method.getResponseHeaderValue("Content-Length").map(Long::parseLong)
            .orElseThrow(() -> new IOException("Server does not support Content-Length"));
      } catch (NumberFormatException e) {
        throw new IOException("Server has malformed Content-Length header");
      }
    }

    /*
     * Some HTTP server report 0 bytes length.
     * Do the Range bytes test if the server is reporting 0 bytes length
     */
    if (total_length == 0)
      needtest = true;

    if (needtest && !rangeOk(url))
      throw new IOException("Server does not support byte Ranges");

    if (total_length > 0) {
      // this means that we will read the file in one gulp then deal with it in memory
      int useBuffer = (int) Math.min(total_length, maxHTTPBufferSize); // entire file size if possible
      useBuffer = Math.max(useBuffer, defaultHTTPBufferSize); // minimum buffer
      setBufferSize(useBuffer);
    }

    if (debugLeaks)
      openFiles.add(location);
  }

  public void close() {
    if (debugLeaks)
      openFiles.remove(location);

    if (session != null) {
      session.close();
      session = null;
    }
  }

  private boolean rangeOk(String url) {
    try (HTTPMethod method = HTTPFactory.Get(session, url)) {
      method.setRange(0, 0);
      doConnect(method);
      int code = method.getStatusCode();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= " + code);

      this.total_length =
          method.getResponseHeaderValue("Content-Range").map(r -> Long.parseLong(r.substring(r.lastIndexOf("/") + 1)))
              .orElseThrow(() -> new IOException("Server did not return a Content-Range"));
      return true;

    } catch (IOException e) {
      return false;
    }
  }

  private void doConnect(HTTPMethod method) throws IOException {

    // Execute the method.
    int statusCode = method.execute();

    if (statusCode == 404)
      throw new FileNotFoundException(url + " " + method.getStatusLine());

    if (statusCode >= 300)
      throw new IOException(url + " " + method.getStatusLine());

    if (debugDetails) {
      // request headers dont seem to be available until after execute()
      printHeaders("Request: " + method.getURI(), method.getRequestHeaders().entries());
      printHeaders("Response: " + method.getStatusCode(), method.getResponseHeaders().entries());
    }
  }

  private void printHeaders(String title, Collection<Map.Entry<String, String>> headers) {
    if (headers.isEmpty())
      return;
    System.out.println(title);
    for (Map.Entry<String, String> entry : headers) {
      System.out.println(String.format("  %s = %s" + entry.getKey(), entry.getValue()));
    }
  }

  /**
   * Read directly from file, without going through the buffer.
   * All reading goes through here or readToByteChannel;
   *
   * @param pos start here in the file
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  protected int read_(long pos, byte[] buff, int offset, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length)
      end = total_length - 1;

    if (debug)
      System.out.println(" HTTPRandomAccessFile bytes=" + pos + "-" + end + ": ");

    try (HTTPMethod method = HTTPFactory.Get(session, url)) {
      method.setFollowRedirects(true);
      method.setRange(pos, end);
      doConnect(method);

      int code = method.getStatusCode();
      if (code != 206)
        throw new IOException("Server does not support Range requests, code= " + code);

      int readLen = method.getResponseHeaderValue("Content-Length").map(Integer::parseInt)
          .orElseThrow(() -> new IOException("Server did not return a Content-Length"));

      readLen = Math.min(len, readLen);

      InputStream is = method.getResponseAsStream();
      readLen = copy(is, buff, offset, readLen);
      return readLen;

    }
  }

  private int copy(InputStream in, byte[] buff, int offset, int want) throws IOException {
    int done = 0;
    while (want > 0) {
      int bytesRead = in.read(buff, offset + done, want);
      if (bytesRead == -1)
        break;
      done += bytesRead;
      want -= bytesRead;
    }
    return done;
  }

  @Override
  public long readToByteChannel(WritableByteChannel dest, long offset, long nbytes) throws IOException {
    int n = (int) nbytes;
    byte[] buff = new byte[n];
    int done = read_(offset, buff, 0, n);
    dest.write(ByteBuffer.wrap(buff));
    return done;
  }

  // override selected RandomAccessFile public methods

  @Override
  public long length() {
    long fileLength = total_length;
    if (fileLength < dataEnd)
      return dataEnd;
    else
      return fileLength;
  }

  /**
   * Always returns {@code 0L}, as we cannot easily determine the last time that a remote file was modified.
   *
   * @return {@code 0L}, always.
   */
  // LOOK: An idea of how we might implement this: https://github.com/Unidata/thredds/pull/479#issuecomment-194562614
  @Override
  public long getLastModified() {
    return 0;
  }
}
