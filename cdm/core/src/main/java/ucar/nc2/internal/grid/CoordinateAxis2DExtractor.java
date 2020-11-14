package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.array.Array;
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
public class CoordinateAxis2DExtractor {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis2DExtractor.class);

  private final Array<Double> coords;
  private final Array<Double> bounds;
  private final Array<Integer> hours;
  private final boolean isInterval;
  private boolean isContiguous; // TODO
  private final AttributeContainer attributes;
  private final String getRuntimeAxisName;

  CoordinateAxis2DExtractor(CoordinateAxis dtCoordAxis) {
    Preconditions.checkArgument(dtCoordAxis.getRank() == 2);
    this.attributes = dtCoordAxis.attributes();

    Attribute hourAtt = this.attributes.findAttribute(CDM.TIME_OFFSET_HOUR);
    Preconditions.checkNotNull(hourAtt);
    this.hours = (Array<Integer>) hourAtt.getArrayValues();

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

    coords = Arrays.toDouble(data);
    isInterval = computeIsInterval(dtCoordAxis);
    bounds = makeBoundsFromAux(dtCoordAxis);
  }

  public String getRuntimeAxisName() {
    return getRuntimeAxisName;
  }

  public boolean isInterval() {
    return isInterval;
  }

  public Array<Integer> getHourOffsets() {
    return hours;
  }

  public Array<Double> getMidpoints() {
    return coords;
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
}
