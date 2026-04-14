/*
 * Copyright (c) 1998-2026 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import ucar.nc2.grib.collection.Grib;
import ucar.nc2.util.DiskCache2;
import thredds.inventory.MFile;
import thredds.inventory.MFiles;
import java.io.File;

/**
 * manages where the grib index files live
 *
 * @author caron
 * @since 12/18/2014
 */
public class GribIndexCache {

  private static DiskCache2 diskCache;

  public static synchronized void setDiskCache2(DiskCache2 dc) {
    diskCache = dc;
  }

  public static synchronized DiskCache2 getDiskCache2() {
    if (diskCache == null)
      diskCache = DiskCache2.getDefault();
    return diskCache;
  }

  /**
   * Get index file, may be in cache directory, may not exist
   *
   * @param fileLocation full path of original index filename
   * @return File, possibly in cache, may or may not exist
   */
  public static MFile getFileOrCache(String fileLocation) {
    MFile result = getExistingFileOrCache(fileLocation);
    if (result != null)
      return result;
    return MFiles.create(getDiskCache2().getFile(fileLocation).getPath());
  }

  /**
   * Looking for an existing file, in cache or not
   *
   * @param fileLocation full path of original index filename
   * @return existing file if you can find it, else null
   */
  public static MFile getExistingFileOrCache(String fileLocation) {
    MFile idxMFile = MFiles.create(fileLocation);
    if (!MFiles.isLocal(idxMFile)) {
      // for remote file systems, check to see if the index file exists and, if so, use it
      // note: the remote index file may require updating, which isn't support at this point,
      // so opening the remote GRIB file may ultimately fail.
      if (idxMFile.exists()) {
        return idxMFile;
      }
    }
    // if the GRIB index file is local OR the remote index does not exist, check
    // the DiskCache.
    File result = getDiskCache2().getExistingFileOrCache(fileLocation);
    if (result == null && Grib.debugGbxIndexOnly && fileLocation.endsWith(".gbx9.ncx4")) { // might create only from
                                                                                           // gbx9 for debugging
      int length = fileLocation.length();
      String maybeIndexAlreadyExists = fileLocation.substring(0, length - 10) + ".ncx4";
      result = getDiskCache2().getExistingFileOrCache(maybeIndexAlreadyExists);
    }
    return result == null ? null : MFiles.create(result.getPath());
  }
}
