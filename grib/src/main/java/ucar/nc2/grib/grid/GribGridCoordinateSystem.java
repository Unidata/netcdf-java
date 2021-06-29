/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.collect.ImmutableList;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid2.GridTimeCoordinateSystem;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;

public class GribGridCoordinateSystem implements GridCoordinateSystem {
  private final ImmutableList<GridAxis<?>> axes;
  private final GribGridHorizCoordinateSystem hcs;
  private final GribGridTimeCoordinateSystem tcs;

  public GribGridCoordinateSystem(List<GridAxis<?>> axes, GribGridTimeCoordinateSystem tcs,
      GribGridHorizCoordinateSystem hcs) {
    this.axes = ImmutableList.copyOf(axes); // LOOK sort
    this.tcs = tcs;
    this.hcs = hcs;
  }

  @Override
  public String getName() {
    List<String> names = getGridAxes().stream().map(a -> a.getName()).collect(Collectors.toList());
    return String.join(" ", names);
  }

  @Override
  public ImmutableList<GridAxis<?>> getGridAxes() {
    ImmutableList.Builder<GridAxis<?>> builder = ImmutableList.builder();
    builder.addAll(axes);
    builder.add(hcs.getYHorizAxis());
    builder.add(hcs.getXHorizAxis());
    return builder.build();
  }

  @Override
  public GridTimeCoordinateSystem getTimeCoordSystem() {
    return tcs;
  }

  @Override
  public GridHorizCoordinateSystem getHorizCoordSystem() {
    return hcs;
  }

  @Override
  public String showFnSummary() {
    return null;
  }

  @Override
  public String toString() {
    return "GribGridCoordinateSystem{" + "axes=" + axes + ", hcs=" + hcs + ", tcs=" + tcs + '}';
  }
}
