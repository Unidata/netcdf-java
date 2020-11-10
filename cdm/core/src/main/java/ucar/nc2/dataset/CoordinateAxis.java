/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.internal.dataset.conv.CF1Convention;
import ucar.nc2.time.Calendar;
import java.io.IOException;
import java.util.Formatter;

/**
 * A Coordinate Axis is a Variable that specifies one of the coordinates of a CoordinateSystem.
 * Mathematically it is a scalar function F from index space to S:
 * 
 * <pre>
 *  F:D -> S
 *  where D is a product set of dimensions (aka <i>index space</i>), and S is the set of reals (R) or Strings.
 * </pre>
 * <p/>
 * If its element type is char, it is considered a string-valued Coordinate Axis and rank is reduced by one,
 * since the outermost dimension is considered the string length: v(i, j, .., strlen).
 * If its element type is String, it is a string-valued Coordinate Axis.
 * Otherwise it is numeric-valued, and <i>isNumeric()</i> is true.
 * <p/>
 * A CoordinateAxis is optionally marked as georeferencing with an AxisType. It should have
 * a units string and optionally a description string.
 * <p/>
 * A Structure cannot be a CoordinateAxis, although members of Structures can.
 * TODO make Immutable in ver7
 */
public class CoordinateAxis extends VariableDS {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CoordinateAxis.class);

  /**
   * Create a coordinate axis from an existing VariableDS.Builder.
   *
   * @param vdsBuilder an existing Variable in dataset.
   * @return CoordinateAxis or one of its subclasses (CoordinateAxis1D, CoordinateAxis2D, or CoordinateAxis1DTime).
   */
  public static CoordinateAxis.Builder<?> fromVariableDS(VariableDS.Builder<?> vdsBuilder) {
    if ((vdsBuilder.getRank() == 0) || (vdsBuilder.getRank() == 1)
        || (vdsBuilder.getRank() == 2 && vdsBuilder.dataType == DataType.CHAR)) {
      return CoordinateAxis1D.builder().copyFrom(vdsBuilder);
    } else if (vdsBuilder.getRank() == 2) {
      return CoordinateAxis2D.builder().copyFrom(vdsBuilder);
    } else {
      return CoordinateAxis.builder().copyFrom(vdsBuilder);
    }
  }

  /** Get type of axis */
  @Nullable
  public AxisType getAxisType() {
    return axisType;
  }

  @Override
  public String getUnitsString() {
    String units = super.getUnitsString();
    return units == null ? "" : units;
  }

  /**
   * Does the axis have numeric values?
   *
   * @return true if the CoordAxis is numeric, false if its string valued ("nominal").
   */
  public boolean isNumeric() {
    return getDataType().isNumeric();
  }

  /**
   * If the edges are contiguous or disjoint
   * Caution: many datasets do not explicitly specify this info, this is often a guess; default is true.
   *
   * @return true if the edges are contiguous or false if disjoint. Assumed true unless set otherwise.
   * @deprecated use GridAxis1D.isContiguous()
   */
  @Deprecated
  public boolean isContiguous() {
    return isContiguous;
  }

  /**
   * An interval coordinate consists of two numbers, bound1 and bound2.
   * The coordinate value must lie between them, but otherwise is somewhat arbitrary.
   * If not interval, then it has one number, the coordinate value.
   * 
   * @return true if its an interval coordinate.
   * @deprecated use GridAxis1D.isInterval()
   */
  @Deprecated
  public boolean isInterval() {
    return false; // interval detection is done in subclasses
  }

  /** @deprecated use NetcdfDataset.isIndependentCoordinate(CoordinateAxis) */
  @Deprecated
  public boolean isIndependentCoordinate() {
    if (isCoordinateVariable())
      return true;
    return attributes.hasAttribute(_Coordinate.AliasForDimension);
  }

  /**
   * Get the direction of increasing values, used only for vertical Axes.
   *
   * @return POSITIVE_UP, POSITIVE_DOWN, or null if unknown.
   * @deprecated use GridCoordSys.getPositive()
   */
  @Deprecated
  public String getPositive() {
    return positive;
  }

  /**
   * The name of this coordinate axis' boundary variable
   *
   * @return the name of this coordinate axis' boundary variable, or null if none.
   * @deprecated do not use.
   */
  @Deprecated
  public String getBoundaryRef() {
    return boundaryRef;
  }

  ////////////////////////////////

  private MAMath.MinMax minmax; // remove

  // TODO make Immutable in ver7
  private void init() {
    try {
      Array data = read();
      minmax = MAMath.getMinMax(data);
    } catch (IOException ioe) {
      log.error("Error reading coordinate values ", ioe);
      throw new IllegalStateException(ioe);
    }
  }

  /**
   * The smallest coordinate value. Only call if isNumeric.
   *
   * @return the minimum coordinate value
   * @deprecated use GridAxis1D.getMinValue()
   */
  @Deprecated
  public double getMinValue() {
    if (minmax == null)
      init();
    return minmax.min;
  }

  /**
   * The largest coordinate value. Only call if isNumeric.
   *
   * @return the maximum coordinate value
   * @deprecated use GridAxis1D.getMaxValue()
   */
  @Deprecated
  public double getMaxValue() {
    if (minmax == null)
      init();
    return minmax.max;
  }

  //////////////////////

  /**
   * Get a string representation
   *
   * @param buf place info here
   */
  public void getInfo(Formatter buf) {
    buf.format("%-30s", getNameAndDimensions());
    buf.format("%-30s", getUnitsString());
    if (axisType != null) {
      buf.format("%-10s", axisType.toString());
    }
    buf.format("%s", getDescription());
  }

  /** Standard sort on Coordinate Axes */
  public static class AxisComparator implements java.util.Comparator<CoordinateAxis> {
    public int compare(CoordinateAxis c1, CoordinateAxis c2) {
      Preconditions.checkNotNull(c1);
      Preconditions.checkNotNull(c2);

      AxisType t1 = c1.getAxisType();
      AxisType t2 = c2.getAxisType();

      if ((t1 == null) && (t2 == null))
        return c1.getShortName().compareTo(c2.getShortName());
      if (t1 == null)
        return -1;
      if (t2 == null)
        return 1;

      return t1.axisOrder() - t2.axisOrder();
    }
  }

  /**
   * Instances which have same content are equal.
   */
  public boolean equals(Object oo) {
    if (this == oo)
      return true;
    if (!(oo instanceof CoordinateAxis))
      return false;
    if (!super.equals(oo))
      return false;
    CoordinateAxis o = (CoordinateAxis) oo;

    if (getAxisType() != null)
      if (getAxisType() != o.getAxisType())
        return false;

    if (getPositive() != null)
      return getPositive().equals(o.getPositive());

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    int result = super.hashCode();
    if (getAxisType() != null)
      result = 37 * result + getAxisType().hashCode();
    if (getPositive() != null)
      result = 37 * result + getPositive().hashCode();
    return result;
  }

  /////////////////////////////////////

  /** Figure out what calendar to use from the axis' attributes. */
  // needed by time coordinates
  public ucar.nc2.time.Calendar getCalendarFromAttribute() {
    return getCalendarFromAttribute(ncd, attributes);
  }

  /** Figure out what calendar to use from the given attributes. */
  public static ucar.nc2.time.Calendar getCalendarFromAttribute(@Nullable NetcdfDataset ncd,
      AttributeContainer attributes) {
    String cal = attributes.findAttributeString(CF.CALENDAR, null);
    if (cal == null) { // default for CF and COARDS
      Attribute convention = (ncd == null) ? null : ncd.getRootGroup().findAttribute(CDM.CONVENTIONS);
      if (convention != null && convention.isString()) {
        String hasName = convention.getStringValue();
        int version = CF1Convention.getVersion(hasName);
        if (version >= 0) {
          return Calendar.gregorian;
          // if (version < 7 ) return Calendar.gregorian;
          // if (version >= 7 ) return Calendar.proleptic_gregorian; //
        }
        if (hasName != null && hasName.equalsIgnoreCase("COARDS"))
          return Calendar.gregorian;
      }
    }
    return ucar.nc2.time.Calendar.get(cal);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  // TODO make these final and immutable in 6.
  protected final NetcdfDataset ncd; // remove
  protected final AxisType axisType;
  protected final String positive; // remove
  protected final String boundaryRef; // remove
  protected boolean isContiguous; // remove

  protected CoordinateAxis(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.ncd = (NetcdfDataset) this.ncfile;
    this.axisType = builder.axisType;
    this.positive = builder.positive;
    this.boundaryRef = builder.boundaryRef;
    this.isContiguous = builder.isContiguous;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setAxisType(this.axisType).setPositive(this.positive).setBoundary(this.boundaryRef)
        .setIsContiguous(this.isContiguous);
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  /** Get a Builder of CoordinateAxis */
  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  /** A Builder of CoordinateAxis. */
  public static abstract class Builder<T extends Builder<T>> extends VariableDS.Builder<T> {
    public AxisType axisType;
    protected String positive;
    protected String boundaryRef;
    protected boolean isContiguous = true;
    private boolean built;

    protected abstract T self();

    public T setAxisType(AxisType axisType) {
      this.axisType = axisType;
      return self();
    }

    public T setPositive(String positive) {
      this.positive = positive;
      return self();
    }

    public T setBoundary(String boundaryRef) {
      this.boundaryRef = boundaryRef;
      return self();
    }

    public T setIsContiguous(boolean isContiguous) {
      this.isContiguous = isContiguous;
      return self();
    }

    @Override
    public T copyFrom(VariableDS.Builder<?> vds) {
      super.copyFrom(vds);
      return self();
    }

    public CoordinateAxis build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new CoordinateAxis(this, parentGroup);
    }
  }

}
