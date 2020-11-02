/*
 * Copyright (c) 2020 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package thredds.inventory;

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
}
