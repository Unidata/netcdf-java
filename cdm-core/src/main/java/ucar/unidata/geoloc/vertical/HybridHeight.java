/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

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
 * "atmosphere_hybrid_height_coordinate".
 * <p>
 * <strong>height(x,y,z) = a(z) + b(z)*orog(x,y)</strong>
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.8/cf-conventions.html#atmosphere-hybrid-height-coordinate"
 */
@Immutable
public class HybridHeight extends AbstractVerticalTransform {

  /** Surface pressure name identifier */
  public static final String OROG = "Orography_variableName";

  /** The "a" variable name identifier */
  public static final String A = "A_variableName";

  /** The "b" variable name identifier */
  public static final String B = "B_variableName";

  private final Variable aVar, bVar, orogVar;

  /**
   * Construct a coordinate transform for hybrid height
   *
   * @param ds netCDF dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   *        TODO: params will change to AttributeContainer in ver7.
   */
  public static HybridHeight create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    Variable aVar = findVariableFromParameterName(ds, params, A);
    Variable bVar = findVariableFromParameterName(ds, params, B);
    Variable orogVar = findVariableFromParameterName(ds, params, OROG);
    String units = orogVar.findAttributeString(CDM.UNITS, "none");

    return new HybridHeight(timeDim, units, aVar, bVar, orogVar);
  }

  private HybridHeight(Dimension timeDim, String units, Variable aVar, Variable bVar, Variable orogVar) {
    super(timeDim, units);
    this.aVar = aVar;
    this.bVar = bVar;
    this.orogVar = orogVar;
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   * @throws InvalidRangeException not a valid time range
   */
  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) throws IOException, InvalidRangeException {
    Array orogArray = readArray(orogVar, timeIndex);
    Array aArray = aVar.read();
    Array bArray = bVar.read();

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    int[] shape2D = orogArray.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index orogIndex = orogArray.getIndex();

    ArrayDouble.D3 height = new ArrayDouble.D3(nz, ny, nx);

    for (int z = 0; z < nz; z++) {
      double az = aArray.getDouble(aIndex.set(z));
      double bz = bArray.getDouble(bIndex.set(z));

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double orog = orogArray.getDouble(orogIndex.set(y, x));
          height.set(z, y, x, az + bz * orog);
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
   * @throws InvalidRangeException not a valid time range
   */
  @Override
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {
    Array orogArray = readArray(orogVar, timeIndex);
    Array aArray = aVar.read();
    Array bArray = bVar.read();

    int nz = (int) aArray.getSize();
    Index aIndex = aArray.getIndex();
    Index bIndex = bArray.getIndex();

    Index orogIndex = orogArray.getIndex();
    ArrayDouble.D1 height = new ArrayDouble.D1(nz);

    for (int z = 0; z < nz; z++) {
      double az = aArray.getDouble(aIndex.set(z));
      double bz = bArray.getDouble(bIndex.set(z));

      double orog = orogArray.getDouble(orogIndex.set(yIndex, xIndex));
      height.set(z, az + bz * orog);
    }
    return height;
  }

}

