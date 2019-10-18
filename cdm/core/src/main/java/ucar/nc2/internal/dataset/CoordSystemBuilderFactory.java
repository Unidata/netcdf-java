package ucar.nc2.internal.dataset;

import javax.annotation.Nullable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;

public interface CoordSystemBuilderFactory {
  @Nullable
  String getConventionName();

  default boolean isMine(NetcdfFile ncfile) {
    return false; // must have correct convention name, unless overridden.
  }

  CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder);
}
