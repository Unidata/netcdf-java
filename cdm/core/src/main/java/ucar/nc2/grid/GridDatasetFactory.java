package ucar.nc2.grid;

import com.google.common.collect.Iterables;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.grid.GridDatasetImpl;

import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

public class GridDatasetFactory {

  public static Optional<GridDataset> openGridDataset(String endpoint, Formatter errLog) throws IOException {
    NetcdfDataset ds = ucar.nc2.dataset.NetcdfDatasets.openDataset(endpoint);

    Optional<GridDatasetImpl> result =
        GridDatasetImpl.create(ds, errLog).filter(gds -> !Iterables.isEmpty(gds.getGrids()));

    if (!result.isPresent()) {
      errLog.format("Could not open as GridDataset: %s", endpoint);
      return Optional.empty();
    }

    return Optional.of(result.get());
  }

}
