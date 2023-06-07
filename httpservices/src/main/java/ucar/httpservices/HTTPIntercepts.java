/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.http.*;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Package together all the HTTP Intercept code
 */

public class HTTPIntercepts {

  //////////////////////////////////////////////////
  // Constants
  static private final Logger logger = LoggerFactory.getLogger(HTTPIntercepts.class);

  // Allow printing to a variety of targets
  static abstract public interface Printer {
    public void print(String s);

    public void println(String s);
  }

  // Default printer
  static public Printer logprinter = new Printer() {
    public void print(String s) {
      logger.debug(s);
    }

    public void println(String s) {
      logger.debug(s);
    }
  };

  //////////////////////////////////////////////////
  // Inner classes

  abstract static class InterceptCommon {
    private static final Logger logger = LoggerFactory.getLogger(InterceptCommon.class);

    protected HttpContext context = null;
    protected List<Header> headers = new ArrayList<Header>();
    protected HttpRequest request = null;
    protected HttpResponse response = null;
    protected Printer printer = null;

    public InterceptCommon setPrint(Printer printer) {
      this.printer = printer;
      return this;
    }

    public void clear() {
      context = null;
      headers.clear();
      request = null;
      response = null;
    }

    public synchronized HttpRequest getRequest() {
      return this.request;
    }

    public synchronized HttpResponse getResponse() {
      return this.response;
    }

    public synchronized HttpContext getContext() {
      return this.context;
    }

    public synchronized HttpEntity getRequestEntity() {
      if (this.request != null && this.request instanceof HttpEntityEnclosingRequest) {
        return ((HttpEntityEnclosingRequest) this.request).getEntity();
      } else
        return null;
    }

    synchronized HttpEntity getResponseEntity() {
      if (this.response != null) {
        return this.response.getEntity();
      } else
        return null;
    }

    public synchronized List<Header> getHeaders(String key) {
      List<Header> keyh = new ArrayList<Header>();
      for (Header h : this.headers) {
        if (h.getName().equalsIgnoreCase(key.trim()))
          keyh.add(h);
      }
      return keyh;
    }

    public synchronized List<Header> getHeaders() {
      return this.headers;
    }

    public void printHeaders() {
      if (this.request != null) {
        DebugInterceptRequest thisreq = (DebugInterceptRequest) this;
        printer.println("Request: method=" + thisreq.getMethod() + "; uri=" + thisreq.getUri());
        Header[] hdrs = this.request.getAllHeaders();
        if (hdrs == null)
          hdrs = new Header[0];
        printer.println("Request Headers:");
        for (Header h : hdrs) {
          printer.println(h.toString());
        }
      }
      if (this.response != null) {
        DebugInterceptResponse thisresp = (DebugInterceptResponse) this;
        printer.println("Response: code=" + thisresp.getStatusCode());
        Header[] hdrs = this.response.getAllHeaders();
        if (hdrs == null)
          hdrs = new Header[0];
        printer.println("Response Headers:");
        for (Header h : hdrs) {
          printer.println(h.toString());
        }
      }
    }
  }

  //////////////////////////////////////////////////
  // Static Variables

  // Use this flag to indicate that all instances should set debug.
  static protected boolean defaultinterception = false;

  // Global set debug interceptors
  static public void setGlobalDebugInterceptors(boolean tf) {
    defaultinterception = true;
  }

  // Use this flag to have debug interceptors print their info
  // in addition to whatever else it does
  static protected Printer defaultprinter = null;

  static public void setGlobalPrinter(Printer printer) {
    defaultprinter = printer;
  }

  //////////////////////////////////////////////////
  // Specific Interceptors

  static public class DebugInterceptResponse extends HTTPIntercepts.InterceptCommon implements HttpResponseInterceptor {
    protected StatusLine statusline = null; // Status Line

    public int getStatusCode() {
      return (statusline == null ? -1 : statusline.getStatusCode());
    }

