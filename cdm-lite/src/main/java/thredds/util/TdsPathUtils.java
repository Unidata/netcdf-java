/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;

/**
 * Utilities for extracting path information from request.
 * Handles servlet and spring controller cases
 */
public class TdsPathUtils {

  // For "removePrefix/path" style servlet mappings.
  public static String extractPath(HttpServletRequest req, String removePrefix) {

    // may be in pathInfo (Servlet) or servletPath (Controller)
    String dataPath = req.getPathInfo();
    if (dataPath == null) {
      dataPath = req.getServletPath();
    }
    if (dataPath == null) // not sure if this is possible
      return "";

    // removePrefix or "/"+removePrefix
    if (removePrefix != null) {
      if (dataPath.startsWith(removePrefix)) {
        dataPath = dataPath.substring(removePrefix.length());

      } else if (dataPath.startsWith("/")) {
        dataPath = dataPath.substring(1);
        if (dataPath.startsWith(removePrefix))
          dataPath = dataPath.substring(removePrefix.length());
      }

    }

    if (dataPath.startsWith("/"))
      dataPath = dataPath.substring(1);

    if (dataPath.contains("..")) // LOOK what about escapes ??
      throw new IllegalArgumentException("path cannot contain '..'");

    return dataPath;
  }

  public static long getLastModified(String reqPath) {
    File file = getFile(reqPath);
    return (file == null) ? -1 : file.lastModified();
  }

  public static File getFile(String reqPath) {
    String location = DataRootManager.getLocationFromRequestPath(reqPath);
    return (location == null) ? null : new File(location);
  }

  public static NetcdfFile getNetcdfFile(HttpServletRequest request, String reqPath) throws IOException {
    String extracted = TdsPathUtils.extractPath(request, null);
    return NetcdfDatasets.openDataset(extracted);
  }
}
