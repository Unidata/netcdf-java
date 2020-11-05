package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.ArrayChar;
import ucar.array.ArrayString;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableDS;

import java.io.IOException;

/**
 * This extracts coordinate values from a scalar or 1-d CoordinateAxis.
 * Previously this was done in CoordinateAxis1D.
 */
public class CoordinateAxis1DExtractor {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis1DExtractor.class);

  boolean isInterval; // is this an interval coordinate?
  boolean isAscending;

  // read in on doRead()
  double[] coords; // coordinate values, must be between edges
  String[] names; // only set if String or char values

  // defer making until asked, use makeBounds()
  double[] edge; // n+1 edges, edge[k] < midpoint[k] < edge[k+1]
  double[] bound1, bound2; // may be contiguous or not
  boolean boundsAreContiguous;

  int ncoords;
  boolean isRegular;
  double start, increment;

  private final CoordinateAxis dtCoordAxis;

  CoordinateAxis1DExtractor(CoordinateAxis dtCoordAxis) {
    Preconditions.checkArgument(dtCoordAxis.getRank() < 2);
    this.dtCoordAxis = dtCoordAxis;
    doRead();
  }

  private void doRead() {
    if (dtCoordAxis.isNumeric()) {
      readValues();
      makeBounds();
      calcIsRegular();

    } else if (dtCoordAxis.getDataType() == DataType.STRING) {
      readStringValues();

    } else if (dtCoordAxis.getDataType() == DataType.CHAR) {
      readCharValues();
    }
  }

  // only used if String
  private void readStringValues() {
    int count = 0;
    ArrayString data;
    try {
      data = (ArrayString) dtCoordAxis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading string coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    this.ncoords = (int) data.length();
    names = new String[ncoords];
    for (String s : data) {
      names[count++] = s;
    }
  }

  private void readCharValues() {
    int count = 0;
    ArrayChar data;
    try {
      data = (ArrayChar) dtCoordAxis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading char coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    Array<String> strings = data.makeStringsFromChar();
    this.ncoords = (int) strings.length();
    names = new String[ncoords];
    for (String s : strings) {
      names[count++] = s;
    }
  }

  private void readValues() {
    Array<?> data;
    try {
      data = dtCoordAxis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }

    int count = 0;
    this.ncoords = (int) data.length();
    coords = new double[ncoords];
    for (Object val : data) {
      Number n = (Number) val;
      coords[count++] = n.doubleValue();
    }
  }

  /**
   * Calculate bounds, set isInterval, isContiguous
   */
  private void makeBounds() {
    if (dtCoordAxis.isNumeric()) {
      if (!makeBoundsFromAux()) {
        makeEdges();
      }
    }
  }

  private boolean makeBoundsFromAux() {
    String boundsVarName = dtCoordAxis.attributes().findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return false;
    }
    VariableDS boundsVar = (VariableDS) dtCoordAxis.getParentGroup().findVariableLocal(boundsVarName);
    if (null == boundsVar)
      return false;
    if (2 != boundsVar.getRank())
      return false;

    if (dtCoordAxis.getDimension(0) != boundsVar.getDimension(0))
      return false;
    if (2 != boundsVar.getDimension(1).getLength())
      return false;

    Array<?> boundsData;
    try {
      boundsData = boundsVar.readArray();
    } catch (IOException e) {
      log.warn("GridAxis1DBuilder.makeBoundsFromAux read failed ", e);
      return false;
    }

    Preconditions.checkArgument((boundsData.length() == 2 * ncoords),
        "incorrect boundsVar length data for variable " + boundsVar);

    Preconditions.checkArgument((boundsData.getRank() == 2) && (boundsData.getShape()[1] == 2),
        "incorrect boundsVar shape data for variable " + boundsVar);

    double[] value1 = new double[ncoords];
    double[] value2 = new double[ncoords];
    for (int i = 0; i < ncoords; i++) {
      value1[i] = ((Number) boundsData.get(i, 0)).doubleValue();
      value2[i] = ((Number) boundsData.get(i, 1)).doubleValue();
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
      System.arraycopy(value2, 0, edge, 1, ncoords + 1 - 1);
      boundsAreContiguous = true;
    } else { // what does edge mean when not contiguous ??
      edge = new double[ncoords + 1];
      edge[0] = value1[0];
      for (int i = 1; i < ncoords; i++) {
        edge[i] = (value1[i] + value2[i - 1]) / 2;
      }
      edge[ncoords] = value2[ncoords - 1];
      boundsAreContiguous = false;
    }

    bound1 = value1;
    bound2 = value2;
    isInterval = true;

    return true;
  }

  private void makeEdges() {
    int size = ncoords;
    edge = new double[size + 1];
    if (size < 1) {
      return;
    }
    for (int i = 1; i < size; i++) {
      edge[i] = (coords[i - 1] + coords[i]) / 2;
    }
    edge[0] = coords[0] - (edge[1] - coords[0]);
    edge[size] = coords[size - 1] + (coords[size - 1] - edge[size - 1]);
    boundsAreContiguous = true;
  }

  private void makeBoundsFromEdges() {
    int size = ncoords;
    if (size == 0)
      return;

    bound1 = new double[size];
    bound2 = new double[size];
    for (int i = 0; i < size; i++) {
      bound1[i] = edge[i];
      bound2[i] = edge[i + 1];
    }

    // flip if needed
    if (bound1[0] > bound2[0]) {
      double[] temp = bound1;
      bound1 = bound2;
      bound2 = temp;
    }
  }

  private void calcIsRegular() {
    if (ncoords < 2) {
      isAscending = true;
    } else {
      isAscending = coords[0] < coords[1]; // strictly monotonic
    }

    if (!dtCoordAxis.isNumeric()) {
      isRegular = false;
    } else if (ncoords < 2) {
      isRegular = true;
      isAscending = true;
    } else {
      isAscending = coords[0] < coords[1]; // strictly monotonic
      start = coords[0];
      int n = ncoords;
      increment = (coords[n - 1] - coords[0]) / (n - 1);

      isRegular = true;
      for (int i = 1; i < ncoords; i++) {
        if (!ucar.nc2.util.Misc.nearlyEquals(coords[i] - coords[i - 1], increment, 5.0e-3)) {
          isRegular = false;
          break;
        }
        // LOOK we could internally reorder.
        if (isAscending) {
          if (coords[i] <= coords[i - 1]) {
            throw new RuntimeException(dtCoordAxis.getShortName() + " is not monotonic increasing ");
          }
        } else {
          if (coords[i] >= coords[i - 1]) {
            throw new RuntimeException(dtCoordAxis.getShortName() + " is not monotonic decreasing");
          }
        }
      }
    }
  }

  /** If longitude coordinate is not monotonic, construct a new one that is. */
  void correctLongitudeWrap() {
    // correct non-monotonic longitude coords
    if (dtCoordAxis.getAxisType() != AxisType.Lon) {
      return;
    }

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
}