    public synchronized void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
      this.response = response;
      this.context = context;
      this.statusline = response.getStatusLine();
      if (this.printer != null)
        printHeaders();
      if (this.response != null) {
        Header[] hdrs = this.response.getAllHeaders();
        for (int i = 0; i < hdrs.length; i++) {
          headers.add(hdrs[i]);
        }
      }
    }
  }

  static public class DebugInterceptRequest extends InterceptCommon implements HttpRequestInterceptor {
    protected RequestLine requestline = null; // request Line

    public String getMethod() {
      return (requestline == null ? null : requestline.getMethod());
    }

    public String getUri() {
      return (requestline == null ? null : requestline.getUri());
    }

    public synchronized void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
      this.request = request;
      this.context = context;
      this.requestline = request.getRequestLine();
      if (this.printer != null)
        printHeaders();
      if (this.request != null) {
        Header[] hdrs = this.request.getAllHeaders();
        for (int i = 0; i < hdrs.length; i++) {
          headers.add(hdrs[i]);
        }
      }
    }
  }

  /**
   * Temporary hack to remove Content-Encoding: XXX-Endian headers
   */
  static class ContentEncodingInterceptor extends InterceptCommon implements HttpResponseInterceptor {
    public synchronized void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
      if (response == null)
        return;
      Header[] hdrs = response.getAllHeaders();
      if (hdrs == null)
        return;
      boolean modified = false;
      for (int i = 0; i < hdrs.length; i++) {
        Header h = hdrs[i];
        if (!h.getName().equalsIgnoreCase("content-encoding"))
          continue;
        String value = h.getValue();
        if (value.trim().toLowerCase().endsWith("-endian")) {
          hdrs[i] = new BasicHeader("X-Content-Encoding", value);
          modified = true;
        }
      }
      if (modified)
        response.setHeaders(hdrs);
      // Similarly, suppress encoding for Entity
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        Header ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          String value = ceheader.getValue();
        }
      }
    }
  }

  static class GZIPResponseInterceptor implements HttpResponseInterceptor {
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        Header ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          HeaderElement[] codecs = ceheader.getElements();
          for (HeaderElement h : codecs) {
            if (h.getName().equalsIgnoreCase("gzip")) {
              response.setEntity(new GzipDecompressingEntity(response.getEntity()));
              return;
            }
          }
        }
      }
    }
  }

  static class DeflateResponseInterceptor implements HttpResponseInterceptor {
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
      HttpEntity entity = response.getEntity();
      if (entity != null) {
        Header ceheader = entity.getContentEncoding();
        if (ceheader != null) {
          HeaderElement[] codecs = ceheader.getElements();
          for (HeaderElement h : codecs) {
            if (h.getName().equalsIgnoreCase("deflate")) {
              response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
              return;
            }
          }
        }
      }
    }
  }

  public DebugInterceptRequest debugRequestInterceptor() {
    for (HttpRequestInterceptor hri : reqintercepts) {
      if (hri instanceof DebugInterceptRequest) {
        return ((DebugInterceptRequest) hri);
      }
    }
    return null;
  }

  public DebugInterceptResponse debugResponseInterceptor() {
    for (HttpResponseInterceptor hri : rspintercepts) {
      if (hri instanceof DebugInterceptResponse) {
        return ((DebugInterceptResponse) hri);
      }
    }
    return null;
  }

  //////////////////////////////////////////////////
  // Instance variables

  protected Printer printer = logprinter;

  // This is a hack to suppress content-encoding headers from request
  // Effectively final because its set in the static initializer and otherwise
  // read only.
  protected HttpResponseInterceptor CEKILL = null;

  // Define interceptor instances; use copy on write for thread safety
  protected List<HttpRequestInterceptor> reqintercepts = new CopyOnWriteArrayList<>();
  protected List<HttpResponseInterceptor> rspintercepts = new CopyOnWriteArrayList<>();

  // Debug Header interceptors
  protected List<HttpRequestInterceptor> dbgreq = new CopyOnWriteArrayList<>();
  protected List<HttpResponseInterceptor> dbgrsp = new CopyOnWriteArrayList<>();

  //////////////////////////////////////////////////
  // Constructor(s)

  public HTTPIntercepts() {
    if (defaultinterception)
      this.addDebugInterceptors();
    else
      this.removeDebugIntercepts();
    this.printer = defaultprinter;
  }

  //////////////////////////////////////////////////
  // Methods

  public void setCEKILL(boolean tf) {
    if (tf)
      CEKILL = new ContentEncodingInterceptor();
    else
      CEKILL = null;
  }

  public void activateInterceptors(HttpClientBuilder cb) {
    for (HttpRequestInterceptor hrq : reqintercepts) {
      cb.addInterceptorLast(hrq);
    }
    for (HttpResponseInterceptor hrs : rspintercepts) {
      cb.addInterceptorLast(hrs);
    }
    // Add debug interceptors
    for (HttpRequestInterceptor hrq : dbgreq) {
      cb.addInterceptorFirst(hrq);
    }
    for (HttpResponseInterceptor hrs : dbgrsp) {
      cb.addInterceptorFirst(hrs);
    }
    // Hack: add Content-Encoding suppressor
    cb.addInterceptorFirst(CEKILL);
  }

  protected synchronized void addDebugInterceptors() {
    DebugInterceptRequest rq = new DebugInterceptRequest();
    DebugInterceptResponse rs = new DebugInterceptResponse();
    rq.setPrint(this.printer);
    rs.setPrint(this.printer);
    /* remove any previous */
    for (int i = reqintercepts.size() - 1; i >= 0; i--) {
      HttpRequestInterceptor hr = reqintercepts.get(i);
      if (hr instanceof InterceptCommon) {
        reqintercepts.remove(i);
      }
    }
    for (int i = rspintercepts.size() - 1; i >= 0; i--) {
      HttpResponseInterceptor hr = rspintercepts.get(i);
      if (hr instanceof InterceptCommon) {
        rspintercepts.remove(i);
      }
    }
    reqintercepts.add(rq);
    rspintercepts.add(rs);
  }

  public synchronized void resetInterceptors() {
    for (HttpRequestInterceptor hri : reqintercepts) {
      if (hri instanceof InterceptCommon) {
        ((InterceptCommon) hri).clear();
      }
    }
  }

  public void setGzipCompression() {
    HttpResponseInterceptor hrsi = new HTTPIntercepts.GZIPResponseInterceptor();
    rspintercepts.add(hrsi);
  }

  public void setDeflateCompression() {
    HttpResponseInterceptor hrsi = new HTTPIntercepts.DeflateResponseInterceptor();
    rspintercepts.add(hrsi);
  }

  public synchronized void removeCompression() {
    for (int i = rspintercepts.size() - 1; i >= 0; i--) { // walk backwards
      HttpResponseInterceptor hrsi = rspintercepts.get(i);
      if (hrsi instanceof HTTPIntercepts.GZIPResponseInterceptor
          || hrsi instanceof HTTPIntercepts.DeflateResponseInterceptor) {
        rspintercepts.remove(i);
      }
    }
  }

  public synchronized void removeDebugIntercepts() {
    for (int i = rspintercepts.size() - 1; i >= 0; i--) { // walk backwards
      HttpResponseInterceptor hrsi = rspintercepts.get(i);
      if (hrsi instanceof DebugInterceptResponse)
        rspintercepts.remove(i);
    }
    for (int i = reqintercepts.size() - 1; i >= 0; i--) { // walk backwards
      HttpRequestInterceptor hrsi = reqintercepts.get(i);
      if (hrsi instanceof DebugInterceptRequest)
        reqintercepts.remove(i);
    }
  }
}
