/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import java.nio.file.Paths;

public class ZarrUtils {
  /**
   * Converts subscripted chunk number to a flat index
   * 
   * @param subs - chunk index in each dimension
   * @param nChunks number of chunks per dimension
   * @return chunk number as a flat index
   */
  public static int subscriptsToIndex(int[] subs, int[] nChunks) {
    int num = 0;
    int sz = 1;
    for (int i = subs.length - 1; i >= 0; i--) {
      num += sz * subs[i];
      sz *= nChunks[i];
    }
    return num;
  }

  /**
   * Get name of an file within a Zarr object, i.e. name of file
   * e.g. 0.0.0 or, in the case of a nested store, 0/0/0
   * 
   * @param filePath
   * @return data path relative to Zarr object
   */
  public static String getDataFileName(String filePath) {
    // search for a path delimiter or default to "/"
    String delim_key = "#delimiter=";
    int index = filePath.indexOf(delim_key);
    String delimiter = index < 0 ? "/" : filePath.substring(index + delim_key.length(), index + delim_key.length() + 1);

    // split into path components
    String[] path = trimLocation(filePath).split(delimiter);

    // search backwards until we find a path component that is not part of a data file name
    // assumes groups and vars do not have numeric names
    String fileName = path[path.length - 1];
    for (int i = path.length - 2; i >= 0; i--) {
      try {
        Double.parseDouble(path[i]);
      } catch (NumberFormatException nfe) {
        break; // non numeric names are not data files
      }
      fileName = path[i] + delimiter + fileName;
    }
    return fileName;
  }

  /**
   * Get the name of the current Zarr object (variable or group)
   * Defined as the name of the current directory
   * 
   * @param filePath
   * @return name of directory containing filepath as a String or empty String if unresolved
   */
  public static String getObjectNameFromPath(String filePath) {
    try {
      return Paths.get(filePath).getParent().getFileName().toString();
    } catch (NullPointerException ex) {
      return "";
    }
  }

  /**
   * Get the name of the parent Group of an object
   * Defined as the name of the directory two level up
   * 
   * @param filePath
   * @return name of the grandparent directory of filepath as a String
   */
  public static String getParentGroupNameFromPath(String filePath, String rootPath) {
    try {
      return Paths.get(rootPath).relativize(Paths.get(filePath).getParent().getParent()).toString().replace("\\", "/");
    } catch (NullPointerException ex) {
      return "/";
    }
  }

  /**
   * Remove characters outside logical filepath, e.g. S3 tags
   * 
   * @param location
   * @return logical filepath
   */
  public static String trimLocation(String location) {
    if (location.contains(":")) {
      location = location.substring(location.indexOf(":") + 1);
    }
    if (location.contains("?")) {
      location = location.substring(location.indexOf("?") + 1);
    }
    if (location.contains("#")) {
      location = location.substring(0, location.lastIndexOf("#"));
    }
    if (location.endsWith("/")) {
      location = location.substring(0, location.length() - 1);
    }
    return location;
  }
}
