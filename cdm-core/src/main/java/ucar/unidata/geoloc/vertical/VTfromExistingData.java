/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.unidata.util.Parameter;
import java.io.IOException;
import java.util.List;

/**
 * This implements a VerticalTransform using an existing 3D variable.
 * This is a common case when the 3D pressure or height field is stored in the file.
 */
@Immutable
public class VTfromExistingData extends AbstractVerticalTransform {
  /** The name of the Parameter whose value is the variable that contains the 2D Height or Pressure field */
  public static final String existingDataField = "existingDataField";

  // The variable that contains the 2D Height or Pressure field
  private final Variable existingDataVar;

  /**
   * Constructor.
   *
   * @param ds containing Dataset
   * @param timeDim time Dimension
   * @param params list of transformation Parameters
   *        TODO: params will change to AttributeContainer in ver7.
   */
  public static VTfromExistingData create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    Variable existingDataVar = findVariableFromParameterName(ds, params, existingDataField);
    String units = existingDataVar.getUnitsString();
    return new VTfromExistingData(timeDim, units, existingDataVar);
  }

  private VTfromExistingData(Dimension timeDim, String units, Variable existingDataVar) {
    super(timeDim, units);
    this.existingDataVar = existingDataVar;
  }

  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array data = readArray(existingDataVar, timeIndex);

    // copy for now - better to just return Array, with promise its rank 3
    int[] shape = data.getShape();
    ArrayDouble.D3 ddata = (ArrayDouble.D3) Array.factory(DataType.DOUBLE, shape);
    MAMath.copyDouble(ddata, data);
    return ddata;
  }

  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  @Override
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {
    ArrayDouble.D3 ddata = getCoordinateArray(timeIndex);
    int[] origin = {0, yIndex, xIndex};
    int[] shape = {ddata.getShape()[0], 1, 1};

    return (ArrayDouble.D1) ddata.section(origin, shape).reduce();
  }

}

