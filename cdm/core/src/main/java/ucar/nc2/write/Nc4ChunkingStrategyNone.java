/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.write;

import ucar.nc2.Variable;

/** No chunking is done. */
public class Nc4ChunkingStrategyNone extends Nc4ChunkingDefault {
  @Override
  public boolean isChunked(Variable v) {
    return false;
  }

  @Override
  public boolean isChunked(Variable.Builder<?> vb) {
    return false;
  }

  @Override
  public int getDeflateLevel(Variable.Builder<?> v) {
    return 0;
  }

  @Override
  public boolean isShuffle(Variable.Builder<?> v) {
    return false;
  }
}
