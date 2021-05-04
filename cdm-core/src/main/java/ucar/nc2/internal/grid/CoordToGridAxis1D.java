package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;

import java.util.Optional;

/** This extracts coordinate values from a scalar or 1-d numeric CoordinateAxis. */
class CoordToGridAxis1D {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordToGridAxis1D.class);

  final boolean isInterval; // is this an interval coordinate?
  final boolean boundsAreContiguous;
  final boolean boundsAreRegular;

  final double[] coords; // coordinate values
  final double[] edge; // contiguous only: n+1 edges, edge[k] < midpoint[k] < edge[k+1]
  final double[] bound1, bound2; // may be contiguous or not

  final int ncoords;
  final boolean isRegular;
  boolean isAscending;

  CoordToGridAxis1D(String name, Array<Double> values, Optional<Array<Double>> boundsOpt) {
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
      for (int i = 0; i < ncoords - 1; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(value1[i + 1], value2[i]))
          contig = false;
      }

      if (contig) {
        edge = new double[ncoords + 1];
        edge[0] = value1[0];
        System.arraycopy(value2, 0, edge, 1, ncoords);
        boundsAreContiguous = true;
        boundsAreRegular = calcIsRegular(edge);
      } else {
        boundsAreContiguous = false;
        boundsAreRegular = false;
        edge = null;
      }

      bound1 = value1;
      bound2 = value2;
      isInterval = true;
    } else {
      bound1 = null;
      edge = null;
      bound2 = null;
      isInterval = false;
      boundsAreContiguous = false;
      boundsAreRegular = false;
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

  void extract(GridAxis1D.Builder<?> builder) {
    builder.setNcoords(ncoords);
    if (!isInterval) {
      if (isRegular) {
        builder.setSpacing(GridAxis.Spacing.regularPoint);
        double starting = coords[0];
        double ending = coords[ncoords - 1];
        double increment = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
        builder.setRegular(ncoords, starting, ending, increment);

      } else {
        builder.setSpacing(GridAxis.Spacing.irregularPoint);
        builder.setValues(coords);
        double starting = coords[0];
        double ending = coords[ncoords - 1];
        double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords - 1);
        builder.setResolution(Math.abs(resolution));
      }

    } else {
      if (boundsAreRegular) {
        builder.setSpacing(GridAxis.Spacing.regularInterval);
        double starting = edge[0];
        double ending = edge[edge.length - 1];
        double increment = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords);
        builder.setRegular(ncoords, starting, ending, increment);

      } else if (boundsAreContiguous) {
        builder.setSpacing(GridAxis.Spacing.contiguousInterval);
        builder.setValues(edge);
        double starting = edge[0];
        double ending = edge[edge.length - 1];
        double resolution = (ncoords == 1) ? 0.0 : (ending - starting) / (ncoords);
        builder.setResolution(Math.abs(resolution));

      } else {
        builder.setSpacing(GridAxis.Spacing.discontiguousInterval);
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

}
