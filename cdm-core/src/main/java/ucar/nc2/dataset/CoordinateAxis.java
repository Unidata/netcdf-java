/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import com.google.common.base.Preconditions;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import ucar.array.ArrayType;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.internal.dataset.conv.CF1Convention;
import ucar.nc2.calendar.Calendar;

/**
 * A Variable that specifies one of the coordinates of a CoordinateSystem,
 * this is a legacy class, use GridAxis for new code.
 * <p/>
 * 
 * <pre>
 * Mathematically it is a scalar function F from index space to S:
 *  F:D -&gt; S
 *  where D is a product set of dimensions (aka <i>index space</i>), and S is the set of reals (R) or Strings.
 * </pre>
 * <p/>
 * If its element type is CHAR, it is considered a string-valued Coordinate Axis and rank is reduced by one,
 * since the outermost dimension is considered the string length: v(i, j, .., strlen).
 * If its element type is String, it is a string-valued Coordinate Axis.
 * Otherwise it is numeric-valued, and <i>isNumeric()</i> is true.
 * <p/>
 * A CoordinateAxis is optionally marked as georeferencing with an AxisType. It should have
 * a units string and optionally a description string.
 * <p/>
 * A Structure cannot be a CoordinateAxis, although members of Structures can.
 */
@Immutable
public class CoordinateAxis extends VariableDS {

  /**
   * Create a coordinate axis from an existing VariableDS.Builder.
   *
   * @param vdsBuilder an existing Variable in dataset.
   * @return CoordinateAxis or one of its subclasses (CoordinateAxis1D, CoordinateAxis2D, or CoordinateAxis1DTime).
   */
  public static CoordinateAxis.Builder<?> fromVariableDS(VariableDS.Builder<?> vdsBuilder) {
    if ((vdsBuilder.getRank() == 0) || (vdsBuilder.getRank() == 1)
        || (vdsBuilder.getRank() == 2 && vdsBuilder.dataType == ArrayType.CHAR)) {
      return CoordinateAxis1D.builder().copyFrom(vdsBuilder);
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
    return getArrayType().isNumeric();
  }

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

    return true;
  }

  /**
   * Override Object.hashCode() to implement equals.
   */
  public int hashCode() {
    int result = super.hashCode();
    if (getAxisType() != null)
      result = 37 * result + getAxisType().hashCode();
    return result;
  }

  /////////////////////////////////////

  /** Figure out what calendar to use from the axis' attributes. */
  // needed by time coordinates
  public Calendar getCalendarFromAttribute() {
    return getCalendarFromAttribute(this.ncfile, this.attributes);
  }

  /** Figure out what calendar to use from the given attributes. */
  @Nullable
  public static Calendar getCalendarFromAttribute(@Nullable NetcdfFile ncd, AttributeContainer attributes) {
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
    return Calendar.get(cal).orElse(null);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  protected final AxisType axisType;
  protected final String boundaryRef; // remove

  protected CoordinateAxis(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.axisType = builder.axisType;
    this.boundaryRef = builder.boundaryRef;
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the Builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    b.setAxisType(this.axisType).setBoundary(this.boundaryRef);
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

  public static abstract class Builder<T extends Builder<T>> extends VariableDS.Builder<T> {
    public AxisType axisType;
    protected String boundaryRef;
    private boolean built;

    protected abstract T self();

    public T setAxisType(AxisType axisType) {
      this.axisType = axisType;
      return self();
    }

    public T setBoundary(String boundaryRef) {
      this.boundaryRef = boundaryRef;
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
