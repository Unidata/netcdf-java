/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.httpservices;

import org.apache.http.HttpHost;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.client.BasicAuthCache;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;

/**
 * It turns out that the Apache httpclient code is not
 * well setup to handle dynamic credentials providers
 * such as UrlAuthenticatorDialog. These providers
 * will ask the user on the fly to provide a
 * set of credentials (typically username+pwd).
 * The provider can be set up to cache credentials
 * (again see UrlAuthenticatorDialog), but in the event
 * that the user gives bad credentials, the provider
 * cache clear function does not get called by AuthCache.
 * So, as a work-around, we use our own implementation
 * of BasicAuthCache that stores a list of credentials
 * providers and when the AuthCache.remove function is called,
 * it forwards it to the either the clear() function of the provider,
 * or, if it is an instance of HTTPCredentialsProvider, it will
 * call the remove function to provide finer-grain control.
 * This is an awful hack, but seems to be the simplest solution.
 */

@ThreadSafe
public class HTTPAuthCache extends BasicAuthCache {
  //////////////////////////////////////////////////
  // Instance variables

  // The providers to notify
  List<CredentialsProvider> credsources = new ArrayList<>();

  //////////////////////////////////////////////////
  // Contructor(s)

  /* Default Constructor */
  public HTTPAuthCache(final SchemePortResolver schemePortResolver) {
    super(schemePortResolver);
  }

  public HTTPAuthCache() {
    this(null);
  }

  //////////////////////////////////////////////////
  // Overridden Methods

  @Override
  synchronized public void remove(final HttpHost host) {
    super.remove(host);
    for (CredentialsProvider cp : credsources) {
      if (cp instanceof HTTPCredentialsProvider)
        ((HTTPCredentialsProvider) cp).remove(host);
      else
        cp.clear();
    }
  }

  // New
  synchronized public HTTPAuthCache addProvider(CredentialsProvider cp) {
    if (!credsources.contains(cp))
      credsources.add(cp);
    return this;
  }

  synchronized public HTTPAuthCache removeProvider(CredentialsProvider cp) {
    credsources.remove(cp);
    return this;
  }
}
