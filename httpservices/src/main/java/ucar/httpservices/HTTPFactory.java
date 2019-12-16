/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.httpservices;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.message.BasicHttpResponse;
import java.lang.reflect.Constructor;
import java.util.Set;

/**
 * HTTPFactory creates method instance.
 * This code was originally in HttpMethod.
 */

public class HTTPFactory {
  // In order to test client side code that mocks
  // HTTPMethod, provide a static global
  // than can be set by a test program.

  public static java.lang.Class MOCKMETHODCLASS = null;

  //////////////////////////////////////////////////////////////////////////
  // Static factory methods for creating HTTPSession instances

  public static HTTPSession newSession(String host, int port) throws HTTPException {
    return new HTTPSession(host, port);
  }

  public static HTTPSession newSession(String url) throws HTTPException {
    return new HTTPSession(url);
  }

  public static HTTPSession newSession(HttpHost target) throws HTTPException {
    return new HTTPSession(target);
  }

  @Deprecated
  public static HTTPSession newSession(AuthScope scope) throws HTTPException {
    HttpHost hh = new HttpHost(scope.getHost(), scope.getPort(), HttpHost.DEFAULT_SCHEME_NAME);
    return new HTTPSession(hh);
  }

  //////////////////////////////////////////////////////////////////////////
  // Static factory methods for creating HTTPMethod instances

  public static HTTPMethod Get(HTTPSession session, String legalurl) throws HTTPException {
    return makemethod(HTTPSession.Methods.Get, session, legalurl);
  }

  public static HTTPMethod Head(HTTPSession session, String legalurl) throws HTTPException {
    return makemethod(HTTPSession.Methods.Head, session, legalurl);
  }

  public static HTTPMethod Put(HTTPSession session, String legalurl) throws HTTPException {
    return makemethod(HTTPSession.Methods.Put, session, legalurl);
  }

  public static HTTPMethod Post(HTTPSession session, String legalurl) throws HTTPException {
    return makemethod(HTTPSession.Methods.Post, session, legalurl);
  }

  public static HTTPMethod Options(HTTPSession session, String legalurl) throws HTTPException {
    return makemethod(HTTPSession.Methods.Options, session, legalurl);
  }

  public static HTTPMethod Get(HTTPSession session) throws HTTPException {
    return Get(session, null);
  }

  public static HTTPMethod Head(HTTPSession session) throws HTTPException {
    return Head(session, null);
  }

  public static HTTPMethod Put(HTTPSession session) throws HTTPException {
    return Put(session, null);
  }

  public static HTTPMethod Post(HTTPSession session) throws HTTPException {
    return Post(session, null);
  }

  public static HTTPMethod Options(HTTPSession session) throws HTTPException {
    return Options(session, null);
  }

  public static HTTPMethod Get(String legalurl) throws HTTPException {
    return Get(null, legalurl);
  }

  public static HTTPMethod Head(String legalurl) throws HTTPException {
    return Head(null, legalurl);
  }

  public static HTTPMethod Put(String legalurl) throws HTTPException {
    return Put(null, legalurl);
  }

  public static HTTPMethod Post(String legalurl) throws HTTPException {
    return Post(null, legalurl);
  }

  public static HTTPMethod Options(String legalurl) throws HTTPException {
    return Options(null, legalurl);
  }

  /**
   * Common method creation code so we can isolate mocking
   *
   * @param session
   * @return
   * @throws HTTPException
   */
  protected static HTTPMethod makemethod(HTTPSession.Methods m, HTTPSession session, String url) throws HTTPException {
    HTTPMethod meth = null;
    if (MOCKMETHODCLASS == null) { // do the normal case
      meth = new HTTPMethod(m, session, url);
    } else {// (MOCKMETHODCLASS != null)
      java.lang.Class methodcl = MOCKMETHODCLASS;
      Constructor<HTTPMethod> cons = null;
      try {
        cons = methodcl.getConstructor(HTTPSession.Methods.class, HTTPSession.class, String.class);
      } catch (Exception e) {
        throw new HTTPException("HTTPFactory: no proper HTTPMethod constructor available", e);
      }
      try {
        meth = cons.newInstance(m, session, url);
      } catch (Exception e) {
        throw new HTTPException("HTTPFactory: HTTPMethod constructor failed", e);
      }
    }
    return meth;
  }


  public static Set<String> getAllowedMethods() {
    HttpResponse rs = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 0, "");
    Set<String> set = new HttpOptions().getAllowedMethods(rs);
    return set;
  }

}
