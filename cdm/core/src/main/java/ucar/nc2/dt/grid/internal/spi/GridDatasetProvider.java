/*
 * Copyright (c) 2022 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dt.grid.internal.spi;

import java.io.IOException;
import java.util.Formatter;
import java.util.Set;
import javax.annotation.Nullable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dt.GridDataset;

/**
 * A special SPI to register GridDataset providers.
 * <p>
 * FOR INTERNAL USE ONLY
 */
public interface GridDatasetProvider {

  default boolean isMine(NetcdfDataset ncd) {
    return false;
  }

  default boolean isMine(String location, Set<Enhance> enhanceMode) {
    return false;
  }

  @Nullable
  default GridDataset open(String location) throws IOException {
    return open(location, NetcdfDataset.getDefaultEnhanceMode());
  }

  @Nullable
  GridDataset open(String location, Set<NetcdfDataset.Enhance> enhanceMode) throws IOException;

  @Nullable
  default GridDataset open(NetcdfDataset ncd) throws IOException {
    return open(ncd, null);
  }

  @Nullable
  GridDataset open(NetcdfDataset ncd, Formatter parseInfo) throws IOException;
}
