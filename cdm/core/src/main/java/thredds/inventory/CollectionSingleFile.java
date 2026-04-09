/*
 * Copyright (c) 1998-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import ucar.nc2.util.DiskCache2;

/**
 * A CollectionManager consisting of a single file
 *
 * @author caron
 * @since 12/23/11
 */
public class CollectionSingleFile extends CollectionList {

  public CollectionSingleFile(MFile file, org.slf4j.Logger logger) {
    super(file.getName(), logger);
    mfiles.add(file);
    try {
      MFile p = file.getParent();
      this.root = p != null ? p.getPath() : DiskCache2.getDefault().getRootDirectory();
    } catch (java.io.IOException e) {
      this.root = DiskCache2.getDefault().getRootDirectory();
    }

    this.lastModified = file.getLastModified();
  }

}
