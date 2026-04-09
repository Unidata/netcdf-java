/*
 * Copyright (c) 2020-2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ServiceLoader;
import thredds.filesystem.ControllerOS;

public class MControllers {

  /**
   * Create an {@link MController} capable of working with a given location.
   *
   * @param location location under which granules should be managed
   * @return {@link MController}
   */
  public static MController create(String location) {
    MControllerProvider mControllerProvider = null;

    // look for dynamically loaded MControllerProviders
    if (location != null) {
      for (MControllerProvider provider : ServiceLoader.load(MControllerProvider.class)) {
        if (provider.canScan(location)) {
          mControllerProvider = provider;
          break;
        }
      }
    }

    return mControllerProvider != null ? mControllerProvider.create() : new ControllerOS();
  }

  /**
   * Create a {@link DirectoryStream} of {@link MFile}s for a given location.
   *
   * @param location location to scan
   * @return {@link DirectoryStream} of {@link MFile}s
   * @throws IOException if an I/O error occurs
   */
  public static DirectoryStream<MFile> newDirectoryStream(String location) throws IOException {
    MController controller = create(location);
    DirectoryStream<MFile> stream = controller.getFullInventoryAtLocation(location);
    controller.close();
    if (stream == null) {
      throw new IOException("Could not create DirectoryStream for " + location);
    }
    return stream;
  }

  /**
   * Create a {@link DirectoryStream} of {@link MFile} subdirectories for a given location.
   *
   * @param location location to scan
   * @return {@link DirectoryStream} of {@link MFile} subdirectories
   * @throws IOException if an I/O error occurs
   */
  public static DirectoryStream<MFile> newSubdirStream(String location) throws IOException {
    MController controller = create(location);
    CollectionConfig config = new CollectionConfig(location, location, false, null, null);
    DirectoryStream<MFile> stream = controller.getSubdirs(config, true);
    controller.close();
    if (stream == null) {
      throw new IOException("Could not create SubdirStream for " + location);
    }
    return stream;
  }
}
