/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import java.io.IOException;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.util.Parameter;

/**
 * Create a 3D height(z,y,x) array using the CF formula for "atmospheric sigma vertical coordinate".
 * <p>
 * <strong>pressure(x,y,z) = ptop + sigma(z)*surfacePressure(x,y)</strong>
 */
@Immutable
public class AtmosSigma extends AbstractVerticalTransform {

  /** P-naught identifier */
  public static final String PTOP = "PressureTop_variableName";

  /** Surface pressure name identifier */
  public static final String PS = "SurfacePressure_variableName";

  /** The "depth" variable name identifier */
  public static final String SIGMA = "Sigma_variableName";

  // surface pressure
  private final Variable psVar;
  // The sigma array, function of z
  private final double[] sigma;
  // Top of the model
  private final double ptop;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   *        TODO: params will change to AttributeContainer in ver7.
   */
  public static AtmosSigma create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {

    Variable psVar = findVariableFromParameterName(ds, params, PS);
    String units = psVar.findAttributeString(CDM.UNITS, "none");

    Variable ptopVar = findVariableFromParameterName(ds, params, PTOP);
    double ptop = readAndConvertUnit(ptopVar, units);

    Variable sigmaVar = findVariableFromParameterName(ds, params, SIGMA);
    double[] sigma;
    try {
      Array data = sigmaVar.read();
      sigma = (double[]) data.get1DJavaArray(double.class);
    } catch (IOException e) {
      throw new IllegalArgumentException("AtmosSigma failed to read " + sigmaVar + " err= " + e.getMessage());
    }

    return new AtmosSigma(timeDim, units, psVar, sigma, ptop);
  }

  private AtmosSigma(Dimension timeDim, String units, Variable psVar, double[] sigma, double ptop) {
    super(timeDim, units);
    this.psVar = psVar;
    this.sigma = sigma;
    this.ptop = ptop;
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   */
  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array ps = readArray(psVar, timeIndex);
    Index psIndex = ps.getIndex();

    int nz = sigma.length;
    int[] shape2D = ps.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        double psVal = ps.getDouble(psIndex.set(y, x));
        for (int z = 0; z < nz; z++) {
          result.set(z, y, x, ptop + sigma[z] * (psVal - ptop));
        }
      }
    }

    return result;
  }

  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * (needds test!!!)
   * 
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  @Override
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {

    Array ps = readArray(psVar, timeIndex);
    Index psIndex = ps.getIndex();
    int nz = sigma.length;
    ArrayDouble.D1 result = new ArrayDouble.D1(nz);

    double psVal = ps.getDouble(psIndex.set(yIndex, xIndex));
    for (int z = 0; z < nz; z++) {
      result.set(z, ptop + sigma[z] * (psVal - ptop));
    }

    return result;
  }

}

