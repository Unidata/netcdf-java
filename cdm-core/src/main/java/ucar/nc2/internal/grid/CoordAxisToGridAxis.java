package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridAxisPoint;
import ucar.nc2.grid.GridAxisInterval;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/** Create a GridAxis from a CoordinateAxis */
class CoordAxisToGridAxis {
  private static final Logger log = LoggerFactory.getLogger(CoordAxisToGridAxis.class);

  static CoordAxisToGridAxis create(CoordinateAxis axis, GridAxisDependenceType dependenceTypeFromClassifier,
      boolean isIndependent) {
    if (axis.getAxisType().isTime() && axis.getArrayType().isString()) {
      ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);
      return new CoordAxisToGridAxis(extract.getConvertedAxis(), dependenceTypeFromClassifier, isIndependent);
    }
    return new CoordAxisToGridAxis(axis, dependenceTypeFromClassifier, isIndependent);
  }

  private final CoordinateAxis axis;
  private final GridAxisDependenceType dependenceTypeFromClassifier;
  private final boolean isIndependent;

  private CoordAxisToGridAxis(CoordinateAxis axis, GridAxisDependenceType dependenceTypeFromClassifier,
      boolean isIndependent) {
    Preconditions.checkArgument(axis.getRank() < 2);
    this.axis = axis;
    this.dependenceTypeFromClassifier = dependenceTypeFromClassifier;
    this.isIndependent = isIndependent;
  }

  GridAxis<?> extractGridAxis() {
    ExtractCoordinateValues converter =
        new ExtractCoordinateValues(axis.getShortName(), readValues(), readBounds(), axis.getAxisType().isHoriz());
    if (axis.getAxisType() == AxisType.Lon) {
      // Fix discontinuities in longitude axis. These occur when the axis crosses the date line.
      converter.correctLongitudeWrap();
    }

    if (converter.isInterval) {
      GridAxisInterval.Builder<?> builder = GridAxisInterval.builder();
      extractGridAxis1D(builder);
      converter.extractToIntervalBuilder(builder);
      return builder.build();
    } else {
      GridAxisPoint.Builder<?> builder = GridAxisPoint.builder();
      extractGridAxis1D(builder);
      converter.extractToPointBuilder(builder);
      return builder.build();
    }
  }

  private void extractGridAxis1D(GridAxis.Builder<?> builder) {
    Preconditions.checkArgument(axis.getRank() < 2);

    builder.setName(axis.getShortName()).setUnits(axis.getUnitsString()).setDescription(axis.getDescription())
        .setAttributes(axis.attributes()).setAxisType(axis.getAxisType());

    GridAxisDependenceType dependenceType;
    if (axis.isScalar()) {
      dependenceType = GridAxisDependenceType.scalar;
    } else if (axis.isCoordinateVariable()) {
      dependenceType = GridAxisDependenceType.independent;
    } else if (this.isIndependent) { // is a coordinate alias
      dependenceType = dependenceTypeFromClassifier; // TODO not clear
      builder.setDependsOn(axis.getDimension(0).getShortName());
    } else {
      dependenceType = GridAxisDependenceType.dependent;
      ArrayList<String> dependsOn = new ArrayList<>();
      for (Dimension d : axis.getDimensions()) { // LOOK axes may not exist
        dependsOn.add(d.makeFullName(axis));
      }
      builder.setDependsOn(dependsOn);
    }
    builder.setDependenceType(axis.isScalar() ? GridAxisDependenceType.scalar : dependenceType);
  }

  private Array<Double> readValues() {
    Array<?> data;
    try {
      data = axis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
    return Arrays.toDouble(data);
  }

  private Optional<Array<Double>> readBounds() {
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

  /*
   * static GridAxisOffsetTimeRegular extractGridAxisOffset2D(CoordinateAxis axis, GridAxisDependenceType
   * dependenceType,
   * Map<String, GridAxis<?>> gridAxes) {
   * Preconditions.checkArgument(axis.getAxisType() == AxisType.TimeOffset);
   * Preconditions.checkArgument(axis.getRank() == 2);
   * GridAxisOffsetTimeRegular.Builder<?> builder =
   * GridAxisOffsetTimeRegular.builder(axis).setAxisType(axis.getAxisType()).setDependenceType(dependenceType);
   * 
   * CoordinateAxis2DExtractor extract = new CoordinateAxis2DExtractor(axis);
   * builder.setMidpoints(extract.getMidpoints());
   * builder.setBounds(extract.getBounds());
   * builder.setMinutesOffsets(extract.getMinutesOffsets());
   * builder.setSpacing(extract.isInterval() ? GridAxis.Spacing.discontiguousInterval :
   * GridAxis.Spacing.irregularPoint);
   * 
   * Preconditions.checkNotNull(extract.getRuntimeAxisName());
   * GridAxis runtime = gridAxes.get(extract.getRuntimeAxisName());
   * Preconditions.checkNotNull(runtime, extract.getRuntimeAxisName());
   * Preconditions.checkArgument(runtime instanceof GridAxis1DTime, extract.getRuntimeAxisName());
   * Preconditions.checkArgument(runtime.getAxisType() == AxisType.RunTime, extract.getRuntimeAxisName());
   * builder.setRuntimeAxis((GridAxis1DTime) runtime);
   * 
   * return builder.build();
   * }
   */
}
