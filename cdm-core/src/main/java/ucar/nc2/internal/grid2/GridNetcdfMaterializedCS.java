/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.grid2;

import ucar.array.Range;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.MaterializedCoordinateSystem;

import javax.annotation.Nullable;
import java.util.List;

public class GridNetcdfMaterializedCS implements MaterializedCoordinateSystem {

  @Override
  public GridNetcdfTimeCS getTimeCoordSystem() {
    return tcs;
  }

  @Nullable
  @Override
  public GridAxisPoint getEnsembleAxis() {
    return ens;
  }

  @Nullable
  @Override
  public GridAxis<?> getVerticalAxis() {
    return vert;
  }

  @Override
  public GridNetcdfHorizCS getHorizCoordSystem() {
    return hcs;
  }

  //////////////////////////////////////////////////////////////////////////
  private final GridNetcdfTimeCS tcs;
  private final GridNetcdfHorizCS hcs;
  private final GridAxisPoint ens;
  private final GridAxis<?> vert;
  private final List<Range> ranges;

  private GridNetcdfMaterializedCS(Builder builder) {
    this.tcs = builder.tcs;
    this.hcs = builder.hcs;
    this.ens = builder.ens;
    this.vert = builder.vert;
    this.ranges = builder.ranges;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    return builder().setTimeCoordSys(this.tcs).setHorizCoordSys(this.hcs).setEnsAxis(this.ens).setVertAxis(this.vert);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private GridNetcdfTimeCS tcs;
    private GridNetcdfHorizCS hcs;
    private GridAxisPoint ens;
    private GridAxis<?> vert;
    private List<Range> ranges;
    private boolean built;

    public Builder setTimeCoordSys(GridNetcdfTimeCS tcs) {
      this.tcs = tcs;
      return this;
    }

    public Builder setHorizCoordSys(GridNetcdfHorizCS hcs) {
      this.hcs = hcs;
      return this;
    }

    public Builder setEnsAxis(GridAxisPoint ens) {
      this.ens = ens;
      return this;
    }

    public Builder setVertAxis(GridAxis<?> vert) {
      this.vert = vert;
      return this;
    }

    public Builder setRanges(List<Range> ranges) {
      this.ranges = ranges;
      return this;
    }

    public GridNetcdfMaterializedCS build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridNetcdfMaterializedCS(this);
    }
  }
}
