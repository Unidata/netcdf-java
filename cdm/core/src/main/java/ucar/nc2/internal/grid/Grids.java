package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridAxis2DTime;

import java.util.ArrayList;

/** static utilities */
class Grids {

  static GridAxis2DTime extractGridAxisTime2D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis1DTime runtime) {
    GridAxis2DTime.Builder<?> builder = GridAxis2DTime.builder().initFromVariableDS(axis);
    builder.setRuntimeAxis(runtime);
    return builder.build();
  }

  static GridAxis1D extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis) {
    GridAxis1D.Builder<?> builder;
    AxisType axisType = axis.getAxisType();
    if (axisType == AxisType.Time || axisType == AxisType.RunTime) {
      builder = GridAxis1DTime.builder(axis).setAxisType(axis.getAxisType());
    } else {
      builder = GridAxis1D.builder(axis).setAxisType(axis.getAxisType());
    }
    extractGridAxis1D(ncd, axis, builder);
    return builder.build();
  }

  private static void extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis1D.Builder<?> builder) {
    Preconditions.checkArgument(axis.getRank() < 2);
    CoordinateAxis1DExtractor extract = new CoordinateAxis1DExtractor(axis);

    GridAxis.DependenceType dependenceType;
    if (axis.isCoordinateVariable()) {
      dependenceType = GridAxis.DependenceType.independent;
    } else if (ncd.isIndependentCoordinate(axis)) { // is a coordinate alias
      dependenceType = GridAxis.DependenceType.independent;
      builder.setDependsOn(ImmutableList.of(axis.getDimension(0).getShortName()));
    } else if (axis.isScalar()) {
      dependenceType = GridAxis.DependenceType.scalar;
    } else {
      dependenceType = GridAxis.DependenceType.dependent;
      ArrayList<String> dependsOn = new ArrayList<>();
      for (Dimension d : axis.getDimensions()) { // LOOK axes may not exist
        dependsOn.add(d.makeFullName(axis));
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
      CoordinateAxis1DTimeExtractor extractTime = new CoordinateAxis1DTimeExtractor(axis, extract.coords);
      timeBuilder.setTimeHelper(extractTime.timeHelper);
      timeBuilder.setCalendarDates(extractTime.cdates);
    }
  }

}
