package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.Dimension;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;

import java.util.ArrayList;

/** static utilities */
class Grids {

  static GridAxis1D extractGridAxis1D(CoordinateAxis axis, boolean isIndependent) {
    GridAxis1D.Builder<?> builder;
    if (axis.getAxisType().isTime()) {
      builder = GridAxis1DTime.builder(axis).setAxisType(axis.getAxisType());
    } else {
      builder = GridAxis1D.builder(axis).setAxisType(axis.getAxisType());
    }
    extractGridAxis1D(axis, builder, isIndependent);
    return builder.build();
  }

  private static void extractGridAxis1D(CoordinateAxis dtCoordAxis, GridAxis1D.Builder<?> builder,
      boolean isIndependent) {
    Preconditions.checkArgument(dtCoordAxis.getRank() < 2);
    CoordinateAxis1DExtractor extract = new CoordinateAxis1DExtractor(dtCoordAxis);

    GridAxis.DependenceType dependenceType;
    if (dtCoordAxis.isCoordinateVariable()) {
      dependenceType = GridAxis.DependenceType.independent;
    } else if (isIndependent) { // its a coordinate alias
      dependenceType = GridAxis.DependenceType.independent;
      builder.setDependsOn(ImmutableList.of(dtCoordAxis.getDimension(0).getShortName()));
    } else if (dtCoordAxis.isScalar()) {
      dependenceType = GridAxis.DependenceType.scalar;
    } else {
      dependenceType = GridAxis.DependenceType.dependent;
      ArrayList<String> dependsOn = new ArrayList<>();
      for (Dimension d : dtCoordAxis.getDimensions()) { // LOOK axes may not exist
        dependsOn.add(d.makeFullName(dtCoordAxis));
      }
      builder.setDependsOn(dependsOn);
    }
    builder.setDependenceType(dependenceType);

    // Fix discontinuities in longitude axis. These occur when the axis crosses the date line.
    extract.correctLongitudeWrap();

    builder.setNcoords(extract.ncoords);
    if (extract.isRegular) {
      builder.setSpacing(GridAxis.Spacing.regularPoint);
      double ending = extract.start + extract.increment * extract.ncoords;
      builder.setGenerated(extract.ncoords, extract.start, ending, extract.increment);

    } else if (!extract.isInterval) {
      builder.setSpacing(GridAxis.Spacing.irregularPoint);
      builder.setValues(extract.coords);
      double starting = extract.edge[0];
      double ending = extract.edge[extract.ncoords - 1];
      double resolution = (extract.ncoords == 1) ? 0.0 : (ending - starting) / (extract.ncoords - 1);
      builder.setResolution(Math.abs(resolution));

    } else if (extract.boundsAreContiguous) {
      builder.setSpacing(GridAxis.Spacing.contiguousInterval);
      builder.setValues(extract.edge);
      double starting = extract.edge[0];
      double ending = extract.edge[extract.ncoords];
      double resolution = (ending - starting) / extract.ncoords;
      builder.setResolution(Math.abs(resolution));

    } else {
      builder.setSpacing(GridAxis.Spacing.discontiguousInterval);
      double[] bounds = new double[2 * extract.ncoords];
      int count = 0;
      for (int i = 0; i < extract.ncoords; i++) {
        bounds[count++] = extract.bound1[i];
        bounds[count++] = extract.bound2[i];
      }
      builder.setValues(bounds);
      double starting = bounds[0];
      double ending = bounds[2 * extract.ncoords - 1];
      double resolution = (extract.ncoords == 1) ? 0.0 : (ending - starting) / (extract.ncoords - 1);
      builder.setResolution(Math.abs(resolution));
    }

    if (builder instanceof GridAxis1DTime.Builder) {
      GridAxis1DTime.Builder<?> timeBuilder = (GridAxis1DTime.Builder<?>) builder;
      CoordinateAxis1DTimeExtractor extractTime = new CoordinateAxis1DTimeExtractor(dtCoordAxis, extract.coords);
      timeBuilder.setTimeHelper(extractTime.timeHelper);
      timeBuilder.setCalendarDates(extractTime.cdates);
    }
  }

}
