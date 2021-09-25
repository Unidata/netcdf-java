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
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.unidata.util.Parameter;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

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

  /**
   * Create a vertical transform for CF vertical coordinate "atmosphere_ln_pressure_coordinate"
   *
   * @param ds dataset
   * @param timeDim time dimension
   * @param params list of transformation Parameters
   */
  public static Optional<AtmosSigma> create(DataReader ds, Dimension timeDim, AttributeContainer params,
      Formatter errlog) {
    String psName = params.findAttributeString(PS, null);
    if (psName == null) {
      errlog.format("AtmosSigma: no parameter named %s%n", PS);
      return Optional.empty();
    }
    String ptopName = params.findAttributeString(PTOP, null);
    if (ptopName == null) {
      errlog.format("AtmosSigma: no parameter named %s%n", PTOP);
      return Optional.empty();
    }
    String sigmaName = params.findAttributeString(SIGMA, null);
    if (sigmaName == null) {
      errlog.format("AtmosSigma: no parameter named %s%n", SIGMA);
      return Optional.empty();
    }

    double p0;
    try {
      p0 = ds.read(psName).getScalar().doubleValue();
    } catch (Exception e) {
      errlog.format("AtmosLnPressure failed to read %s err= %s%n", psName, e.getMessage());
      return Optional.empty();
    }
    String units = ds.getUnits(psName);

    double ptop = readAndConvertUnit(ptopName, units);

    double[] sigma;
    try {
      Array<Number> sigmaData = ds.read(sigmaName);
      int n = (int) Arrays.computeSize(sigmaData.getShape());
      sigma = new double[n];
      int count = 0;
      for (Number levValue : sigmaData) {
        sigma[count++] = levValue.doubleValue();
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("AtmosSigma failed to read " + sigmaName + " err= " + e.getMessage());
    }

    return Optional.of(new AtmosSigma(timeDim, units, null, sigma, ptop));
  }

  /////////////////////////////////////////////////////////

  // surface pressure
  private final Variable psVar;
  // The sigma array, function of z
  private final double[] sigma;
  // Top of the model
  private final double ptop;

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
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException {
    return null;
    /*
     * Array ps = readArray(psVar, timeIndex);
     * Index psIndex = ps.getIndex();
     * 
     * int nz = sigma.length;
     * int[] shape2D = ps.getShape();
     * int ny = shape2D[0];
     * int nx = shape2D[1];
     * 
     * ArrayDouble.D3 result = new ArrayDouble.D3(nz, ny, nx);
     * 
     * for (int y = 0; y < ny; y++) {
     * for (int x = 0; x < nx; x++) {
     * double psVal = ps.getDouble(psIndex.set(y, x));
     * for (int z = 0; z < nz; z++) {
     * result.set(z, y, x, ptop + sigma[z] * (psVal - ptop));
     * }
     * }
     * }
     * 
     * return result;
     */
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
  public Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException {
    return null;

    /*
     * Array ps = readArray(psVar, timeIndex);
     * Index psIndex = ps.getIndex();
     * int nz = sigma.length;
     * D1 result = new D1(nz);
     * 
     * double psVal = ps.getDouble(psIndex.set(yIndex, xIndex));
     * for (int z = 0; z < nz; z++) {
     * result.set(z, ptop + sigma[z] * (psVal - ptop));
     * }
     * 
     * return result;
     */
  }

}

