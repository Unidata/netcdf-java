/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.AttributeContainer;
import ucar.nc2.Dimension;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Formatter;
import java.util.Optional;

/**
 * Implement CF vertical coordinate "atmosphere_ln_pressure_coordinate"
 * pressure(z) = p0 * exp(-lev(k))" .
 *
 * LOOK since its not 3D, we dont know what the 2D extent is.
 * So we can implement getCoordinateArray1D but not getCoordinateArray3D.
 */
@Immutable
public class AtmosLnPressure extends AbstractVerticalTransform {
  public static final String P0 = "ReferencePressureVariableName";
  public static final String LEV = "VerticalCoordinateVariableName";

  /**
   * Create a vertical transform for CF vertical coordinate "atmosphere_ln_pressure_coordinate"
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   */
  public static Optional<AtmosLnPressure> create(DataReader ds, Dimension timeDim, AttributeContainer params,
      Formatter errlog) {
    String p0name = params.findAttributeString(P0, null);
    if (p0name == null) {
      errlog.format("AtmosLnPressure: no p arameter names %s%n", P0);
      return Optional.empty();
    }
    String levName = params.findAttributeString(LEV, null);
    if (levName == null) {
      errlog.format("AtmosLnPressure: no p arameter names %s%n", LEV);
      return Optional.empty();
    }

    double p0;
    try {
      p0 = ds.read(p0name).getScalar().doubleValue();
    } catch (Exception e) {
      errlog.format("AtmosLnPressure failed to read %s err= %s%n", p0name, e.getMessage());
      return Optional.empty();
    }
    String units = ds.getUnits(p0name);

    Array<Number> levArray;
    try {
      levArray = ds.read(levName);
      if (levArray.getRank() != 1) {
        errlog.format("AtmosLnPressure %s = %s must be 1 dimensional%n", LEV, levName);
        return Optional.empty();
      }
    } catch (Exception e) {
      errlog.format("AtmosLnPressure failed to read %s err= %s%n", levName, e.getMessage());
      return Optional.empty();
    }

    int n = (int) Arrays.computeSize(levArray.getShape());
    double[] pressure = new double[n];
    int count = 0;
    for (Number levValue : levArray) {
      pressure[count++] = p0 * Math.exp(-levValue.doubleValue());
    }

    Array<Number> pressureArray = Arrays.factory(ArrayType.DOUBLE, levArray.getShape(), pressure);
    return Optional.of(new AtmosLnPressure(timeDim, units, pressureArray));
  }

  ////////////////////////////////////////////////////////////////////
  private final Array<Number> pressure;

  private AtmosLnPressure(Dimension timeDim, String units, Array<Number> pressure) {
    super(timeDim, units);
    this.pressure = pressure;
  }

  @Nullable
  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) {
    return null;
  }

  @Override
  public Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) {
    return pressure;
  }

}


