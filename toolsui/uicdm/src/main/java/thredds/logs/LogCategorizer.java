/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.logs;

import ucar.unidata.util.StringUtil2;

public class LogCategorizer {
  static boolean showRoots;

  private static String roots;

  public static void setRoots(String raw) {
    if (null != raw)
      roots = raw;
  }

  public static PathMatcher readRoots() {
    PathMatcher pathMatcher = new PathMatcher();
    String rootString = StringUtil2.replace(roots, "\n", ",");
    String[] roots = rootString.split(",");
    for (int i = 0; i < roots.length; i += 2) {
      if (showRoots)
        System.out.printf("  %-40s %-40s%n", roots[i], roots[i + 1]);
      pathMatcher.put(roots[i]);
    }

    return pathMatcher;
  }

  private static PathMatcher pathMatcher;

  public static String getDataroot(String path, int status) {
    if (pathMatcher == null)
      pathMatcher = readRoots();

    String ss = getServiceSpecial(path);
    if (ss != null)
      return "service-" + ss;

    if (path.startsWith("//thredds/"))
      path = path.substring(1);

    if (!path.startsWith("/thredds/"))
      return "zervice-root";

    String dataRoot = null;
    String spath = path.substring(9); // remove /thredds/
    String service = findService(spath);
    if (service != null) {
      // if (service.equals("radarServer")) dataRoot = "radarServer";
      // else if (service.equals("casestudies")) dataRoot = "casestudies";
      // else if (service.equals("dqc")) dataRoot = "dqc";
      if (spath.length() > service.length()) {
        spath = spath.substring(service.length() + 1);
        PathMatcher.Match match = pathMatcher.match(spath);
        if (match != null)
          dataRoot = match.root;
      }
    }

    if (dataRoot == null) {
      if (status >= 400)
        dataRoot = "zBad";
    }

    if (dataRoot == null) {
      service = getService(path);
      dataRoot = "zervice-" + service;
    }
    return dataRoot;
  }

  // the ones that dont start with thredds
  public static String getServiceSpecial(String path) {
    String ss = null;
    if (path.startsWith("/dqcServlet"))
      ss = "dqcServlet";
    else if (path.startsWith("/cdmvalidator"))
      ss = "cdmvalidator";
    return ss;
  }

  public static String getService(String path) {
    String service = getServiceSpecial(path);
    if (service != null)
      return service;

    if (path.startsWith("/dts"))
      return "dts";

    if (path.startsWith("/dqcServlet"))
      return "dqcServlet";

    if (path.startsWith("/thredds/")) {
      String spath = path.substring(9);
      service = findService(spath);
      if ((service == null) && (spath.startsWith("ncml") || spath.startsWith("uddc") || spath.startsWith("iso")))
        service = "ncIso";
      if ((service == null) && (spath.endsWith("xml") || spath.endsWith("html")))
        service = "catalog";

      if (service == null) {
        int pos = spath.indexOf('?');
        if (pos > 0) {
          String req = spath.substring(0, pos);
          if (req.endsWith("xml") || req.endsWith("html"))
            service = "catalog";
        }
      }
    }

    if (service == null)
      service = "other";
    return service;
  }

  public static String[] services = {"admin", "cataloggen", "catalog", "cdmremote", "cdmrfeature", "dodsC", "dqc",
      "fileServer", "godiva2", "ncss", "ncstream", "radarServer", "remoteCatalogService", "view", "wcs", "wms"};

  public static String findService(String path) {
    for (String service : services) {
      if (path.startsWith(service))
        return service;
    }
    return null;
  }

}
