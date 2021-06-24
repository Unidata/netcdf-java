/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

/**
 * The spacing of the coordinate values.
 */
public enum GridAxisSpacing {
  // If making changes, update ucar.gcdm.GcdmGridConverter#convertAxisSpacing(GridAxis.Spacing)
  // and consider if need for addition to gcdm_grid.proto.
  /**
   * Regularly spaced points (start, end, npts); start and end are midpoints, edges halfway between midpoints,
   * resol = (start - end) / (npts-1)
   */
  regularPoint, //
  /**
   * Irregular spaced points values[npts]; edges halfway between coords.
   */
  irregularPoint, //

  /**
   * Regular contiguous intervals (start, end, npts); start and end are edges, midpoints halfway between edges,
   * resol = (start - end) / npts.
   */
  regularInterval, //
  /**
   * Irregular contiguous intervals values[npts+1]; values are the edges, midpoints halfway between edges.
   */
  contiguousInterval, //
  /**
   * Irregular discontiguous intervals values[2*npts]; values are the edges: low0, high0, low1, high1, ...
   * Note that monotonicity is not guaranteed, and is ambiguous.
   */
  discontiguousInterval; //

  /**
   * If the spacing is regular.
   */
  public boolean isRegular() {
    return (this == GridAxisSpacing.regularPoint) || (this == GridAxisSpacing.regularInterval);
  }

  /**
   * If the coordinate values are intervals.
   */
  public boolean isInterval() {
    return this == GridAxisSpacing.regularInterval || this == GridAxisSpacing.contiguousInterval
        || this == GridAxisSpacing.discontiguousInterval;
  }
}
