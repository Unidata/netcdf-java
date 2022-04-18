/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.net;

import java.net.*;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.httpservices.HTTPSession;

/**
 * how do we know if URLStreamHandlerFactory has already been se
 * 
 * @deprecated do not use
 */
@Deprecated
public class URLStreamHandlerFactory implements java.net.URLStreamHandlerFactory {

  private static final Logger logger = LoggerFactory.getLogger(URLStreamHandlerFactory.class);

  //////////////////////////////////////////////////////////////////////////
  private static Map<String, URLStreamHandler> map = new java.util.HashMap<>();
  private static boolean installed;

  public static void install() {
    try {
      if (!installed) {
        java.net.URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory());
        installed = true;
      }
    } catch (Error e) {
      logger.error("Error installing URLStreamHandlerFactory " + e.getMessage());
    }
  }

  public static void register(String protocol, URLStreamHandler sh) {
    map.put(protocol.toLowerCase(), sh);
  }

  public static URL makeURL(String urlString) throws MalformedURLException {
    return installed ? new URL(urlString) : makeURL(null, urlString);
  }

  public static URL makeURL(URL parent, String urlString) throws MalformedURLException {
    if (installed)
      return new URL(parent, urlString);

    // install failed, use alternate form of URL constructor
    try {
      URI uri = new URI(urlString);
      URLStreamHandler h = map.get(uri.getScheme().toLowerCase());
      return new URL(parent, urlString, h);
    } catch (URISyntaxException e) {
      throw new MalformedURLException(e.getMessage());
    }

    // return new URL( url.getScheme(), url.getHost(), url.getPort(), url.getFile(), h);
  }

  public URLStreamHandler createURLStreamHandler(String protocol) {
    return map.get(protocol.toLowerCase());
  }

}
