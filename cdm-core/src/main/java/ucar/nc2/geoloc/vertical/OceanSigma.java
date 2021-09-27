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
 * Implement CF "ocean_sigma_coordinate".
 * :standard_name = "ocean_sigma_coordinate";
 * :formula_terms = "sigma: sigma eta: zeta depth: depth"
 * :height_formula = "height(x,y,z) = eta(n,j,i) + sigma(k)*(depth(j,i)+eta(n,j,i))"
 *
 * Definition
 * z(n,k,j,i) = eta(n,j,i) + sigma(k)*(depth(j,i)+eta(n,j,i))
 *
 * where z(n,k,j,i) is height (positive upwards) relative to the datum (e.g. mean sea level) at gridpoint (n,k,j,i),
 * eta(n,j,i) is the height of the sea surface (positive upwards) relative to the datum at gridpoint (n,j,i),
 * sigma(k) is the dimensionless coordinate at vertical gridpoint (k), and depth(j,i) is the distance (a positive value)
 * from the datum to the sea floor at horizontal gridpoint (j,i).
 *
 * formula_terms = "sigma: var1 eta: var2 depth: var3"
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.9/cf-conventions.html#_ocean_sigma_coordinate"
 */
@Immutable
public class OceanSigma extends AbstractVerticalTransform {

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {
    String formula_terms = getFormula(params, errlog);
    if (null == formula_terms) {
      return Optional.empty();
    }

    List<String> varNames = parseFormula(formula_terms, "sigma eta depth", errlog);
    if (varNames.size() != 3) {
      return Optional.empty();
    }

    String sigmaVar = varNames.get(0);
    String etaVar = varNames.get(1);
    String depthVar = varNames.get(2);
    String units = getUnits(ds, depthVar);

    int rank = getRank(ds, sigmaVar);
    if (rank != 1) {
      errlog.format("OceanSigma %s: sVar has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }

    rank = getRank(ds, etaVar);
    if (rank != 3) {
      errlog.format("OceanSigma %s: etaVar has rank %d should be 3%n", params.getName(), rank);
      return Optional.empty();
    }

    rank = getRank(ds, depthVar);
    if (rank != 2) {
      errlog.format("OceanSigma %s: depthVar has rank %d should be 2%n", params.getName(), rank);
      return Optional.empty();
    }

    try {
      return Optional.of(new OceanSigma(ds, params.getName(), units, sigmaVar, etaVar, depthVar));
    } catch (IOException e) {
      errlog.format("OceanSigma %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String sigmaVar;
  private final String etaVar;
  private final String depthVar;
  private final Array<Number> sigmaData;

  private OceanSigma(NetcdfDataset ds, String ctvName, String units, String sigmaVar, String etaVar, String depthVar)
      throws IOException {
    super(ds, CF.ocean_sigma_coordinate, ctvName, units);

    this.sigmaVar = sigmaVar;
    this.etaVar = etaVar;
    this.depthVar = depthVar;

    this.sigmaData = readArray(ds, sigmaVar);
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> etaData = readArray(ds, etaVar, timeIndex);
    Array<Number> depthData = readArray(ds, depthVar);

    Preconditions.checkArgument(depthData.getRank() == 2);
    Preconditions.checkArgument(etaData.getRank() == 2);

    int ny = depthData.getShape()[0];
    int nx = depthData.getShape()[1];
    int nz = sigmaData.getShape()[0];

    Preconditions.checkArgument(etaData.getShape()[0] == ny);
    Preconditions.checkArgument(etaData.getShape()[1] == nx);

    double[] result = new double[nz * ny * nx];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      double sigmaVal = sigmaData.get(z).doubleValue();
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double etaVal = etaData.get(y, x).doubleValue();
          double depthVal = depthData.get(y, x).doubleValue();
          result[count++] = etaVal + sigmaVal * (depthVal + etaVal);
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, result);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return OceanSigma.create(ds, params, errlog);
    }
  }
}

