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
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.Optional;

/**
 * Implement Weather Research and Forecast (WRF) model's vertical Eta coordinate.
 * Because the transform depends on the CoordinateSystem, must handle differently.
 *
 * <pre>
 * height(x,y,z) = (PH(x,y,z) + PHB(x,y,z)) / 9.81").
 * pressure(x,y,z) = P(x,y,z) + PB(x,y,z)
 *
 * Geopotential is provided on the staggered z grid so transform to height levels.
 * Pressure is provided on the non-staggered z grid so use pressure levels.
 * </pre>
 */
@Immutable
public class WrfEta extends AbstractVerticalTransform {
  public static final String WRF_ETA_COORDINATE = "wrf_eta_coordinate";
  private static final String BasePressureVariable = "PB";
  private static final String PerturbationPressureVariable = "P";
  private static final String BaseGeopotentialVariable = "PHB";
  private static final String PerturbationGeopotentialVariable = "PH";

  public static Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
      Formatter errlog) {

    boolean isXStag = isStaggered(csys, AxisType.GeoX, AxisType.Lon, 1);
    boolean isYStag = isStaggered(csys, AxisType.GeoY, AxisType.Lat, 0);
    boolean isZStag = isStaggered(csys, AxisType.GeoZ, null, 0);

    String units;
    String pertVar;
    String baseVar;

    if (isZStag) {
      // Geopotential is provided on the staggered z grid
      // so we'll transform like grids to height levels
      pertVar = PerturbationGeopotentialVariable;
      baseVar = BaseGeopotentialVariable;
      units = "m"; // PH and PHB are in m^2/s^2, so dividing by g=9.81 m/s^2 results in meters
    } else {
      // Pressure is provided on the non-staggered z grid
      // so we'll transform like grids to pressure levels
      pertVar = PerturbationPressureVariable;
      baseVar = BasePressureVariable;
      units = "Pa"; // P and PB are in Pascals //ADD:safe assumption? grab unit attribute?
    }

    int rank = getRank(ds, pertVar);
    if (rank != 4) {
      errlog.format("WrfEta %s: %s has rank %d should be 4%n", params.getName(), pertVar, rank);
      return Optional.empty();
    }
    rank = getRank(ds, baseVar);
    if (rank != 4) {
      errlog.format("WrfEta %s: %s has rank %d should be 4%n", params.getName(), baseVar, rank);
      return Optional.empty();
    }

    return Optional.of(new WrfEta(ds, params.getName(), units, pertVar, baseVar, isXStag, isYStag, isZStag));
  }

  private static boolean isStaggered(CoordinateSystem coords, AxisType type1, @Nullable AxisType type2, int dimIndex) {
    CoordinateAxis axis1 = coords.findAxis(type1);
    if (axis1 != null) {
      return axis1.getShortName().endsWith("stag");
    }
    if (type2 == null) {
      return false;
    }
    CoordinateAxis axis2 = coords.findAxis(type2);
    if (axis2 != null) {
      String dimName = axis2.getDimension(dimIndex).getShortName();
      return dimName.endsWith("stag");
    }
    return false;
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////
  private final String pertVar;
  private final String baseVar;
  private final boolean isXStag, isYStag, isZStag;

  private WrfEta(NetcdfDataset ds, String ctvName, String units, String pertVar, String baseVar, boolean isXStag,
      boolean isYStag, boolean isZStag) {
    super(ds, WRF_ETA_COORDINATE, ctvName, units);

    this.pertVar = pertVar;
    this.baseVar = baseVar;
    this.isXStag = isXStag;
    this.isYStag = isYStag;
    this.isZStag = isZStag;
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) throws IOException, InvalidRangeException {
    Array<Number> pertData = readArray(ds, pertVar, timeIndex);
    Array<Number> baseData = readArray(ds, baseVar, timeIndex);

    Preconditions.checkArgument(pertData.getRank() == 3);
    Preconditions.checkArgument(baseData.getRank() == 3);

    int nz = baseData.getShape()[0];
    int ny = baseData.getShape()[1];
    int nx = baseData.getShape()[2];

    Preconditions.checkArgument(pertData.getShape()[0] == nz);
    Preconditions.checkArgument(pertData.getShape()[1] == ny);
    Preconditions.checkArgument(pertData.getShape()[2] == nx);

    double[] result = new double[nz * ny * nx];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      for (int y = 0; y < ny; y++) {
        for (int x = 0; x < nx; x++) {
          double pert = pertData.get(z, y, x).doubleValue();
          double base = baseData.get(z, y, x).doubleValue();
          double d = pert + base;
          if (isZStag) {
            d = d / 9.81; // convert geopotential to height
          }
          result[count++] = d;
        }
      }
    }

    if (isYStag) {
      result = addStaggerY(result, baseData.getShape());
      ny++;
    } else if (isXStag) {
      result = addStaggerX(result, baseData.getShape());
      nx++;
    }
    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz, ny, nx}, result);
  }

  /**
   * Add 1 to the size of the array for the X dimension.
   * Use linear average and interpolation to fill in the values.
   */
  private double[] addStaggerX(double[] array, int[] shape) {
    int nz = shape[0];
    int ny = shape[1];
    int nx = shape[2];
    int nnx = nx + 1;

    double[] result = new double[nz * ny * nnx];
    double[] extract = new double[nx]; // temporary

    // loop through the z,y dimensions and "extrapinterpolate" x
    for (int i = 0; i < nz; i++) {
      for (int j = 0; j < ny; j++) {
        for (int k = 0; k < nx; k++) {
          extract[k] = array[i * nx * ny + j * nx + k];
        }
        double[] extra = extrapinterpolate(extract); // compute new values
        for (int k = 0; k < nnx; k++) {
          result[i * nnx * ny + j * nnx + k] = extra[k];
        }
      }
    }
    return result;
  }

  /**
   * Add 1 to the size of the array for the Y dimension.
   * Use linear average and interpolation to fill in the values.
   */
  private double[] addStaggerY(double[] array, int[] shape) {
    int nz = shape[0];
    int ny = shape[1];
    int nx = shape[2];
    int nny = ny + 1;

    double[] result = new double[nz * nny * nx];
    double[] extract = new double[ny]; // temporary

    // loop through the z,xdimensions and "extrapinterpolate" y
    for (int i = 0; i < nz; i++) {
      for (int k = 0; k < nx; k++) {
        for (int j = 0; j < ny; j++) {
          extract[j] = array[i * nx * ny + j * nx + k];
        }
        double[] extra = extrapinterpolate(extract); // compute new values
        for (int j = 0; j < nny; j++) {
          result[i * nx * nny + j * nx + k] = extra[j];
        }
      }
    }
    return result;
  }

  /** Add one element to the array by linear interpolation and extrapolation at the ends. */
  private double[] extrapinterpolate(double[] array) {
    int n = array.length;
    double[] d = new double[n + 1];

    // end points from linear extrapolation
    // equations confirmed by Christopher Lindholm
    d[0] = 1.5 * array[0] - 0.5 * array[1];
    d[n] = 1.5 * array[n - 1] - 0.5 * array[n - 2];

    // inner points from simple average
    for (int i = 1; i < n; i++) {
      d[i] = 0.5 * (array[i - 1] + array[i]);
    }

    return d;
  }

  ///////////////////////////////////////////////////////////////////////////////////////////////

  public static class Builder implements VerticalTransform.Builder {
    public Optional<VerticalTransform> create(NetcdfDataset ds, CoordinateSystem csys, AttributeContainer params,
        Formatter errlog) {
      return WrfEta.create(ds, csys, params, errlog);
    }
  }
}

