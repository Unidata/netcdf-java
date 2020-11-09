/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import com.google.common.base.Preconditions;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;
import java.io.IOException;
import java.util.List;

/**
 * Create a 3D height(z,y,x) array using the netCDF CF convention formula for
 * "atmosphere_hybrid_sigma_pressure_coordinate".
 * <p>
 * <strong>pressure(x,y,z) = a(z)*p0 + b(z)*surfacePressure(x,y)</strong>
 * or
 * <p>
 * <strong>pressure(x,y,z) = ap(z) + b(z)*surfacePressure(x,y)</strong>
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#_atmosphere_hybrid_sigma_pressure_coordinate"
 */
@Immutable
public class HybridSigmaPressure extends AbstractVerticalTransform {

  /** P-naught identifier */
  public static final String P0 = "P0_variableName";

  /** Surface pressure name identifier */
  public static final String PS = "SurfacePressure_variableName";

  /** The "a" variable name identifier */
  public static final String A = "A_variableName";

  /** The "ap" variable name identifier */
  public static final String AP = "AP_variableName";

  /** The "b" variable name identifier */
  public static final String B = "B_variableName";

  private final Variable aVar, bVar, psVar;

  // Scale factor for the a array.
  private final double scaleA;

  /**
   * Construct a coordinate transform for sigma pressure
   *
   * @param ds netCDF dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   *        TODO: params will change to AttributeContainer in ver7.
   */
  public static HybridSigmaPressure create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    Variable psVar = findVariableFromParameterName(ds, params, PS);
    String units = psVar.findAttributeString(CDM.UNITS, "none");

    // Either A or AP
    String aName = getParameterStringValue(params, A);
    String apName = getParameterStringValue(params, AP);
    Preconditions.checkArgument(aName != null || apName != null,
        String.format("HybridSigmaPressure %s or %s parameter must be set", A, AP));
    Variable aVar;
    double scaleA;

    if (apName != null) { // ap(z) + b(z)*surfacePressure(x,y)
      aVar = ds.findVariable(apName);
      Preconditions.checkNotNull(aVar, String.format("HybridSigmaPressure %s not found", apName));
      String apUnits = aVar.findAttributeString(CDM.UNITS, null);
      scaleA = (apUnits == null) ? 1.0 : convertUnitFactor(apUnits, units);

    } else { // a(z)*p0 + b(z)*surfacePressure(x,y)
      aVar = ds.findVariable(aName);
      Preconditions.checkNotNull(aVar, String.format("HybridSigmaPressure %s not found", aName));

      Variable p0Var = findVariableFromParameterName(ds, params, P0);
      scaleA = readAndConvertUnit(p0Var, units);
    }

    Variable bVar = findVariableFromParameterName(ds, params, B);

    return new HybridSigmaPressure(timeDim, units, aVar, bVar, psVar, scaleA);
  }

  private HybridSigmaPressure(Dimension timeDim, String units, Variable aVar, Variable bVar, Variable psVar,
      double scaleA) {
    super(timeDim, units);
    this.psVar = psVar;
    this.aVar = aVar;
    this.bVar = bVar;
    this.scaleA = scaleA;
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws IOException problem reading data
   * @throws InvalidRangeException _more_
   */
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array psArray = readArray(psVar, timeIndex);
    Array aArray = aVar.read();
    Array bArray = bVar.read();

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    // it's possible to have rank 3 because pressure can have a level, usually 1
    // Check if rank 3 and try to reduce
    if (psArray.getRank() == 3)
      psArray = psArray.reduce(0);

    int[] shape2D = psArray.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    Index psIndex = psArray.getIndex();

    ArrayDouble.D3 press = new ArrayDouble.D3(nz, ny, nx);

    double ps;
    for (int z = 0; z < nz; z++) {
      double term1 = aArray.getDouble(aIndex.set(z)) * scaleA;
      double bz = bArray.getDouble(bIndex.set(z));

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          ps = psArray.getDouble(psIndex.set(y, x));
          press.set(z, y, x, term1 + bz * ps);
        }
      }
    }

    return press;
  }

  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   * @throws java.io.IOException problem reading data
   * @throws ucar.ma2.InvalidRangeException _more_
   */
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {

    Array psArray = readArray(psVar, timeIndex);
    Array aArray = aVar.read();
    Array bArray = bVar.read();

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    // it's possible to have rank 3 because pressure can have a level, usually 1
    // Check if rank 3 and try to reduce
    if (psArray.getRank() == 3)
      psArray = psArray.reduce(0);

    Index psIndex = psArray.getIndex();

    ArrayDouble.D1 press = new ArrayDouble.D1(nz);

    double ps;
    for (int z = 0; z < nz; z++) {
      double term1 = aArray.getDouble(aIndex.set(z)) * scaleA;
      double bz = bArray.getDouble(bIndex.set(z));
      ps = psArray.getDouble(psIndex.set(yIndex, xIndex));
      press.set(z, term1 + bz * ps);
    }
    return press;
  }

}

