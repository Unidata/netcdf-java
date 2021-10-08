/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.internal.grid.CylindricalCoord;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A Coordinate System for materialized gridded data, ie that has been read into memory. */
@Immutable
public class MaterializedCoordinateSystem {

  /** Get the GridTimeCoordinateSystem, may be null. */
  @Nullable
  public GridTimeCoordinateSystem getTimeCoordSystem() {
    return tcs;
  }

  /** Get the Ensemble GridAxis, may be null. */
  @Nullable
  public GridAxisPoint getEnsembleAxis() {
    return ens;
  }

  /** Get the Vertical GridAxis, may be null. */
  @Nullable
  public GridAxis<?> getVerticalAxis() {
    return vert;
  }

  /** Get the GridHorizCoordinateSystem. */
  public GridHorizCoordinateSystem getHorizCoordinateSystem() {
    return hcs;
  }

  /** Get the X axis (either GeoX or Lon). */
  public GridAxisPoint getXHorizAxis() {
    return getHorizCoordinateSystem().getXHorizAxis();
  }

  /** Get the Y axis (either GeoY or Lat). */
  public GridAxisPoint getYHorizAxis() {
    return getHorizCoordinateSystem().getYHorizAxis();
  }

  /** The shape of this array. */
  public List<Integer> getMaterializedShape() {
    List<Integer> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getMaterializedShape());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getNominalSize());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getNominalSize());
    }
    result.addAll(getHorizCoordinateSystem().getShape());
    return result;
  }

  /** For subsets, the ranges of the original axes that constitutes the subset. */
  public List<ucar.array.Range> getSubsetRanges() {
    List<ucar.array.Range> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      result.addAll(getTimeCoordSystem().getSubsetRanges());
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis().getSubsetRange());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis().getSubsetRange());
    }
    result.add(getHorizCoordinateSystem().getYHorizAxis().getSubsetRange());
    result.add(getHorizCoordinateSystem().getXHorizAxis().getSubsetRange());
    return result;
  }

  /** Get the GridAxes in this MaterializedCoordinateSystem. */
  public List<GridAxis<?>> getGridAxes() {
    List<GridAxis<?>> result = new ArrayList<>();
    if (getTimeCoordSystem() != null) {
      if (getTimeCoordSystem().getRunTimeAxis() != null) {
        result.add(getTimeCoordSystem().getRunTimeAxis());
      }
      result.add(getTimeCoordSystem().getTimeOffsetAxis(0));
    }
    if (getEnsembleAxis() != null) {
      result.add(getEnsembleAxis());
    }
    if (getVerticalAxis() != null) {
      result.add(getVerticalAxis());
    }
    result.add(getHorizCoordinateSystem().getYHorizAxis());
    result.add(getHorizCoordinateSystem().getXHorizAxis());
    return result;
  }

  /** Public by accident. */
  public boolean specialReadNeeded() {
    return lonCoordinate != null;
  }

  /** Public by accident. */
  public Array<Number> readSpecial(Grid grid) throws InvalidRangeException, IOException {
    return lonCoordinate.readSpecial(this, grid);
  }

  //////////////////////////////////////////////////////////////////////////
  private final GridTimeCoordinateSystem tcs;
  private final GridHorizCoordinateSystem hcs;
  private final GridAxisPoint ens;
  private final GridAxis<?> vert;
  private final ImmutableList<Range> ranges; // LOOK needed?
  private final @Nullable CylindricalCoord lonCoordinate;

  private MaterializedCoordinateSystem(Builder builder) {
    this.tcs = builder.tcs;
    this.hcs = builder.hcs;
    this.ens = builder.ens;
    this.vert = builder.vert;
    this.ranges = builder.ranges;
    this.lonCoordinate = builder.lonCoordinate;
  }

  /** Turn into a mutable Builder. Can use toBuilder().build() to copy. */
  public Builder toBuilder() {
    return builder().setTimeCoordSys(this.tcs).setHorizCoordSys(this.hcs).setEnsAxis(this.ens).setVertAxis(this.vert);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private GridTimeCoordinateSystem tcs;
    private GridHorizCoordinateSystem hcs;
    private GridAxisPoint ens;
    private GridAxis<?> vert;
    private ImmutableList<Range> ranges; // LOOK: needed?
    private CylindricalCoord lonCoordinate;
    private boolean built;

    public Builder setTimeCoordSys(GridTimeCoordinateSystem tcs) {
      this.tcs = tcs;
      return this;
    }

    public Builder setHorizCoordSys(GridHorizCoordinateSystem hcs) {
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

    // LOOK needed ?
    public Builder setRanges(List<Range> ranges) {
      this.ranges = ImmutableList.copyOf(ranges);
      return this;
    }

    public Builder setCylindricalCoord(CylindricalCoord lonCoordinate) {
      this.lonCoordinate = lonCoordinate;
      return this;
    }

    public MaterializedCoordinateSystem build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new MaterializedCoordinateSystem(this);
    }
  }

}
