/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package thredds.inventory;

import java.io.IOException;
import java.util.ServiceLoader;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.filesystem.MFileOS;
import ucar.nc2.internal.ncml.NcmlReader;

/**
 * Static helper methods for MFile objects.
 *
 * @since 5.4
 */
public class MFiles {
  private static final Logger logger = LoggerFactory.getLogger(NcmlReader.class);

  /**
   * Create an {@link thredds.inventory.MFile} from a given location if it exists and is readable, otherwise return
   * null.
   *
   * @param location location of file (local or remote) to be used to back the MFile object
   * @return {@link thredds.inventory.MFile}
   */
  @Nullable
  public static MFile create(String location) {
    MFileProvider mFileProvider = null;

    // look for dynamically loaded MFileProviders
    for (MFileProvider provider : ServiceLoader.load(MFileProvider.class)) {
      if (provider.canProvide(location)) {
        mFileProvider = provider;
        break;
      }
    }

    MFile mfile = null;
    try {
      mfile = mFileProvider != null ? mFileProvider.create(location) : MFileOS.getExistingFile(location);
    } catch (IOException ioe) {
      logger.error("Error creating MFile at {}.", location, ioe);
    }
    return mfile;
  }
}
