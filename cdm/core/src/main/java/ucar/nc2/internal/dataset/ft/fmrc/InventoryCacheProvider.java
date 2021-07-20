/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.dataset.ft.fmrc;

import thredds.inventory.MFile;
import ucar.nc2.ft.fmrc.GridDatasetInv;

import javax.annotation.Nullable;
import java.io.*;

/**
 * Service Provider Interface for providing a persisted cache for {@link GridDatasetInv}.
 *
 * For use by the THREDDS Data Server and not intended to be used publicly.
 *
 */
public interface InventoryCacheProvider {

  /**
   * Get the grid inventory associated with an MFile from the cache.
   *
   * @param mfile the mfile containing gridded data
   * @return grid inventory of the of mfile, null if not found
   * @throws IOException
   */
  @Nullable
  public GridDatasetInv get(MFile mfile) throws IOException;

  /**
   * Add the grid inventory associated with an MFile to the cache.
   *
   * @param mfile the mfile containing gridded data
   * @param inventory the grid inventory of the of mfile
   * @throws IOException
   */
  public void put(MFile mfile, GridDatasetInv inventory) throws IOException;

}
