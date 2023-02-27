/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.dap4lib;

import dap4.core.util.ChecksumMode;
import dap4.core.dmr.DapDataset;
import dap4.core.util.*;
import dap4.dap4lib.D4DSP;
import dap4.dap4lib.D4DataCompiler;
import org.apache.http.HttpStatus;
import ucar.httpservices.*;
import ucar.nc2.NetcdfFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Make a request to a server and convert the reply
 * to a DapDataset from the returned bytes.
 */

public class HttpDSP extends D4DSP {

  //////////////////////////////////////////////////
  // Constants

  protected static final boolean DEBUG = false;

  protected static final int DFALTPRELOADSIZE = 50000; // databuffer

  //////////////////////////////////////////////////
  // Static methods

  static public void setHttpDebug() {
    HTTPIntercepts.setGlobalDebugInterceptors(true);
  }

  //////////////////////////////////////////////////
  // Instance variables

  protected boolean allowCompression = true;
  protected int status = HttpStatus.SC_OK; // response

  //////////////////////////////////////////////////
  // Constructor(s)

  public HttpDSP() {
    super();
  }

  //////////////////////////////////////////////////
  // DSP API

  public D4DSP open(String fileurl, ChecksumMode cmode) throws DapException {
    super.open(fileurl, cmode);
    return this;
  }

  /**
   * @param url
   * @param context Any parameters that may help to decide.
   * @return true if this url appears to be processible by this DSP
   */
  public boolean dspMatch(String url, DapContext context) {
    return DapProtocol.isDap4URI(url);
  }

  /////////////////////////////////////////
  // AbstractDSP extensions

  /*
   * @Override
   * public String getPath()
   * {
   * return this.originalurl;
   * }
   */

  //////////////////////////////////////////////////
  // Load methods

  public void loadDMR() throws DapException {
    String methodurl = getMethodUrl(RequestMode.DMR, this.checksummode);
    try {
      InputStream stream = makeRequest(methodurl); // close later
      setStream(stream, RequestMode.DMR);
      super.loadDMR();
    } catch (IOException e) {
      throw new DapException(e);
    }
  }

  /**
   * This will be called lazily when trying to read data.
   *
   * @throws DapException
   */
  public void loadDAP() throws DapException {
    String methodurl = getMethodUrl(RequestMode.DAP, this.checksummode);
    try (InputStream stream = makeRequest(methodurl)) {
      // Extract and "compile" the server response, ignoring the leading DMR
      setStream(stream, RequestMode.DAP);
      super.loadDAP();
    } catch (IOException ioe) {
      throw new DapException(ioe);
    }
  }

  //////////////////////////////////////////////////
  // Request/Response methods

  /**
   * Open a connection and make a request for the (possibly constrained) DMR|DAP
   * (choice is made by url extension)
   * 
   * @throws DapException
   */

  protected InputStream makeRequest(String methodurl) throws DapException {
    // Assume mode is consistent with the url.
    InputStream stream;
    // Make the request and return the input stream for accessing the databuffer
    URI uri;
    try {
      uri = HTTPUtil.parseToURI(methodurl);
    } catch (URISyntaxException mue) {
      throw new DapException("Malformed url: " + methodurl);
    }
    long start = System.currentTimeMillis();
    long stop = 0;
    this.status = 0;
    HTTPMethod method = null; // Implicitly passed out to caller via stream
    try { // Note that we cannot use try with resources because we export the method stream, so method
      // must not be closed.
      method = HTTPFactory.Get(methodurl);
      if (allowCompression)
        method.setCompression("deflate,gzip");
      this.status = method.execute();
      if (this.status != HttpStatus.SC_OK) {
        String msg = method.getResponseAsString();
        throw new DapException("Request failure: " + status + ": " + methodurl).setCode(status);
      }
      // Get the response body stream => do not close the method
      return method.getResponseAsStream();
    } catch (HTTPException e) {
      if (method != null)
        method.close();
      throw new DapException(e);
    }
  }

  //////////////////////////////////////////////////
  // Utilities

  protected static String buildURL(String baseurl, String suffix, DapDataset template, String ce) {
    StringBuilder methodurl = new StringBuilder();
    methodurl.append(baseurl);
    if (suffix != null) {
      methodurl.append('.');
      methodurl.append(suffix);
    }
    if (ce != null && ce.length() > 0) {
      methodurl.append("?");
      methodurl.append(DapConstants.CONSTRAINTTAG);
      methodurl.append('=');
      methodurl.append(ce);
    }
    return methodurl.toString();
  }

}
