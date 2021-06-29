/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.zarr;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ZarrPathUtils {

  /**
   * Get the name of the current Zarr object (variable or group)
   * Defined as the name of the current directory
   * 
   * @param filePath
   * @return name of directory containing filepath as a String
   */
  @Nullable
  public static String getObjectNameFromPath(String filePath) {
    try {
      return Paths.get(filePath).getParent().getFileName().toString();
    } catch (NullPointerException ex) {
      return null;
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
    return location;
  }
}
