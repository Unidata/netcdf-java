/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;


import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import javax.annotation.concurrent.Immutable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;


/**
 * Provide Auth related utilities
 */

@Immutable
public abstract class HTTPAuthUtil {

  //////////////////////////////////////////////////
  // Constants

  //////////////////////////////////////////////////
  // AuthScope Utilities

  /**
   * Given a session url AuthScope and a Method url AuthScope,
   * Indicate it the are "compatible" as defined as follows.
   * The method AuthScope is <i>compatible</i> with the session AuthScope
   * if its host+port is the same as the session's host+port and its scheme is
   * compatible, where e.g. http is compatible with https.
   * The scope realm is ignored.
   *
   * @param ss Session AuthScope
   * @param ms Method AuthScope
   * @return
   */
  static boolean authscopeCompatible(AuthScope ss, AuthScope ms) {
    if (!ss.getHost().equalsIgnoreCase(ms.getHost()))
      return false;
    if (ss.getPort() != ms.getPort())
      return false;
    String sss = ss.getScheme();
    String mss = ms.getScheme();
    if (sss != AuthScope.ANY_SCHEME && mss != AuthScope.ANY_SCHEME && sss != mss)
      return false;
    return true;
  }


  static AuthScope uriToAuthScope(String surl) throws HTTPException {
    try {
      URI uri = HTTPUtil.parseToURI(surl);
      return uriToAuthScope(uri);
    } catch (URISyntaxException e) {
      throw new HTTPException(e);
    }
  }

  /**
   * Create an AuthScope from a URI; remove any principal
   *
   * @param uri to convert
   * @return an AuthScope instance
   */

  static AuthScope uriToAuthScope(URI uri) {
    assert (uri != null);
    return new AuthScope(uri.getHost(), uri.getPort(), AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
  }

  static URI authscopeToURI(AuthScope authScope) throws HTTPException {
    try {
      URI url = new URI("https", null, authScope.getHost(), authScope.getPort(), "", null, null);
      return url;
    } catch (URISyntaxException mue) {
      throw new HTTPException(mue);
    }
  }

  // Note that the scheme field of HttpHost is not the same
  // as the scheme field of AuthScope. The HttpHost schem is
  // a protocol like http or https. The schem field of AuthScope
  // is the authorization scheme like Basic or NTLM or Digest.
  static public HttpHost authscopeToHost(AuthScope scope) {
    return new HttpHost(scope.getHost(), scope.getPort(), HttpHost.DEFAULT_SCHEME_NAME);
  }

  // Note that the scheme field of HttpHost is not the same
  // as the scheme field of AuthScope. The HttpHost schem is
  // a protocol like http or https. The schem field of AuthScope
  // is the authorization scheme like Basic or NTLM or Digest.
  static public AuthScope hostToAuthScope(HttpHost host) {
    return new AuthScope(host.getHostName(), host.getPort(), AuthScope.ANY_REALM, AuthScope.ANY_SCHEME);
  }

  public static AuthScope bestmatch(AuthScope scope, Set<AuthScope> scopelist) {
    Credentials creds = null;
    int bestMatchFactor = -1;
    AuthScope bestMatch = null;
    for (final AuthScope current : scopelist) {
      final int factor = scope.match(current);
      if (factor > bestMatchFactor) {
        bestMatchFactor = factor;
        bestMatch = current;
      }
    }
    return bestMatch;
  }

}
