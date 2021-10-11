/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/**
 * Implement CF "ocean_s_coordinate".
 *
 * <pre>
 * standard_name = "ocean_s_coordinate"
 * Definition
 * z(n,k,j,i) = eta(n,j,i)*(1+s(k)) + depth_c*s(k) + (depth(j,i)-depth_c)*C(k)
 * where
 * C(k) = (1-b)*sinh(a*s(k))/sinh(a) + b*[tanh(a*(s(k)+0.5))/(2*tanh(0.5*a)) - 0.5]
 * where
 * z(n,k,j,i) is height (positive upwards) relative to the datum (e.g. mean sea level) at gridpoint (n,k,j,i),
 * eta(n,j,i) is the height of the sea surface (positive upwards) relative to the datum at gridpoint (n,j,i), s(k) is
 * the
 * dimensionless coordinate at vertical gridpoint (k), and depth(j,i) is the distance (a positive value) from the datum
 * to the sea floor at horizontal gridpoint (j,i). The constants a, b, and depth_c control the stretching. The constants
 * a and b are dimensionless, and depth_c must have units of length.
 *
 * The format for the formula_terms attribute is
 * formula_terms = "s: var1 eta: var2 depth: var3 a: var4 b: var5 depth_c: var6"
 * </pre>
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.9/cf-conventions.html#_ocean_s_coordinate"
 */
public class OceanS extends AbstractVerticalTransform {

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {
    String formula_terms = getFormula(params, errlog);
    if (null == formula_terms) {
      return Optional.empty();
    }

    List<String> varNames = parseFormula(formula_terms, "s eta depth a b depth_c", errlog);
    if (varNames.size() != 6) {
      return Optional.empty();
    }

    String s = varNames.get(0);
    String eta = varNames.get(1);
    String depth = varNames.get(2);

    String a = varNames.get(3);
    String b = varNames.get(4);
    String depth_c = varNames.get(5);
    String units = getUnits(ds, depth);

    int rank = getRank(ds, s);
    if (rank != 1) {
      errlog.format("OceanS %s: s has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }
    rank = getRank(ds, eta);
    if (rank != 3) {
      errlog.format("OceanS %s: eta has rank %d should be 3%n", params.getName(), rank);
      return Optional.empty();
    }
    rank = getRank(ds, depth);
    if (rank != 2) {
      errlog.format("OceanS %s: depth has rank %d should be 2%n", params.getName(), rank);
      return Optional.empty();
    }

    try {
      return Optional.of(new OceanS(ds, params.getName(), units, eta, s, depth, a, b, depth_c));
    } catch (IOException e) {
      errlog.format("OceanSigma %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  private final String etaVar, sVar, depthVar;
  private final double a, b, depth_c;
  private final Array<Number> sArray;
  private final Array<Number> depthArray;

  private OceanS(NetcdfDataset ds, String ctvName, String units, String etaVar, String sVar, String depthVar,
      String aVar, String bVar, String depthCVar) throws IOException {
    super(ds, CF.ocean_s_coordinate, ctvName, units);

    this.etaVar = etaVar;
    this.sVar = sVar;
    this.depthVar = depthVar;

    this.sArray = readArray(ds, sVar);
    this.depthArray = readArray(ds, depthVar);
    this.a = readScalarDouble(ds, aVar);
    this.b = readScalarDouble(ds, bVar);
    this.depth_c = readScalarDouble(ds, depthCVar);
  }

  @Override
  public ucar.array.Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> etaArray = readArray(ds, etaVar, timeIndex);
    Array<Number> c = makeC(sArray);

    return makeHeight(etaArray, sArray, depthArray, c, depth_c);
  }

  @Override
  public ucar.array.Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException {
    Array<Number> etaArray = readArray(ds, etaVar, timeIndex);
    Array<Number> c = makeC(sArray);

    return makeHeight1D(etaArray, sArray, depthArray, c, depth_c, xIndex, yIndex);
  }

  // C(z) = (1-b)*sinh(a*s(z))/sinh(a) + b*(tanh(a*(s(z)+0.5))/(2*tanh(0.5*a))-0.5)
  private ucar.array.Array<Number> makeC(Array<Number> s) {
    if (a == 0) {
      return s; // per R. Signell, USGS
    }
    double fac1 = 1.0 - b;
    double denom1 = 1.0 / Math.sinh(a);
    double denom2 = 1.0 / (2.0 * Math.tanh(0.5 * a));

    int count = 0;
    int nz = (int) s.getSize();
    double[] c = new double[nz];
    for (Number val : s) {
      double sz = val.doubleValue();
      double term1 = fac1 * Math.sinh(a * sz) * denom1;
      double term2 = b * (Math.tanh(a * (sz + 0.5)) * denom2 - 0.5);
      c[count++] = term1 + term2;
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz}, c);
  }


  /**
   * Make height from the given data.
   * old equationn: height(x,y,z) = eta(x,y)*(1+s(z)) + depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
   * -sachin 03/23/09
   * The new corrected equation according to Hernan Arango (Rutgers)
   * height(x,y,z) = S(x,y,z) + eta(x,y) * (1 + S(x,y,z) / depth(x,y) )
   * where,
   * S(x,y,z) = depth_c*s(z) + (depth(x,y)-depth_c)*C(z)
   */
  private ucar.array.Array<Number> makeHeight(Array<Number> eta, Array<Number> s, Array<Number> depth, Array<Number> c,
      double depth_c) {
    int nz = (int) s.getSize();
    int ny = eta.getShape()[0];
    int nx = eta.getShape()[1];

    double[] height = new double[nz * ny * nx];
    int count = 0;
    for (int z = 0; z < nz; z++) {
      double sz = s.get(z).doubleValue();
      double cz = c.get(z).doubleValue();
      double term1 = depth_c * sz;

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          // -sachin 03/23/09 modifications according to corrected equation.
          double fac1 = depth.get(y, x).doubleValue();
          double term2 = (fac1 - depth_c) * cz;

          double Sterm = term1 + term2;
          double term3 = eta.get(y, x).doubleValue();
          double term4 = 1 + Sterm / fac1;

          height[count++] = Sterm + term3 * term4;
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, height);
  }

  // Modify method 'makeHeight' as new method for getting vertical coordinate array for single point.
  // - sachin
  private Array<Number> makeHeight1D(Array<Number> eta, Array<Number> s, Array<Number> depth, Array<Number> c,
      double depth_c, int x_index, int y_index) {
    int nz = (int) s.getSize();

    double[] height = new double[nz];
    int count = 0;
    for (int z = 0; z < nz; z++) {
      double sz = s.get(z).doubleValue();
      double cz = c.get(z).doubleValue();
      double term1 = depth_c * sz;

      // -sachin 03/06/09 modifications according to corrected equation.
      double fac1 = depth.get(y_index, x_index).doubleValue();
      double term2 = (fac1 - depth_c) * cz;

      double Sterm = term1 + term2;
      double term3 = eta.get(y_index, x_index).doubleValue();
      double term4 = 1 + Sterm / fac1;

      height[count++] = Sterm + term3 * term4;
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz}, height);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return OceanS.create(ds, params, errlog);
    }
  }
}

