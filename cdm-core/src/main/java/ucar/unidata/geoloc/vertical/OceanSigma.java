/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import java.io.IOException;

/**
 * Create a 3D height(z,y,x) array using the CF formula for "ocean_sigma_z_coordinate".
 * 
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#_ocean_sigma_over_z_coordinate"
 */

public class OceanSigma extends AbstractVerticalTransform {

  /** The eta variable name identifier */
  public static final String ETA = "Eta_variableName";

  /** The "s" variable name identifier */
  public static final String SIGMA = "Sigma_variableName";

  /** The "depth" variable name identifier */
  public static final String DEPTH = "Depth_variableName";

  private final Variable etaVar, sVar, depthVar;

  /**
   * Create a new vertical transform for Ocean S coordinates
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   */
  public static OceanSigma create(NetcdfFile ds, Dimension timeDim, AttributeContainer params) {

    Variable etaVar = findVariableFromParameterName(ds, params, ETA);
    Variable sVar = findVariableFromParameterName(ds, params, SIGMA);
    Variable depthVar = findVariableFromParameterName(ds, params, DEPTH);
    String units = depthVar.findAttributeString(CDM.UNITS, "none");

    return new OceanSigma(timeDim, units, etaVar, sVar, depthVar);
  }

  private OceanSigma(Dimension timeDim, String units, Variable etaVar, Variable sVar, Variable depthVar) {
    super(timeDim, units);
    this.etaVar = etaVar;
    this.sVar = sVar;
    this.depthVar = depthVar;
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   */
  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array eta = readArray(etaVar, timeIndex);
    Array sigma = readArray(sVar, timeIndex);
    Array depth = readArray(depthVar, timeIndex);

    int nz = (int) sigma.getSize();
    Index sIndex = sigma.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigma.getDouble(sIndex.set(z));
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double etaVal = eta.getDouble(etaIndex.set(y, x));
          double depthVal = depth.getDouble(depthIndex.set(y, x));

          height.set(z, y, x, etaVal + sigmaVal * (depthVal + etaVal));
        }
      }
    }

    return height;
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

    Array eta = readArray(etaVar, timeIndex);
    Array sigma = readArray(sVar, timeIndex);
    Array depth = readArray(depthVar, timeIndex);

    int nz = (int) sigma.getSize();
    Index sIndex = sigma.getIndex();

    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D1 height = new ArrayDouble.D1(nz);

    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigma.getDouble(sIndex.set(z));

      double etaVal = eta.getDouble(etaIndex.set(yIndex, xIndex));
      double depthVal = depth.getDouble(depthIndex.set(yIndex, xIndex));

      height.set(z, etaVal + sigmaVal * (depthVal + etaVal));
    }

    return height;
  }

}

