/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import com.google.common.collect.ImmutableList;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisDependenceType;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid2.GridTimeCoordinateSystem;
import ucar.nc2.grid2.MaterializedCoordinateSystem;
import ucar.nc2.internal.grid2.GridNetcdfHorizCS;

import java.util.Formatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Grib implementation of {@link GridCoordinateSystem} */
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
    List<String> names = getGridAxes().stream().filter(a -> a.getDependenceType() == GridAxisDependenceType.independent)
        .map(a -> a.getName()).collect(Collectors.toList());
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
  public GridTimeCoordinateSystem getTimeCoordinateSystem() {
    return tcs;
  }

  @Override
  public GridHorizCoordinateSystem getHorizCoordinateSystem() {
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

  /////////////////////////////////////////////////////////////////////////////////////////
  public Optional<MaterializedCoordinateSystem> subset(GridSubset params, Formatter errlog) {
    MaterializedCoordinateSystem.Builder builder = MaterializedCoordinateSystem.builder();
    AtomicBoolean fail = new AtomicBoolean(false); // gets around need for final variable in lambda

    if (tcs != null) {
      tcs.subset(params, errlog).ifPresentOrElse(builder::setTimeCoordSys, () -> fail.set(true));
    }
    if (getEnsembleAxis() != null) {
      getEnsembleAxis().subset(params, errlog).ifPresentOrElse(builder::setEnsAxis, () -> fail.set(true));
    }
    if (getVerticalAxis() != null) {
      getVerticalAxis().subset(params, errlog).ifPresentOrElse(builder::setVertAxis, () -> fail.set(true));
    }
    hcs.subset(params, errlog).ifPresentOrElse(hcs -> builder.setHorizCoordSys((GridNetcdfHorizCS) hcs),
        () -> fail.set(true));

    if (fail.get()) {
      return Optional.empty();
    } else {
      return Optional.of(builder.build());
    }
  }
}
