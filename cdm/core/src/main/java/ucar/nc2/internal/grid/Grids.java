package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridAxisOffsetTimeRegular;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

/** static utilities */
class Grids {
  private static final Logger log = LoggerFactory.getLogger(Grids.class);

  static GridAxis1D extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis.DependenceType dependenceType) {
    GridAxis1D.Builder<?> builder;
    AxisType axisType = axis.getAxisType();
    if (axisType == AxisType.Time || axisType == AxisType.RunTime) {
      builder = GridAxis1DTime.builder(axis).setAxisType(axis.getAxisType());
    } else {
      builder = GridAxis1D.builder(axis).setAxisType(axis.getAxisType());
    }
    extractGridAxis1D(ncd, axis, builder, dependenceType);
    return builder.build();
  }

  private static void extractGridAxis1D(NetcdfDataset ncd, CoordinateAxis axis, GridAxis1D.Builder<?> builder,
      GridAxis.DependenceType dependenceTypeFromClassifier) {
    Preconditions.checkArgument(axis.getRank() < 2);

    GridAxis.DependenceType dependenceType;
    if (axis.isCoordinateVariable()) {
      dependenceType = GridAxis.DependenceType.independent;
    } else if (ncd.isIndependentCoordinate(axis)) { // is a coordinate alias
      dependenceType = dependenceTypeFromClassifier; // TODO not clear
      builder.setDependsOn(axis.getDimension(0).getShortName());
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
    builder.setDependenceType(axis.isScalar() ? GridAxis.DependenceType.scalar : dependenceType);

    CoordToGridAxis1D converter = new CoordToGridAxis1D(builder.toString(), readValues(axis), readBounds(axis));
    if (axis.getAxisType() == AxisType.Lon) {
      // Fix discontinuities in longitude axis. These occur when the axis crosses the date line.
      converter.correctLongitudeWrap();
    }
    converter.extract(builder);

    if (builder instanceof GridAxis1DTime.Builder) {
      GridAxis1DTime.Builder<?> timeBuilder = (GridAxis1DTime.Builder<?>) builder;
      CoordinateAxis1DTimeExtractor extractTime = new CoordinateAxis1DTimeExtractor(axis, converter.coords);
      timeBuilder.setTimeHelper(extractTime.timeHelper);
      timeBuilder.setCalendarDates(extractTime.cdates);
    }
  }

  private static Array<Double> readValues(CoordinateAxis axis) {
    Array<?> data;
    try {
      data = axis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
    return Arrays.toDouble(data);
  }

  private static Optional<Array<Double>> readBounds(CoordinateAxis axis) {
    String boundsVarName = axis.attributes().findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return Optional.empty();
    }
    VariableDS boundsVar = (VariableDS) axis.getParentGroup().findVariableLocal(boundsVarName);
    if (null == boundsVar)
      return Optional.empty();
    if (2 != boundsVar.getRank())
      return Optional.empty();
    if (axis.getDimension(0) != boundsVar.getDimension(0))
      return Optional.empty();
    if (2 != boundsVar.getDimension(1).getLength())
      return Optional.empty();

    Array<?> boundsData;
    try {
      boundsData = boundsVar.readArray();
    } catch (IOException e) {
      log.warn("GridAxis1DBuilder.makeBoundsFromAux read failed ", e);
      return Optional.empty();
    }

    return Optional.of(Arrays.toDouble(boundsData));
  }

  static GridAxisOffsetTimeRegular extractGridAxisOffset2D(CoordinateAxis axis, GridAxis.DependenceType dependenceType,
      Map<String, GridAxis> gridAxes) {
    Preconditions.checkArgument(axis.getAxisType() == AxisType.TimeOffset);
    Preconditions.checkArgument(axis.getRank() == 2);
    GridAxisOffsetTimeRegular.Builder<?> builder =
        GridAxisOffsetTimeRegular.builder(axis).setAxisType(axis.getAxisType()).setDependenceType(dependenceType);

    CoordinateAxis2DExtractor extract = new CoordinateAxis2DExtractor(axis);
    builder.setMidpoints(extract.getMidpoints());
    builder.setBounds(extract.getBounds());
    builder.setNcoords(extract.getNtimes());
    builder.setHourOffsets(extract.getHourOffsets());
    builder.setSpacing(extract.isInterval() ? GridAxis.Spacing.discontiguousInterval : GridAxis.Spacing.irregularPoint);

    Preconditions.checkNotNull(extract.getRuntimeAxisName());
    GridAxis runtime = gridAxes.get(extract.getRuntimeAxisName());
    Preconditions.checkNotNull(runtime, extract.getRuntimeAxisName());
    Preconditions.checkArgument(runtime instanceof GridAxis1DTime, extract.getRuntimeAxisName());
    Preconditions.checkArgument(runtime.getAxisType() == AxisType.RunTime, extract.getRuntimeAxisName());
    builder.setRuntimeAxis((GridAxis1DTime) runtime);

    return builder.build();
  }

  /** Standard sort on Coordinate Axes */
  public static class AxisComparator implements java.util.Comparator<GridAxis> {
    public int compare(GridAxis c1, GridAxis c2) {
      Preconditions.checkNotNull(c1);
      Preconditions.checkNotNull(c2);
      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();
      Preconditions.checkNotNull(t1);
      Preconditions.checkNotNull(t2);
      return t1.axisOrder() - t2.axisOrder();
    }
  }


}
