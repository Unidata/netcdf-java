/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.http;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Http requests go through this Singleton.
 * TODO We anticipate especially managing credentials from here. Not yet implemented.
 * TODO HTTPSession.setGlobalCompression()
 * If so, we might need multiple HttpService to manage different users, eg from a server?
 * escaping?
 * Lots of complicated stuff going on in ucar.httpservices.HTTPSession, etc.
 * Do the naive thing for now, and see how much we will have to port.
 * HttpRequest request = HttpService.standardGetRequestBuilder(url).build();
 * HttpResponse<InputStream> response = HttpService.standardRequest(request);
 * HttpHeaders responseHeaders = response.headers();
 */
public class HttpService {
  public static final HttpService STANDARD = new HttpService("NetcdfJava7", Duration.ofSeconds(10));

  public static HttpClient standardHttpCLient() {
    return STANDARD.makeHttpCLient();
  }

  public static HttpRequest.Builder standardGetRequestBuilder(String url) {
    return STANDARD.makeGetRequestBuilder(url);
  }

  public static HttpResponse<InputStream> standardRequest(HttpRequest request) throws IOException {
    return STANDARD.makeRequest(standardHttpCLient(), request);
  }

  public static HttpResponse<String> standardRequestForString(HttpRequest request) throws IOException {
    return STANDARD.makeRequestForString(standardHttpCLient(), request);
  }

  // CredentialsProvider provider;
  private String userAgent;
  private Duration waitSecs;

  public HttpService(String userAgent, Duration waitSecs) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(userAgent));
    Preconditions.checkNotNull(waitSecs);
    this.userAgent = userAgent;
    this.waitSecs = waitSecs;
  }

  /*
   * public HttpService setCredentialsProvider(CredentialsProvider provider) {
   * Preconditions.checkNotNull(provider);
   * this.provider = provider;
   * return this;
   * }
   */

  public void setUserAgent(String userAgent) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(userAgent));
    this.userAgent = userAgent;
  }

  public void setWait(Duration waitSecs) {
    Preconditions.checkNotNull(waitSecs);
    this.waitSecs = waitSecs;
  }

  public HttpClient makeHttpCLient() {
    return HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).connectTimeout(waitSecs)
        .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER)).build();
  }

  public HttpRequest.Builder makeGetRequestBuilder(String url) {
    return HttpRequest.newBuilder().uri(URI.create(url)).setHeader("User-Agent", userAgent).timeout(waitSecs).GET();
  }

  public HttpResponse<InputStream> makeRequest(HttpClient session, HttpRequest request) throws IOException {
    HttpResponse.BodyHandler<InputStream> bodyHandler = HttpResponse.BodyHandlers.ofInputStream();
    int retries = 4;
    HttpResponse<InputStream> response = null;
    while (retries > 0) {
      try {
        response = session.send(request, bodyHandler);
        break;
      } catch (InterruptedException e) {
        retries--;
      }
    }
    if (response == null) {
      throw new IOException("Server interruption limit exceeded");
    }

    int statusCode = response.statusCode();
    if (statusCode == 404) {
      throw new FileNotFoundException(request.uri() + " got 404");
    }
    if (statusCode >= 300) {
      if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN) {
        throw new IOException("Unauthorized to open dataset " + request.uri());
      } else {
        throw new IOException(request.uri() + " is not a valid URL, return status=" + statusCode);
      }
    }

    return response;
  }

  public HttpResponse<String> makeRequestForString(HttpClient session, HttpRequest request) throws IOException {
    HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();
    int retries = 4;
    HttpResponse<String> response = null;
    while (retries > 0) {
      try {
        response = session.send(request, bodyHandler);
        break;
      } catch (InterruptedException e) {
        retries--;
      }
    }
    if (response == null) {
      throw new IOException("Server interruption limit exceeded");
    }

    int statusCode = response.statusCode();
    if (statusCode == 404) {
      throw new FileNotFoundException(request.uri() + " got 404");
    }
    if (statusCode >= 300) {
      if (statusCode == HTTP_UNAUTHORIZED || statusCode == HTTP_FORBIDDEN)
        throw new IOException("Unauthorized to open dataset " + request.uri());
      else
        throw new IOException(request.uri() + " is not a valid URL, return status=" + statusCode);
    }

    return response;
  }
}
