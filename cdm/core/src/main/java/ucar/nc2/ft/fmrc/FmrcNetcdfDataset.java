/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.fmrc;

import ucar.nc2.dataset.NetcdfDataset;

/**
 * A virtual NetcdfDataset built by {@link FmrcDataset}. These datasets have no IOSP, original file, or
 * aggregation backing them, so {@link NetcdfDataset#getLastModified()} cannot determine a last-modified time on its
 * own. Instead, report the most recent last-modified time of the files in the FMRC inventory the dataset was built
 * from.
 */
class FmrcNetcdfDataset extends NetcdfDataset {
  private final long lastModified;

  /**
   * @param lastModified most recent last-modified time (msecs since epoch) of the files in the inventory, or 0 if
   *        unknown
   */
  FmrcNetcdfDataset(long lastModified) {
    this.lastModified = lastModified;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }
}
