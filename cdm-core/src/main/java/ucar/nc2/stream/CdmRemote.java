/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.stream;

import com.google.common.base.Stopwatch;

import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import ucar.array.ArrayType;
import ucar.nc2.internal.http.HttpService;
import ucar.ma2.*;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import java.io.*;
import java.util.Formatter;
import java.util.concurrent.TimeUnit;

/**
 * A remote CDM dataset (extending NetcdfFile), using cdmremote protocol to communicate.
 * Similar to Opendap in that it is a remote access protocol using indexed data access.
 * Supports full CDM / netcdf-4 data model.
 */
public class CdmRemote extends ucar.nc2.NetcdfFile {
  public static final String PROTOCOL = "cdmremote";
  public static final String SCHEME = PROTOCOL + ":";

  private static boolean showRequest;
  private static boolean compress;

  public static void setDebugFlags(ucar.nc2.util.DebugFlags debugFlag) {
    showRequest = debugFlag.isSet("CdmRemote/showRequest");
  }

  public static void setAllowCompression(boolean b) {
    compress = b;
  }

  /**
   * Create the canonical form of the URL.
   * If the urlName starts with "http:", change it to start with "cdmremote:", otherwise
   * leave it alone.
   *
   * @param urlName the url string
   * @return canonical form
   */
  public static String canonicalURL(String urlName) {
    if (urlName.startsWith("http:")) {
      return SCHEME + urlName.substring(5);
    } else if (urlName.startsWith("https:")) {
      return SCHEME + urlName.substring(6);
    }
    return urlName;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////

  @Override
  protected Array readData(ucar.nc2.Variable v, Section section) throws IOException {
    // if (unlocked)
    // throw new IllegalStateException("File is unlocked - cannot use");

    if (v.getDataType() == DataType.SEQUENCE) {
      Structure s = (Structure) v;
      StructureDataIterator siter = s.getStructureIterator(-1);
      return new ArraySequence(s.makeStructureMembers(), siter, -1);
    }

    Formatter f = new Formatter();
    f.format("%s?req=data", remoteURI);
    if (compress) {
      f.format("&deflate=5");
    }
    f.format("&var=%s", NetcdfFiles.makeFullName(v));
    if ((section != null) && (section.computeSize() != v.getSize()) && (v.getDataType() != DataType.SEQUENCE)) {
      f.format("(%s)", section.toString());
    }
    String url = f.toString();

    /*
     * LOOK need to escape ?? String escapedURI = f.toString();
     * URI escapedURI;
     * try {
     * escapedURI = HTTPUtil.parseToURI(url);
     * } catch (URISyntaxException e) {
     * throw new RuntimeException(e);
     * }
     */

    if (showRequest) {
      System.out.printf("CdmRemote data request for variable: '%s' section=(%s)%n url='%s'%n esc='%s'%n",
          v.getFullName(), section, url, url);
    }

    HttpRequest request = HttpService.standardGetRequestBuilder(url).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);

    HttpHeaders responseHeaders = response.headers();
    Optional<String> contentLength = responseHeaders.firstValue("Content-Length");
    if (contentLength.isPresent()) {
      int readLen = Integer.parseInt(contentLength.get());
      if (showRequest) {
        System.out.printf(" content-length = %d%n", readLen);
      }
      if (v.getArrayType() != ArrayType.SEQUENCE) {
        int wantSize = (int) (v.getElementSize() * (section == null ? v.getSize() : section.computeSize()));
        if (readLen != wantSize) {
          throw new IOException("content-length= " + readLen + " not equal expected Size= " + wantSize); // LOOK
        }
      }
    }

    NcStreamReader reader = new NcStreamReader();
    NcStreamReader.DataResult result = reader.readData(response.body(), this, remoteURI);

    assert NetcdfFiles.makeFullName(v).equals(result.varNameFullEsc);
    return result.data;
  }

  @Override
  protected StructureDataIterator getStructureIterator(Structure s, int bufferSize) {
    try {
      InputStream is = sendQuery(remoteURI, NetcdfFiles.makeFullName(s));
      NcStreamReader reader = new NcStreamReader();
      return reader.getStructureIterator(is, this);

    } catch (Throwable e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  // session may be null, if so, will be closed on method.close()
  public static InputStream sendQuery(String remoteURI, String query) throws IOException {
    Stopwatch stopwatch = Stopwatch.createStarted();

    StringBuilder sbuff = new StringBuilder(remoteURI);
    sbuff.append("?");
    sbuff.append(query);
    if (showRequest) {
      System.out.printf(" CdmRemote sendQuery= %s", sbuff);
    }
    String url = sbuff.toString();

    HttpRequest request = HttpService.standardGetRequestBuilder(url).build();
    HttpResponse<InputStream> response = HttpService.standardRequest(request);

    if (showRequest) {
      System.out.printf(" CdmRemote request %s took %d msecs %n", url, stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
    return response.body();
  }

  @Override
  public String getFileTypeId() {
    return "ncstreamRemote";
  }

  @Override
  public String getFileTypeDescription() {
    return "ncstreamRemote";
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final String remoteURI;

  private CdmRemote(Builder<?> builder) {
    super(builder);
    this.remoteURI = builder.remoteURI;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  private Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setRemoteURI(this.remoteURI);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /**
   * Get Builder for this class that allows subclassing.
   *
   * @see "https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses"
   */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends Builder<T>> extends NetcdfFile.Builder<T> {
    private String remoteURI;
    private boolean built;

    protected abstract T self();

    public T setRemoteURI(String remoteURI) {
      this.remoteURI = remoteURI;
      return self();
    }

    public CdmRemote build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      read();
      return new CdmRemote(this);
    }

    private void read() {
      Stopwatch stopwatch = Stopwatch.createStarted();

      // get http URL
      String temp = this.remoteURI;
      try {
        if (temp.startsWith(SCHEME)) {
          temp = temp.substring(SCHEME.length());
        } else if (!(temp.startsWith("http:") | temp.startsWith("https:"))) {
          temp = "http:" + temp;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      this.remoteURI = temp;

      try {
        // get the header
        String url = remoteURI + "?req=header";
        if (showRequest) {
          System.out.printf(" CdmRemote request %s%n", url);
        }
        HttpRequest request = HttpService.standardGetRequestBuilder(url).build();
        HttpResponse<InputStream> response = HttpService.standardRequest(request);

        NcStreamReader reader = new NcStreamReader();
        reader.readHeader(response.body(), this);
        this.location = SCHEME + remoteURI;

        if (showRequest) {
          System.out.printf(" CdmRemote request %s took %d msecs %n", url, stopwatch.elapsed(TimeUnit.MILLISECONDS));
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

  }

}
