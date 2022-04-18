package ucar.nc2.dataset.spi;

import javax.annotation.Nullable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.dataset.CoordSystemBuilder;

/**
 * A Service Provider of CoordSystemBuilder.
 */
public interface CoordSystemBuilderFactory {
  @Nullable
  String getConventionName();

  default boolean isMine(NetcdfFile ncfile) {
    return false; // if false, must have correct convention name.
  }

  CoordSystemBuilder open(NetcdfDataset.Builder datasetBuilder);
}
