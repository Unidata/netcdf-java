/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.nio.file.DirectoryStream;

/**
 * Inventory Management Controller
 *
 * @author caron
 * @since Jun 25, 2009
 */
public interface MController extends Closeable {

  /**
   * Returns all leaves in collection, recursing into subdirectories.
   * 
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return DirectoryStream over Mfiles, or null if collection does not exist
   */
  @Nullable
  DirectoryStream<MFile> getInventoryAll(CollectionConfig mc, boolean recheck);

  /**
   * Returns all leaves in top collection, not recursing into subdirectories.
   * 
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return DirectoryStream over Mfiles, or null if collection does not exist
   */
  @Nullable
  DirectoryStream<MFile> getInventoryTop(CollectionConfig mc, boolean recheck);

  /**
   * Returns all subdirectories in top collection.
   * 
   * @param mc defines the collection to scan
   * @param recheck if false, may use cached results. otherwise must sync with File OS
   * @return DirectoryStream over Mfiles, or null if collection does not exist
   */
  @Nullable
  DirectoryStream<MFile> getSubdirs(CollectionConfig mc, boolean recheck);

  /**
   * Get an MFile for a specific location.
   * 
   * @param location the location
   * @return MFile or null
   */
  @Nullable
  default MFile getMFile(String location) {
    return MFiles.create(location);
  }

  @Override
  void close();

}
