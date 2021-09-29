/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import java.io.IOException;

/**
 * Create a 3D height(z,y,x) array using the CF formula for "ocean s vertical coordinate g1".
 * standard name: ocean_s_coordinate_g1
 *
 * @deprecated use ucar.nc2.geoloc.vertical
 */
@Deprecated
@Immutable
public class OceanSG1 extends AbstractVerticalTransform {

  /** The eta variable name identifier */
  public static final String ETA = "Eta_variableName";

  /** The "s" variable name identifier */
  public static final String S = "S_variableName";

  /** The "depth" variable name identifier */
  public static final String DEPTH = "Depth_variableName";

  /** The "depth c" variable name identifier */
  public static final String DEPTH_C = "Depth_c_variableName";

  /** The "C" variable name identifier */
  public static final String C = "c_variableName";

  private final double depth_c;
  private final Variable etaVar, sVar, depthVar, cVar;

  /**
   * Create a new vertical transform for Ocean_S_coordinate_g1
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   */
  public static OceanSG1 create(NetcdfFile ds, Dimension timeDim, AttributeContainer params) {
    Variable etaVar = findVariableFromParameterName(ds, params, ETA);
    Variable sVar = findVariableFromParameterName(ds, params, S);
    Variable depthVar = findVariableFromParameterName(ds, params, DEPTH);
    Variable depthCVar = findVariableFromParameterName(ds, params, DEPTH_C);
    Variable cVar = findVariableFromParameterName(ds, params, C);

    String units = depthVar.findAttributeString(CDM.UNITS, "none");

    return new OceanSG1(timeDim, units, etaVar, sVar, depthVar, depthCVar, cVar);
  }

  private OceanSG1(Dimension timeDim, String units, Variable etaVar, Variable sVar, Variable depthVar,
      Variable depthCVar, Variable cVar) {
    super(timeDim, units);
    this.etaVar = etaVar;
    this.sVar = sVar;
    this.depthVar = depthVar;
    this.cVar = cVar;

    try {
      this.depth_c = depthCVar.readScalarDouble();
    } catch (IOException ioe) {
      throw new IllegalStateException(ioe);
    }
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   */
  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array etaArray = readArray(etaVar, timeIndex);
    Array sArray = readArray(sVar, timeIndex);
    Array depthArray = readArray(depthVar, timeIndex);
    Array cArray = readArray(cVar, timeIndex);

    return makeHeight(etaArray, sArray, depthArray, cArray, depth_c);
  }

  /**
   * Get the 1D vertical coordinate array for this time step and
   * the specified X,Y index for Lat-Lon point.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  @Override
  public ArrayDouble.D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException {
    Array etaArray = readArray(etaVar, timeIndex);
    Array sArray = readArray(sVar, timeIndex);
    Array depthArray = readArray(depthVar, timeIndex);
    Array cArray = readArray(cVar, timeIndex);

    return makeHeight1D(etaArray, sArray, depthArray, cArray, depth_c, xIndex, yIndex);
  }

  /**
   * Make height from the given data. <br>
   *
   * height(x,y,z) = S(x,y,z) + eta(x,y) * (1 + S(x,y,z) / depth([n],x,y) )
   *
   * where,
   * S(x,y,z) = depth_c*s(z) + (depth([n],x,y)-depth_c)*C(z)
   *
   * @param eta eta Array
   * @param s s Array
   * @param depth depth Array
   * @param c c Array
   * @param depth_c value of depth_c
   * @return height data
   */
  private ArrayDouble.D3 makeHeight(Array eta, Array s, Array depth, Array c, double depth_c) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double sz = s.getDouble(sIndex.set(z));
      double cz = c.getDouble(cIndex.set(z));

      double term1 = depth_c * sz;

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double fac1 = depth.getDouble(depthIndex.set(y, x));
          double term2 = (fac1 - depth_c) * cz;

          double Sterm = term1 + term2;

          double term3 = eta.getDouble(etaIndex.set(y, x));
          double term4 = 1 + Sterm / fac1;
          double hterm = Sterm + term3 * term4;

          height.set(z, y, x, hterm);
        }
      }
    }

    return height;
  }

  private ArrayDouble.D1 makeHeight1D(Array eta, Array s, Array depth, Array c, double depth_c, int x_index,
      int y_index) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();


    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    ArrayDouble.D1 height = new ArrayDouble.D1(nz);

    for (int z = 0; z < nz; z++) {
      double sz = s.getDouble(sIndex.set(z));
      double cz = c.getDouble(cIndex.set(z));

      double term1 = depth_c * sz;

      double fac1 = depth.getDouble(depthIndex.set(y_index, x_index));
      double term2 = (fac1 - depth_c) * cz;

      double Sterm = term1 + term2;

      double term3 = eta.getDouble(etaIndex.set(y_index, x_index));
      double term4 = 1 + Sterm / fac1;
      double hterm = Sterm + term3 * term4;

      height.set(z, hterm);
    }

    return height;
  }
}
