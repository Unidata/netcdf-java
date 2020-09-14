/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Variable;

/** Interface for strategies deciding how to chunk netcdf-4 variables. */
public interface Nc4Chunking {

  enum Strategy {
    standard, grib, none
  }

  /** Should this variable be chunked? */
  boolean isChunked(Variable v);

  boolean isChunked(Variable.Builder<?> vb);

  // Get the existing chunking from the attribute CDM.CHUNK_SIZES
  long[] getChunking(Variable v);

  /** Compute the chunk size for this Variable. */
  long[] computeChunking(Variable.Builder<?> vb);

  /** Get the deflation level. 0 corresponds to no compression and 9 to maximum compression. */
  int getDeflateLevel(Variable.Builder<?> vb);

  /**
   * Set true to turn shuffling on which may improve compression. This option is ignored unless a non-zero deflation
   * level is specified.
   */
  boolean isShuffle(Variable.Builder<?> vb);

}
