package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.nc2.grid2.GridAxisInterval;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridAxisSpacing;
import ucar.nc2.util.Misc;

import java.util.Optional;

/** This extracts coordinate values from a scalar or 1-d numeric CoordinateAxis. */
class ExtractCoordinateValues {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExtractCoordinateValues.class);

  boolean isInterval; // is this an interval coordinate?
  boolean isNominal; // is this an nominal point coordinate?
  final boolean boundsAreContiguous;
  final boolean boundsAreRegular;

  final double[] coords; // coordinate values
  final double[] edges; // contiguous only: n+1 edges, edge[k] < midpoint[k] < edge[k+1]
  final double[] bound1, bound2; // may be contiguous or not

  final int ncoords;
  final boolean isRegular;
  boolean isAscending;

  ExtractCoordinateValues(String name, Array<Double> values, Optional<Array<Double>> boundsOpt, boolean isHorizAxis) {
    Preconditions.checkArgument(values.getRank() < 2);

    int count = 0;
    this.ncoords = (int) values.length();
    coords = new double[ncoords];
    for (Double val : values) {
      coords[count++] = val;
    }
    this.isRegular = calcIsRegular(coords);

    if (boundsOpt.isPresent()) {
      Array<Double> bounds = boundsOpt.get(); // shape [ncoords, 2]

      Preconditions.checkArgument((bounds.length() == 2 * ncoords),
          "incorrect boundsVar length data for variable " + name);
      Preconditions.checkArgument((bounds.getRank() == 2) && (bounds.getShape()[1] == 2),
          "incorrect boundsVar shape data for variable " + name);

      double[] value1 = new double[ncoords];
      double[] value2 = new double[ncoords];
      for (int i = 0; i < ncoords; i++) {
        value1[i] = bounds.get(i, 0);
        value2[i] = bounds.get(i, 1);
      }

      // decide if they are contiguous
      boolean contig = true;
      boolean isAscending = ncoords >= 2 && value1[0] < value1[1];
      if (isAscending) {
        for (int i = 0; i < ncoords - 1; i++) {
          if (!ucar.nc2.util.Misc.nearlyEquals(value1[i + 1], value2[i])) {
            contig = false;
            break;
          }
        }
      } else {
        for (int i = 0; i < ncoords - 1; i++) {
          if (!ucar.nc2.util.Misc.nearlyEquals(value1[i], value2[i+1])) {
            contig = false;
            break;
          }
        }
      }

      if (contig) {
        edges = new double[ncoords + 1];
        edges[0] = value1[0];
        System.arraycopy(value2, 0, edges, 1, ncoords);
        boundsAreContiguous = true;
        boundsAreRegular = calcIsRegular(edges);
      } else {
        boundsAreContiguous = false;
        boundsAreRegular = false;
        edges = null;
      }

      bound1 = value1;
      bound2 = value2;
      isInterval = true;
    } else {
      bound1 = null;
      edges = null;
      bound2 = null;
      isInterval = false;
      boundsAreContiguous = false;
      boundsAreRegular = false;
    }

    // if its an interval, see if it can be a point
    if (isInterval && boundsAreContiguous && isPointWithBounds()) {
      isInterval = false;
    }

    // Netcdf always has a coordinate, it may also have a bounds. These are made into intervals, even when they are not.
    // How do we know if its an interval? We dont. But we do know that intervals cannot be horizontal coordinates.
    // So we will call it a nominal point coordinate
    if (isInterval && boundsAreContiguous && isHorizAxis) {
      isInterval = false;
      isNominal = true;
    }
  }

  private boolean calcIsRegular(double[] values) {
    int nvalues = values.length;
    boolean isRegular;

    if (nvalues <= 2) {
      isRegular = true;
      isAscending = true;
    } else {
      isAscending = values[0] < values[1]; // assume strictly monotonic
      double increment = (values[nvalues - 1] - values[0]) / (nvalues - 1);

      boolean isMonotonic = true;
      isRegular = true;
      for (int i = 1; i < nvalues; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(values[i] - values[i - 1], increment, 5.0e-3)) {
          isRegular = false;
        }
        if (isAscending) {
          if (values[i] <= values[i - 1]) {
            isMonotonic = false;
          }
        } else {
          if (values[i] >= values[i - 1]) {
            isMonotonic = false;
          }
        }
      }
    }
    return isRegular;
  }

  private boolean isPointWithBounds() {
    if (ncoords == 1) {
      return false;
    }
    boolean isPoint = true;
    double lowerEdge = coords[0] - (coords[1] - coords[0]) / 2;
    if (!Misc.nearlyEquals(lowerEdge, bound1[0])) {
      isPoint = false;
    }
    for (int i = 0; i < this.coords.length - 1; i++) {
      double upperBound = (this.coords[i] + this.coords[i + 1]) / 2;
      if (!Misc.nearlyEquals(upperBound, bound2[i])) {
        isPoint = false;
      }
    }
    double upperEdge = coords[ncoords - 1] + (coords[ncoords - 1] - coords[ncoords - 2]) / 2;
    if (!Misc.nearlyEquals(upperEdge, bound2[ncoords - 1])) {
      isPoint = false;
    }
    return isPoint;
  }

  // correct non-monotonic longitude coords
  void correctLongitudeWrap() {
    boolean monotonic = true;
    for (int i = 0; i < coords.length - 1; i++) {
      monotonic &= isAscending ? coords[i] < coords[i + 1] : coords[i] > coords[i + 1];
    }
    if (monotonic) {
      return;
    }

    boolean cross = false;
    if (isAscending) {
      for (int i = 0; i < coords.length; i++) {
        if (cross) {
          coords[i] += 360;
        }
        if (!cross && (i < coords.length - 1) && (coords[i] > coords[i + 1])) {
          cross = true;
        }
      }
    } else {
      for (int i = 0; i < coords.length; i++) {
        if (cross) {
          coords[i] -= 360;
        }
        if (!cross && (i < coords.length - 1) && (coords[i] < coords[i + 1])) {
          cross = true;
        }
      }
    }
  }

  void extractToPointBuilder(GridAxisPoint.Builder<?> builder) {
    builder.setNcoords(ncoords);
    if (isNominal) {
      builder.setSpacing(GridAxisSpacing.nominalPoint);
      builder.setValues(coords);
      double starting = coords[0];
      double ending = coords[ncoords - 1];
      double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
      builder.setResolution(Math.abs(resolution));
      builder.setEdges(edges);

    } else if (isRegular) {
      builder.setSpacing(GridAxisSpacing.regularPoint);
      double starting = coords[0];
      double ending = coords[ncoords - 1];
      double increment = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
      builder.setRegular(ncoords, starting, increment);

    } else {
      builder.setSpacing(GridAxisSpacing.irregularPoint);
      builder.setValues(coords);
      double starting = coords[0];
      double ending = coords[ncoords - 1];
      double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
      builder.setResolution(Math.abs(resolution));
    }
  }

  void extractToIntervalBuilder(GridAxisInterval.Builder<?> builder) {
    builder.setNcoords(ncoords);
    if (boundsAreRegular) {
      builder.setSpacing(GridAxisSpacing.regularInterval);
      double starting = edges[0];
      double ending = edges[edges.length - 1];
      double increment = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords);
      builder.setRegular(ncoords, starting, increment);

    } else if (boundsAreContiguous) {
      builder.setSpacing(GridAxisSpacing.contiguousInterval);
      builder.setValues(edges);
      double starting = edges[0];
      double ending = edges[edges.length - 1];
      double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords);
      builder.setResolution(Math.abs(resolution));

    } else {
      builder.setSpacing(GridAxisSpacing.discontiguousInterval);
      double[] bounds = new double[2 * ncoords];
      int count = 0;
      for (int i = 0; i < ncoords; i++) {
        bounds[count++] = bound1[i];
        bounds[count++] = bound2[i];
      }
      builder.setValues(bounds);
      double starting = bounds[0];
      double ending = bounds[2 * ncoords - 1];
      double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
      builder.setResolution(Math.abs(resolution));
    }
  }

}
