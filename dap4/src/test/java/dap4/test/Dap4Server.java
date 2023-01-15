/*
 * Copyright 2012, UCAR/Unidata.
 * See the LICENSE file for more information.
 */

package dap4.test;

import dap4.core.util.DapConstants;
import dap4.core.util.DapException;
import ucar.httpservices.HTTPFactory;
import ucar.httpservices.HTTPMethod;

import java.util.ArrayList;
import java.util.List;

public class Dap4Server {

  //////////////////////////////////////////////////
  // Server Registry

  // Order is important; testing reachability is in the order listed
  static List<Dap4Server> registry;

  static {
    registry = new ArrayList<>();
  }

  static void register(boolean front, Dap4Server svc) {
    // If already in registry, then replace it
    for (int i = 0; i < registry.size(); i++) {
      Dap4Server ds = registry.get(i);
      if (ds.id.equals(svc.id)) {
        registry.set(i, svc);
        return;
      }
    }
    if (front)
      registry.add(0, svc);
    else
      registry.add(svc);
  }

  static Dap4Server getServer(String id) {
    for (Dap4Server ds : registry) {
      if (ds.id.equals(id))
        return ds;
    }
    return null;
  }

  //////////////////////////////////////////////////
  // Instance variables
  public String id;
  public String ip;
  public int port;
  public String servletpath;

  //////////////////////////////////////////////////
  // Constructor(s)

  public Dap4Server(String id, String ip, int port, String servletpath) {
    this.id = id;
    this.ip = ip;
    this.port = port;
    this.servletpath = servletpath;
  }

  public String getURL() {
    return getURL(DapConstants.HTTPSCHEME);
  }

  public String getURL(String scheme) {
    StringBuilder buf = new StringBuilder();
    buf.append(scheme + "//");
    buf.append(this.ip);
    if (this.port > 0) {
      buf.append(":");
      buf.append(this.port);
    }
    buf.append("/");
    buf.append(this.servletpath);
    return buf.toString();
  }

  public boolean ping() {
    boolean ok = false;
    String url = getURL();
    try {
      try (HTTPMethod method = HTTPFactory.Get(url)) {
        int code = method.execute();
        if (code == 200)
          ok = true;
      }
    } catch (Exception e) {
      ok = false;
    }
    return ok;
  }

  static public Dap4Server findServer() throws DapException {
    // Find the server to use
    for (Dap4Server svc : registry) {
      if (svc.ping())
        return svc;
    }
    return null;
  }
}


