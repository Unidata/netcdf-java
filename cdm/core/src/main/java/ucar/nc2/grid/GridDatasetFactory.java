/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.Iterables;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.grid.GridDatasetImpl;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/** Open Grid Datasets. */
public class GridDatasetFactory {

  /** Open a NetcdfDataset and wrap as a GridDataset. Return null if its not a gridDataset. */
  @Nullable
  public static GridDataset openGridDataset(String endpoint, Formatter errLog) throws IOException {
    // Otherwise, wrap a NetcdfDataset
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(endpoint);
    Optional<GridDatasetImpl> result =
        GridDatasetImpl.create(ds, errLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));
    if (!result.isPresent()) {
      errLog.format("Could not open as GridDataset: %s", endpoint);
      ds.close();
      return null;
    }

    return result.get();
  }
}
