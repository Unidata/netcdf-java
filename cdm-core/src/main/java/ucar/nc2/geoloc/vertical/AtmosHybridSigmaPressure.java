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
 * Atmosphere hybrid sigma pressure coordinate
 * standard_name = "atmosphere_hybrid_sigma_pressure_coordinate"
 * Definition
 * p(n,k,j,i) = a(k)*p0 + b(k)*ps(n,j,i)
 * or
 * p(n,k,j,i) = ap(k) + b(k)*ps(n,j,i)
 *
 * where p(n,k,j,i) is the pressure at gridpoint (n,k,j,i), a(k) or ap(k) and b(k) are components of the hybrid
 * coordinate at level k, p0 is a reference pressure, and ps(n,j,i) is the surface pressure at horizontal
 * gridpoint (j,i) and time (n).
 *
 * The choice of whether a(k) or ap(k) is used depends on model formulation;
 * the former is a dimensionless fraction, the latter a pressure value.
 * In both formulations, b(k) is a dimensionless fraction.
 *
 * The format for the formula_terms attribute is
 * formula_terms = "a: var1 b: var2 ps: var3 p0: var4"
 * where a is replaced by ap if appropriate.
 *
 * The hybrid sigma-pressure coordinate for level k is defined as a(k)+b(k) or ap(k)/p0+b(k), as appropriate.
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.9/cf-conventions.html#_atmosphere_hybrid_sigma_pressure_coordinate"
 */
@Immutable
public class AtmosHybridSigmaPressure extends AbstractVerticalTransform {

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {
    String formula_terms = getFormula(params, errlog);
    if (null == formula_terms) {
      return Optional.empty();
    }

    // "a: var1 b: var2 ps: var3 p0: var4"
    List<String> aNames = parseFormula(formula_terms, "a b ps p0", errlog);
    // "ap: var1 b: var2 ps: var3"
    List<String> apNames = parseFormula(formula_terms, "ap b ps", errlog);

    boolean usingA;
    if (aNames.size() == 4) {
      usingA = true;
    } else if (apNames.size() == 3) {
      usingA = false;
    } else {
      return Optional.empty();
    }

    String apName = usingA ? aNames.get(0) : apNames.get(0);
    String bName = usingA ? aNames.get(1) : apNames.get(1);
    String psName = usingA ? aNames.get(2) : apNames.get(2);
    String p0Name = usingA ? aNames.get(3) : null;
    String units = getUnits(ds, psName);

    int rank = getRank(ds, apName);
    if (rank != 1) {
      errlog.format("AtmosHybridSigmaPressure %s: apName has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }
    rank = getRank(ds, bName);
    if (rank != 1) {
      errlog.format("AtmosHybridSigmaPressure %s: bName has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }

    int psRank = getRank(ds, psName);
    if (psRank < 2 || psRank > 3) {
      errlog.format("AtmosHybridSigmaPressure %s: ps has rank %d should be 2 or 3%n", params.getName(), psRank);
      return Optional.empty();
    }

    try {
      return Optional
          .of(new AtmosHybridSigmaPressure(ds, params.getName(), units, apName, bName, psName, p0Name, psRank));
    } catch (IOException e) {
      errlog.format("AtmosHybridSigmaPressure %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String apName;
  private final String bName;
  private final String p0Name;
  private final String psName;
  private final int psRank;
  private final Array<Number> apData;
  private final Array<Number> bData;
  private final double p0;

  AtmosHybridSigmaPressure(NetcdfDataset ds, String ctvName, String units, String apName, String bName, String psName,
      String p0Name, int psRank) throws IOException {
    super(ds, CF.atmosphere_hybrid_sigma_pressure_coordinate, ctvName, units);

    this.apName = apName;
    this.bName = bName;
    this.psName = psName;
    this.p0Name = p0Name;
    this.psRank = psRank;

    this.p0 = p0Name == null ? 1.0 : readScalarDouble(ds, p0Name);
    this.apData = readArray(ds, apName);
    this.bData = readArray(ds, bName);
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> psData = (psRank == 3) ? readArray(ds, psName, timeIndex) : readArray(ds, psName);
    Preconditions.checkArgument(psData.getRank() == 2);

    int ny = psData.getShape()[0];
    int nx = psData.getShape()[1];
    int nz = apData.getShape()[0];

    double[] result = new double[nz * ny * nx];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      double apVal = this.p0 * apData.get(z).doubleValue();
      double bVal = bData.get(z).doubleValue();
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double psVal = psData.get(y, x).doubleValue();
          result[count++] = apVal + bVal * psVal;
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, result);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return AtmosHybridSigmaPressure.create(ds, params, errlog);
    }
  }
}

