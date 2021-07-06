package ucar.nc2.internal.grid2;

import com.google.common.base.Preconditions;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.VariableDS;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.io.IOException;

/**
 * This extracts coordinate values from a 2-d CoordinateAxis.
 * Previously this was done in CoordinateAxis2D.
 */
@Immutable
public class Extract2DCoordinateValues {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Extract2DCoordinateValues.class);

  private final int ntimes;
  private final Array<Double> coords;
  private final Array<Double> bounds;
  private final Array<Integer> minutesFrom0z;
  private final boolean isInterval;
  private boolean isContiguous; // TODO
  private final AttributeContainer attributes;
  private final String getRuntimeAxisName;

  Extract2DCoordinateValues(CoordinateAxis dtCoordAxis) {
    Preconditions.checkArgument(dtCoordAxis.getRank() == 2);
    this.attributes = dtCoordAxis.attributes();

    Attribute minutesAtt = this.attributes.findAttribute(CDM.TIME_OFFSET_MINUTES);
    Preconditions.checkNotNull(minutesAtt);
    this.minutesFrom0z = (Array<Integer>) minutesAtt.getArrayValues();

    this.getRuntimeAxisName = this.attributes.findAttributeString(CDM.RUNTIME_COORDINATE, null);

    Array<?> data;
    try {
      data = dtCoordAxis.readArray();
    } catch (IOException ioe) {
      log.error("Error reading coordinate values " + ioe);
      throw new IllegalStateException(ioe);
    }

    if (data.getRank() != 2) {
      throw new IllegalArgumentException("must be 2D");
    }

    this.ntimes = data.getShape()[1];
    this.coords = Arrays.toDouble(data);
    this.isInterval = computeIsInterval(dtCoordAxis);
    Array<Double> tempBounds = makeBoundsFromAux(dtCoordAxis);
    if (tempBounds == null) {
      tempBounds = makeBoundsFromMidpoints(this.coords);
    }
    this.bounds = tempBounds;
  }

  public String getRuntimeAxisName() {
    return getRuntimeAxisName;
  }

  public boolean isInterval() {
    return isInterval;
  }

  public Array<Integer> getMinutesOffsets() {
    return minutesFrom0z;
  }

  public Array<Double> getMidpoints() {
    return coords;
  }

  public int getNtimes() {
    return ntimes;
  }

  /** Null if not isInterval(), otherwise 3D */
  @Nullable
  public Array<Double> getBounds() {
    return bounds;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // bounds calculations

  private boolean computeIsInterval(CoordinateAxis dtCoordAxis) {
    String boundsVarName = attributes.findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return false;
    }
    VariableDS boundsVar = (VariableDS) dtCoordAxis.getParentGroup().findVariableLocal(boundsVarName);
    if (null == boundsVar)
      return false;
    if (3 != boundsVar.getRank())
      return false;
    if (!dtCoordAxis.getDimension(0).equals(boundsVar.getDimension(0)))
      return false;
    if (!dtCoordAxis.getDimension(1).equals(boundsVar.getDimension(1)))
      return false;
    return 2 == boundsVar.getDimension(2).getLength();
  }

  @Nullable
  private Array<Double> makeBoundsFromAux(CoordinateAxis dtCoordAxis) {
    String boundsVarName = attributes.findAttributeString(CF.BOUNDS, null);
    if (boundsVarName == null) {
      return null;
    }
    VariableDS boundsVar = (VariableDS) dtCoordAxis.getParentGroup().findVariableLocal(boundsVarName);
    Preconditions.checkNotNull(boundsVar);

    Array<?> data;
    try {
      data = boundsVar.readArray();
    } catch (IOException e) {
      log.warn("CoordinateAxis2DExtractor.makeBoundsFromAux read failed ", e);
      return null;
    }
    return Arrays.toDouble(data);
  }

  /**
   * Calculate the bounds from the midpoints of a 2D coordinate.
   * For each row, uses the same algorithm as GridAxis1D.
   */
  private Array<Double> makeBoundsFromMidpoints(Array<Double> midpoints) {
    Preconditions.checkArgument(midpoints.getRank() == 2);
    int[] shape = midpoints.getShape();
    int nrows = shape[0];
    int ncols = shape[1];

    double[] bounds = new double[nrows * ncols * 2];
    int count = 0;
    for (int row = 0; row < nrows; row++) {
      for (int col = 0; col < ncols; col++) {
        if (col == 0) { // values[0] - (values[1] - values[0]) / 2
          bounds[count++] = midpoints.get(row, 0) - (midpoints.get(row, 1) - midpoints.get(row, 0)) / 2;
        } else {
          bounds[count++] = (midpoints.get(row, col) + midpoints.get(row, col - 1)) / 2;
        }
        if (col == ncols - 1) { // values[index] + (values[index] - values[index - 1]) / 2;
          bounds[count++] = midpoints.get(row, col) + (midpoints.get(row, col) - midpoints.get(row, col - 1)) / 2;
        } else {
          bounds[count++] = (midpoints.get(row, col) + midpoints.get(row, col + 1)) / 2;
        }
      }
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nrows, ncols, 2}, bounds);
  }
}
