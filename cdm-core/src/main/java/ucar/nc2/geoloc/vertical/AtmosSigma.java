/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * Implement CF "atmosphere_sigma_coordinate".
 *
 * Atmosphere sigma coordinate
 * standard_name = "atmosphere_sigma_coordinate"
 * Definition
 * p(n,k,j,i) = ptop + sigma(k)*(ps(n,j,i)-ptop)
 * where p(n,k,j,i) is the pressure at gridpoint (n,k,j,i), ptop is the pressure at the top of the model,
 * sigma(k) is the dimensionless coordinate at vertical gridpoint (k), and ps(n,j,i) is the surface pressure at
 * horizontal gridpoint (j,i)and time (n).
 *
 * The format for the formula_terms attribute is
 * formula_terms = "sigma: var1 ps: var2 ptop: var3"
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.9/cf-conventions.html#_atmosphere_sigma_coordinate"
 */
@Immutable
public class AtmosSigma extends AbstractVerticalTransform {

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {
    String formula_terms = getFormula(params, errlog);
    if (null == formula_terms) {
      return Optional.empty();
    }

    List<String> varNames = parseFormula(formula_terms, "sigma ps ptop", errlog);
    if (varNames.size() != 3) {
      return Optional.empty();
    }

    String sigma = varNames.get(0);
    String ps = varNames.get(1);
    String ptop = varNames.get(2);
    String units = getUnits(ds, ps);

    int rank = getRank(ds, sigma);
    if (rank != 1) {
      errlog.format("AtmosSigma %s: sigma has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }

    int psRank = getRank(ds, ps);
    if (psRank < 2 || psRank > 3) {
      errlog.format("AtmosSigma %s: ps has rank %d should be 2 or 3%n", params.getName(), psRank);
      return Optional.empty();
    }

    rank = getRank(ds, ptop);
    if (rank > 1) {
      errlog.format("AtmosSigma %s: ptop has rank %d should be 0 or 1%n", params.getName(), rank);
      return Optional.empty();
    }

    try {
      return Optional.of(new AtmosSigma(ds, params.getName(), units, sigma, ps, ptop, psRank));
    } catch (IOException e) {
      errlog.format("OceanSigma %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String sigma;
  private final String ps;
  private final String ptopName;
  private final int psRank;
  private final Array<Number> sigmaData;
  private final double ptop;

  private AtmosSigma(NetcdfDataset ds, String ctvName, String units, String sigma, String ps, String ptop, int psRank)
      throws IOException {
    super(ds, CF.atmosphere_sigma_coordinate, ctvName, units);

    this.sigma = sigma;
    this.ps = ps;
    this.ptopName = ptop;
    this.psRank = psRank;

    this.ptop = readScalarDouble(ds, ptopName);
    this.sigmaData = readArray(ds, sigma);
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> psData = (psRank == 3) ? readArray(ds, ps, timeIndex) : readArray(ds, ps);
    Preconditions.checkArgument(psData.getRank() == 2);

    int ny = psData.getShape()[0];
    int nx = psData.getShape()[1];
    int nz = sigmaData.getShape()[0];

    double[] result = new double[nz * ny * nx];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigmaData.get(z).doubleValue();
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double psVal = psData.get(y, x).doubleValue();
          result[count++] = ptop + sigmaVal * (psVal - ptop);
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, result);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return AtmosSigma.create(ds, params, errlog);
    }
  }
}

