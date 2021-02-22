/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handle textual substitution for dataroots.
 * This needs to be accessible to NcML, thredds catalogs, feature collecion config.
 * TODO should be a singleton class.
 */
public class AliasTranslator {

  private static final Map<String, String> alias = new ConcurrentHashMap<>();

  public static void addAlias(String aliasKey, String actual) {
    alias.put(aliasKey, actual.replace("\\", "/"));
  }

  public static String translateAlias(String scanDir) {
    for (Map.Entry<String, String> entry : alias.entrySet()) {
      if (scanDir.startsWith(entry.getKey())) { // only at the front
        StringBuilder sb = new StringBuilder(scanDir);
        return sb.replace(0, entry.getKey().length(), entry.getValue()).toString();
      }
    }
    return scanDir;
  }

  public static int size() {
    return alias.size();
  }

}
