/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.filesystem.MFileOS;
import thredds.filesystem.MFileOS7;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.util.NcServiceLoader;



/**
 * Static helper methods for MFile objects.
 *
 * @since 5.4
 */
public class MFiles {
  private static final Logger logger = LoggerFactory.getLogger(NcmlReader.class);

  /**
   * Create an {@link thredds.inventory.MFile} from a given location, the file may or may not exist
   *
   * @param location location of file (local or remote) to be used to back the MFile object
   * @return {@link thredds.inventory.MFile}
   */
  @Nonnull
  public static MFile create(@Nonnull String location) {
    MFileProvider mFileProvider = null;

    // look for dynamically loaded MFileProviders
    for (MFileProvider provider : NcServiceLoader.load(MFileProvider.class)) {
      if (provider.canProvide(location)) {
        mFileProvider = provider;
        break;
      }
    }

    try {
      if (mFileProvider != null) {
        return mFileProvider.create(location);
      }
    } catch (IOException ioe) {
      logger.error("Error creating MFile at {}.", location, ioe);
    }
    return new MFileOS(location);
  }

  /**
   * Create an {@link thredds.inventory.MFile} from a given location if it exists, otherwise return
   * null.
   *
   * @param location location of file (local or remote) to be used to back the MFile object
   * @return {@link thredds.inventory.MFile}
   */
  @Nullable
  public static MFile createIfExists(String location) {
    if (location == null) {
      return null;
    }

    final MFile mFile = create(location);
    return mFile.exists() ? mFile : null;
  }

  /**
   * Checks if MFile represents a file stored on a local filesystem
   *
   * @param mfile MFile to check
   * @return true if MFile is a local filesystem
   */
  public static boolean isLocal(MFile mfile) {
    return mfile instanceof MFileOS || mfile instanceof MFileOS7;
  }
}
