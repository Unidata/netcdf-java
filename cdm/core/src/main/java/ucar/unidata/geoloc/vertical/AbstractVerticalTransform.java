/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.VerticalTransform;
import ucar.unidata.util.Parameter;
import java.io.IOException;
import java.util.List;

/** Abstract superclass for implementations of VerticalTransform. */
@Immutable
abstract class AbstractVerticalTransform implements VerticalTransform {
  protected final String units;
  private final Dimension timeDim;

  /**
   * Construct a VerticalCoordinate
   *
   * @param timeDim time dimension
   * @param units vertical coordinate unit
   */
  AbstractVerticalTransform(Dimension timeDim, @Nullable String units) {
    this.timeDim = timeDim;
    this.units = units;
  }

  @Override
  public abstract ucar.ma2.ArrayDouble.D3 getCoordinateArray(int timeIndex)
      throws java.io.IOException, InvalidRangeException;

  @Override
  public abstract ucar.ma2.ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException;

  @Override
  @Nullable
  public String getUnitString() {
    return units;
  }

  @Override
  public boolean isTimeDependent() {
    return (timeDim != null);
  }

  /** Get the time Dimension, if it exists */
  protected Dimension getTimeDimension() {
    return timeDim;
  }

  /**
   * Read the data {@link ucar.ma2.Array} from the variable, at the specified
   * time index if applicable. If the variable does not have a time
   * dimension, the data array will have the same rank as the Variable.
   * If the variable has a time dimension, the data array will have rank-1.
   *
   * @param v variable to read
   * @param timeIndex time index, ignored if !isTimeDependent()
   * @return Array from the variable at that time index
   *
   * @throws IOException problem reading data
   * @throws InvalidRangeException _more_
   */
  Array readArray(Variable v, int timeIndex) throws IOException, InvalidRangeException {
    int[] shape = v.getShape();
    int[] origin = new int[v.getRank()];

    if (getTimeDimension() != null) {
      int dimIndex = v.findDimensionIndex(getTimeDimension().getShortName());
      if (dimIndex >= 0) {
        shape[dimIndex] = 1;
        origin[dimIndex] = timeIndex;
        return v.read(origin, shape).reduce(dimIndex);
      }
    }

    return v.read(origin, shape);
  }

  @Override
  public VerticalTransform subset(Range t_range, Range z_range, Range y_range, Range x_range) {
    return new VerticalTransformSubset(this, t_range, z_range, y_range, x_range);
  }

  static Variable findVariableFromParameterName(NetcdfFile ds, List<Parameter> params, String paramName) {
    String vName = getParameterStringValue(params, paramName);
    Preconditions.checkNotNull(vName, String.format("VerticalTransform parameter %s not found", paramName));
    Variable v = ds.findVariable(vName);
    Preconditions.checkNotNull(v, String.format("VerticalTransform variable %s not found", vName));
    return v;
  }

  @Nullable
  static String getParameterStringValue(List<Parameter> params, String name) {
    for (Parameter a : params) {
      if (name.equalsIgnoreCase(a.getName()))
        return a.getStringValue();
    }
    return null;
  }

  static boolean getParameterBooleanValue(List<Parameter> params, String name) {
    for (Parameter p : params) {
      if (name.equalsIgnoreCase(p.getName()))
        return Boolean.parseBoolean(p.getStringValue());
    }
    return false;
  }

  static double readAndConvertUnit(Variable scalarVariable, String convertToUnit) {
    double result;
    try {
      result = scalarVariable.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "VerticalTransform failed to read " + scalarVariable + " err= " + e.getMessage());
    }

    String scalarUnitStr = scalarVariable.findAttributeString(CDM.UNITS, null);
    // if no unit is given, assume its the correct unit
    if (scalarUnitStr != null && !convertToUnit.equalsIgnoreCase(scalarUnitStr)) {
      // Convert result to units of wantUnit
      double factor = convertUnitFactor(scalarUnitStr, convertToUnit);
      result *= factor;
    }
    return result;
  }

  static double convertUnitFactor(String unit, String convertToUnit) {
    SimpleUnit wantUnit = SimpleUnit.factory(convertToUnit);
    SimpleUnit haveUnit = SimpleUnit.factory(unit);
    return haveUnit.convertTo(1.0, wantUnit);
  }

}

