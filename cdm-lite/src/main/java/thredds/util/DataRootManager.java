/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.util;

import java.util.HashMap;

/**
 * Describe {@link Class}
 */
public class DataRootManager {
  private static HashMap<String, String> dataRoot = new HashMap<>();

  public static String getLocationFromRequestPath(String reqPath) {
    return dataRoot.get(reqPath);
  }

}
