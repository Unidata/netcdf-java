/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.http;

import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.io.RemoteRandomAccessFile;
import ucar.unidata.io.spi.RandomAccessFileProvider;
import ucar.unidata.util.Urlencoded;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Gives access to files over HTTP, using "Accept-Ranges" HTTP header to do random access. */
public final class RafHttp extends RemoteRandomAccessFile {

  // deprecate ucar.unidata.io.http.maxHttpBufferSize
  // simplifies determination of buffer size, which might not need to be
  // so complex now that we have a read cache.
  // however, if it is set, use it (but change default to defaultRemoteFileBufferSize)
  private static final int maxHttpBufferSize = Integer.parseInt(
      System.getProperty("ucar.unidata.io.http.maxHttpBufferSize", String.valueOf(defaultRemoteFileBufferSize)));
  // if ucar.unidata.io.http.httpBufferSize is set, use it over the deprecated ucar.unidata.io.http.maxHttpBufferSize
  private static final int httpBufferSize =
      Integer.parseInt(System.getProperty("ucar.unidata.io.http.httpBufferSize", String.valueOf(maxHttpBufferSize)));

  private static final long httpMaxCacheSize = Long
      .parseLong(System.getProperty("ucar.unidata.io.http.maxReadCacheSize", String.valueOf(defaultMaxReadCacheSize)));

  private static final boolean debug = false;

  ///////////////////////////////////////////////////////////////////////////////////

  private HttpClient session;
  private long total_length;

  public RafHttp(String url) throws IOException {
    this(url, httpBufferSize, httpMaxCacheSize);
  }

  @Urlencoded
  public RafHttp(String url, int bufferSize, long maxRemoteCacheSize) throws IOException {
    super(url, bufferSize, maxRemoteCacheSize);

    if (debugLeaks)
      allFiles.add(location);

    HttpRequest request = HttpService.standardGetRequestBuilder(url).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    HttpHeaders responseHeaders = response.headers();

    boolean needtest = true;
    Optional<String> acceptRangesOpt = responseHeaders.firstValue("Accept-Ranges");
    if (acceptRangesOpt.isPresent()) {
      String acceptRanges = acceptRangesOpt.get();
      if (acceptRanges.equalsIgnoreCase("bytes")) {
        needtest = false;
      } else if (acceptRanges.equalsIgnoreCase("none")) {
        throw new IOException("Server does not support byte Ranges");
      }
    }

    try {
      this.total_length = responseHeaders.firstValue("Content-Length").map(Long::parseLong)
          .orElseThrow(() -> new IOException("Server does not support Content-Length"));
    } catch (NumberFormatException e) {
      throw new IOException("Server has malformed Content-Length header");
    }

    /*
     * Some HTTP server report 0 bytes length.
     * Do the Range bytes test if the server is reporting 0 bytes length
     */
    if (total_length == 0) {
      needtest = true;
    }

    if (needtest && !rangeOk(url)) {
      throw new IOException("Server does not support byte Ranges");
    }

    if (debugLeaks) {
      openFiles.add(location);
    }
  }

  private boolean rangeOk(String url) throws IOException {
    HttpRequest request = HttpService.standardGetRequestBuilder(url).header("Range", "bytes=0-0").build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    int code = response.statusCode();
    if (code != 206) {
      throw new IOException("Server does not support Range requests, statusCode= " + code);
    }

    HttpHeaders responseHeaders = response.headers();
    String rangeHeader = responseHeaders.firstValue("Content-Range")
        .orElseThrow(() -> new IOException("Server does not support Content-Length"));
    String totalLengthString = rangeHeader.substring(rangeHeader.lastIndexOf("/") + 1);
    try {
      this.total_length = Long.parseLong(totalLengthString);
    } catch (NumberFormatException e) {
      throw new IOException("Server has malformed Content-Range header =" + rangeHeader);
    }
    return true;
  }

  /**
   * Read directly from remote file, without going through the buffer.
   *
   * @param pos start here in the file
   * @param buff put data into this buffer
   * @param offset buffer offset
   * @param len this number of bytes
   * @return actual number of bytes read
   * @throws IOException on io error
   */
  @Override
  public int readRemote(long pos, byte[] buff, int offset, int len) throws IOException {
    long end = pos + len - 1;
    if (end >= total_length) {
      end = total_length - 1;
    }

    if (debug) {
      System.out.println(" HTTPRandomAccessFile bytes=" + pos + "-" + end + ": ");
    }

    HttpRequest request =
        HttpService.standardGetRequestBuilder(url).header("Range", String.format("bytes=%d-%d", pos, end)).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);
    int code = response.statusCode();
    if (code != 206) {
      throw new IOException("Server does not support Range requests, statusCode= " + code);
    }

    HttpHeaders responseHeaders = response.headers();
    int readLen;
    try {
      readLen = responseHeaders.firstValue("Content-Length").map(Integer::parseInt)
          .orElseThrow(() -> new IOException("Server did not return a Content-Length"));
    } catch (NumberFormatException e) {
      throw new IOException("Server has malformed Content-Length header");
    }

    readLen = Math.min(len, readLen);

    readLen = copy(response.body(), buff, offset, readLen);
    return readLen;
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

  ////////////////////////////////////////////////////////////////////////////////////
  // override selected RandomAccessFile public methods

  @Override
  public void closeRemote() {
    if (debugLeaks) {
      openFiles.remove(location);
    }

    if (session != null) {
      session = null;
    }
  }

  @Override
  public long length() {
    long fileLength = total_length;
    return Math.max(fileLength, dataEnd);
  }

  /**
   * Always returns {@code 0L}, as we cannot easily determine the last time that a remote file was modified.
   *
   * @return {@code 0L}, always.
   */
  // An idea of how we might implement this: https://github.com/Unidata/thredds/pull/479#issuecomment-194562614
  @Override
  public long getLastModified() {
    return 0;
  }

  /**
   * Hook into service provider interface for RandomAccessFileProvider.
   */
  public static class Provider implements RandomAccessFileProvider {

    private static final List<String> possibleSchemes = Arrays.asList("http", "https", "nodods", "httpserver");

    @Override
    public boolean isOwnerOf(String location) {
      String scheme = location.split(":")[0].toLowerCase();
      return possibleSchemes.contains(scheme);
    }

    @Override
    public RandomAccessFile open(String location) throws IOException {
      String scheme = location.split(":")[0];
      if (!scheme.equalsIgnoreCase("https")) {
        location = location.replace(scheme, "https");
      }
      return new RafHttp(location);
    }
  }
}
