/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Index;
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
 * Implement CF "ocean_s_coordinate_g2".
 *
 * <pre>
 * Ocean s-coordinate, generic form 2
 * standard_name = "ocean_s_coordinate_g2"
 * Definition
 *     z(n,k,j,i) = eta(n,j,i) + (eta(n,j,i) + depth(j,i)) * S(k,j,i)
 * where
 *   S(k,j,i) = (depth_c * s(k) + depth(j,i) * C(k)) / (depth_c + depth(j,i))
 *
 * where
 * z(n,k,j,i) is height, positive upwards, relative to ocean datum (e.g. mean sea level) at gridpoint (n,k,j,i),
 * eta(n,j,i) is the height of the ocean surface, positive upwards, relative to ocean datum at gridpoint (n,j,i),
 * s(k) is the dimensionless coordinate at vertical gridpoint (k) with a range of -1 ⇐ s(k) ⇐ 0 ,
 * S(0) corresponds to eta(n,j,i) whereas s(-1) corresponds to depth(j,i);
 * C(k) is the dimensionless vertical coordinate stretching function at gridpoint (k) with a range of -1 ⇐ C(k) ⇐ 0,
 * C(0) corresponds to eta(n,j,i) whereas C(-1) corresponds to depth(j,i);
 * the constant depth_c, (positive value), is a critical depth controlling the stretching and
 * depth(j,i) is the distance from ocean datum to sea floor (positive value) at horizontal gridpoint (j,i).
 *
 * The format for the formula_terms attribute is
 *
 *   formula_terms = "s: var1 C: var2 eta: var3 depth: var4 depth_c: var5"
 * </pre>
 *
 * @see "http://cfconventions.org/Data/cf-conventions/cf-conventions-1.9/cf-conventions.html#_ocean_s_coordinate_generic_form_1"
 */
@Immutable
public class OceanSG2 extends AbstractVerticalTransform {

  public static Optional<VerticalTransform> create(NetcdfDataset ds, AttributeContainer params, Formatter errlog) {
    String formula_terms = getFormula(params, errlog);
    if (null == formula_terms) {
      return Optional.empty();
    }

    List<String> varNames = parseFormula(formula_terms, "s C eta depth depth_c", errlog);
    if (varNames.size() != 5) {
      return Optional.empty();
    }

    String sName = varNames.get(0);
    String cName = varNames.get(1);
    String etaName = varNames.get(2);
    String depthName = varNames.get(3);
    String depthCName = varNames.get(4);
    String units = getUnits(ds, depthName);

    int rank = getRank(ds, sName);
    if (rank != 1) {
      errlog.format("OceanSG1 %s: s has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }
    rank = getRank(ds, cName);
    if (rank != 1) {
      errlog.format("OceanSG1 %s: c has rank %d should be 1%n", params.getName(), rank);
      return Optional.empty();
    }

    int etaRank = getRank(ds, etaName);
    if (etaRank != 2 && etaRank != 3) {
      errlog.format("OceanSG1 %s: eta has rank %d should be 2 or 3%n", params.getName(), etaRank);
      return Optional.empty();
    }

    rank = getRank(ds, depthName);
    if (rank != 2) {
      errlog.format("OceanSG1 %s: depth has rank %d should be 2%n", params.getName(), rank);
      return Optional.empty();
    }

    try {
      return Optional
          .of(new OceanSG2(ds, params.getName(), units, sName, cName, etaName, depthName, depthCName, etaRank));
    } catch (IOException e) {
      errlog.format("OceanSG1 %s: failed err = %s%n", params.getName(), e.getMessage());
      return Optional.empty();
    }
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String sName;
  private final String cName;
  private final String etaName;
  private final String depthName;
  private final String depthCName;
  private final int etaRank;
  private final double depth_c;
  private final Array<Number> sArray;
  private final Array<Number> cArray;

  private OceanSG2(NetcdfDataset ds, String ctvName, String units, String sName, String cName, String etaName,
      String depthName, String depthCName, int etaRank) throws IOException {
    super(ds, CF.ocean_s_coordinate_g2, ctvName, units);

    this.sName = sName;
    this.cName = cName;
    this.etaName = etaName;
    this.depthName = depthName;
    this.depthCName = depthCName;
    this.etaRank = etaRank;

    this.depth_c = readScalarDouble(ds, depthCName);
    this.sArray = readArray(ds, sName);
    this.cArray = readArray(ds, cName);
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> etaArray = (etaRank == 3) ? readArray(ds, etaName, timeIndex) : readArray(ds, etaName);
    Array<Number> depthArray = readArray(ds, depthName);

    return makeHeight(etaArray, sArray, depthArray, cArray, depth_c);
  }

  /**
   * height(x,y,z) = eta(x,y) + ( eta(x,y) + depth([n],x,y) ) * S(x,y,z)
   * where,
   * S(x,y,z) = (depth_c*s(z) + (depth([n],x,y) * C(z)) / (depth_c + depth([n],x,y))
   */
  private Array<Number> makeHeight(Array<Number> eta, Array<Number> s, Array<Number> depth, Array<Number> c,
      double depth_c) {
    int nz = (int) s.getSize();
    Index sIndex = s.getIndex();
    Index cIndex = c.getIndex();

    int[] shape2D = eta.getShape();
    int ny = shape2D[0];
    int nx = shape2D[1];
    Index etaIndex = eta.getIndex();
    Index depthIndex = depth.getIndex();

    double[] parray = new double[nz * ny * nx];
    int count = 0;
    for (int z = 0; z < nz; z++) {
      double sz = s.get(sIndex.set(z)).doubleValue();
      double cz = c.get(cIndex.set(z)).doubleValue();

      double term1 = depth_c * sz;

      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {

          double fac1 = depth.get(depthIndex.set(y, x)).doubleValue();
          double term2 = fac1 * cz;

          double Sterm = (term1 + term2) / (depth_c + fac1);

          double term3 = eta.get(etaIndex.set(y, x)).doubleValue();
          double term4 = (term3 + fac1) * Sterm;
          double hterm = term3 + term4;

          parray[count++] = hterm;
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, parray);
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return OceanSG2.create(ds, params, errlog);
    }
  }
}

