/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.vertical;

import javax.annotation.concurrent.Immutable;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;
import java.io.IOException;
import java.util.List;

/**
 * Create a 3D height(z,y,x) array using the CF formula for "ocean s vertical coordinate g2".
 * standard name: ocean_s_coordinate_g2
 *
 * @author Sachin (skbhate@ngi.msstate.edu)
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#_ocean_s_coordinate_generic_form_2"
 */
@Immutable
public class OceanSG2 extends AbstractVerticalTransform {

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
   * Create a new vertical transform for Ocean_S_coordinate_g2
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   */
  public static OceanSG2 create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    Variable etaVar = findVariableFromParameterName(ds, params, ETA);
    Variable sVar = findVariableFromParameterName(ds, params, S);
    Variable depthVar = findVariableFromParameterName(ds, params, DEPTH);
    Variable depthCVar = findVariableFromParameterName(ds, params, DEPTH_C);
    Variable cVar = findVariableFromParameterName(ds, params, C);

    String units = depthVar.findAttributeString(CDM.UNITS, "none");

    return new OceanSG2(timeDim, units, etaVar, sVar, depthVar, depthCVar, cVar);
  }

  private OceanSG2(Dimension timeDim, String units, Variable etaVar, Variable sVar, Variable depthVar,
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
   * @throws java.io.IOException problem reading data
   * @throws ucar.ma2.InvalidRangeException _more_
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
   * @throws java.io.IOException problem reading data
   * @throws ucar.ma2.InvalidRangeException _more_
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
   * height(x,y,z) = eta(x,y) + ( eta(x,y) + depth([n],x,y) ) * S(x,y,z)
   *
   * where,
   * S(x,y,z) = (depth_c*s(z) + (depth([n],x,y) * C(z)) / (depth_c + depth([n],x,y))
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
          double term2 = fac1 * cz;

          double Sterm = (term1 + term2) / (depth_c + fac1);

          double term3 = eta.getDouble(etaIndex.set(y, x));
          double term4 = (term3 + fac1) * Sterm;
          double hterm = term3 + term4;

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
      double term2 = fac1 * cz;

      double Sterm = (term1 + term2) / (depth_c + fac1);

      double term3 = eta.getDouble(etaIndex.set(y_index, x_index));
      double term4 = (term3 + fac1) * Sterm;
      double hterm = term3 + term4;

      height.set(z, hterm);
    }

    return height;
  }
}
