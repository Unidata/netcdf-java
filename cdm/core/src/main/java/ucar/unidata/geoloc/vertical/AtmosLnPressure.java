/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.unidata.geoloc.vertical;

import javax.annotation.concurrent.Immutable;
import ucar.nc2.Variable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Dimension;
import ucar.ma2.*;
import ucar.ma2.ArrayDouble.D1;
import ucar.unidata.util.Parameter;
import java.util.List;
import java.io.IOException;

/**
 * Implement CF vertical coordinate "atmosphere_ln_pressure_coordinate"
 * pressure(z) = p0 * exp(-lev(k))" .
 *
 * Theres a problem here, since its not 3D, we dont know what the 2D extent is.
 * DO NOT USE: see CF1Convention.makeAtmLnCoordinate()
 * 
 * @author caron
 * @see <a href="http://cf-pcmdi.llnl.gov/">http://cf-pcmdi.llnl.gov/</a>
 * @since May 6, 2008
 */
@Immutable
public class AtmosLnPressure extends AbstractVerticalTransform {
  public static final String P0 = "ReferencePressureVariableName";
  public static final String LEV = "VerticalCoordinateVariableName";

  private final Array pressure;

  /**
   * Create a new vertical transform for CF vertical coordinate "atmosphere_ln_pressure_coordinate"
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   *        TODO: params will change to AttributeContainer in ver7.
   */
  public static AtmosLnPressure create(NetcdfFile ds, Dimension timeDim, List<Parameter> params) {
    Variable p0var = findVariableFromParameterName(ds, params, P0);

    double p0;
    try {
      p0 = p0var.readScalarDouble();
    } catch (IOException e) {
      throw new IllegalArgumentException(
          "AtmosLnPressure failed to read " + p0var.getShortName() + " err= " + e.getMessage());
    }
    String units = p0var.getUnitsString();

    Variable levVar = findVariableFromParameterName(ds, params, LEV);

    Array pressure;
    try {
      Array lev = levVar.read();
      assert lev.getRank() == 1;
      pressure = new ArrayDouble.D1((int) lev.getSize());
      IndexIterator ii = pressure.getIndexIterator();
      while (lev.hasNext()) {
        double result = p0 * Math.exp(-lev.nextDouble());
        ii.setDoubleNext(result);
      }

    } catch (IOException e) {
      throw new IllegalArgumentException(
          "AtmosLnPressure failed to read " + levVar.getShortName() + " err= " + e.getMessage());
    }

    return new AtmosLnPressure(timeDim, units, pressure);
  }

  private AtmosLnPressure(Dimension timeDim, String units, Array pressure) {
    super(timeDim, units);
    this.pressure = pressure;
  }

  /**
   * Get the 3D vertical coordinate array for this time step.
   *
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @return vertical coordinate array
   */
  @Override
  public ArrayDouble.D3 getCoordinateArray(int timeIndex) {

    int nz = (int) pressure.getSize();
    int[] shape2D = pressure.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];

    ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);

    IndexIterator ii = pressure.getIndexIterator();
    for (int z = 0; z < nz; z++) {
      double p = ii.getDoubleNext();
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          result.set(z, y, x, p);
        }
      }
    }

    return result;
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
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) {
    throw new UnsupportedOperationException("1D subsetting is not implemented yet for this vertical tranformation");
  }

}


