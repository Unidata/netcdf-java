/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * A 1-dimensional Coordinate Axis representing Calendar time.
 * Its coordinate values can be represented as Dates.
 * Legacy class used only by Aggregation.
 */
public class CoordinateAxis1DTime extends CoordinateAxis1D {

  private static final Logger logger = LoggerFactory.getLogger(CoordinateAxis1DTime.class);

  public static CoordinateAxis1DTime factory(@Nullable NetcdfDataset ncd, VariableDS org, Formatter errMessages)
      throws IOException {
    if (org instanceof CoordinateAxis1DTime) {
      return (CoordinateAxis1DTime) org;
    }
    if (org.getArrayType() == ArrayType.CHAR) {
      return fromStringVarDS(ncd, org, ImmutableList.of(org.getDimension(0)));
    }
    if (org.getArrayType() == ArrayType.STRING) {
      return fromStringVarDS(ncd, org, org.getDimensions());
    }
    return fromVarDS(ncd, org, errMessages);
  }

  /**
   * Constructor for CHAR or STRING variables.
   * Must contain ISO dates.
   *
   * @param ncd the containing dataset
   * @param org the underlying Variable
   * @param dims list of dimensions
   * @throws IllegalArgumentException if cant convert coordinate values to a Date
   */
  private static CoordinateAxis1DTime fromStringVarDS(@Nullable NetcdfDataset ncd, VariableDS org,
      List<Dimension> dims) {
    CoordinateAxis1DTime.Builder<?> builder =
        CoordinateAxis1DTime.builder().setName(org.getShortName()).setArrayType(ArrayType.STRING)
            .setUnits(org.getUnitsString()).setDesc(org.getDescription()).setDimensions(dims);
    builder.setOriginalVariable(org).setOriginalName(org.getOriginalName());

    builder.setTimeHelper(new CoordinateAxisTimeHelper(getCalendarFromAttribute(ncd, org.attributes()), null));
    builder.addAttributes(org.attributes());
    if (org instanceof CoordinateAxis) {
      builder.setAxisType(((CoordinateAxis) org).getAxisType());
    }
    return builder.build(org.getParentGroup());
  }

  /**
   * Constructor for numeric values - must have units
   *
   * @param ncd the containing dataset
   * @param org the underlying Variable
   * @throws IOException on read error
   */
  private static CoordinateAxis1DTime fromVarDS(@Nullable NetcdfDataset ncd, VariableDS org, Formatter errMessages)
      throws IOException {
    CoordinateAxis1DTime.Builder<?> builder = CoordinateAxis1DTime.builder().setName(org.getShortName())
        .setArrayType(org.getArrayType()).setUnits(org.getUnitsString()).setDesc(org.getDescription());
    builder.setOriginalVariable(org).setOriginalName(org.getOriginalName());

    CoordinateAxisTimeHelper helper =
        new CoordinateAxisTimeHelper(getCalendarFromAttribute(ncd, org.attributes()), org.getUnitsString());
    builder.setTimeHelper(helper);

    // make the coordinates
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);
    Array<Number> data = (Array<Number>) org.readArray();

    int count = 0;
    for (int i = 0; i < ncoords; i++) {
      double val = data.get(i).doubleValue();
      if (Double.isNaN(val)) {
        continue;
      }
      result.add(helper.makeCalendarDateFromOffset((int) val));
      count++;
    }

    ArrayList<Dimension> dims = new ArrayList<>(org.getDimensions());
    builder.setCalendarDates(result);
    builder.setDimensions(dims);
    builder.addAttributes(org.attributes());

    if (org instanceof CoordinateAxis) {
      builder.setAxisType(((CoordinateAxis) org).getAxisType());
    }
    return builder.build(org.getParentGroup());
  }

  ////////////////////////////////////////////////////////////////

  /**
   * Get the list of datetimes in this coordinate as CalendarDate objects.
   *
   * @return list of CalendarDates.
   */
  public List<CalendarDate> getCalendarDates() {
    return cdates;
  }

  ////////////////////////////////////////////////////////////////////////

  // TODO move to builder
  private List<CalendarDate> makeTimesFromChar(VariableDS org, Formatter errMessages) throws IOException {
    int ncoords = (int) org.getSize();
    int rank = org.getRank();
    int strlen = org.getShape(rank - 1);
    ncoords /= strlen;

    List<CalendarDate> result = new ArrayList<>(ncoords);

    Array<Byte> data = (Array<Byte>) org.readArray();
    Array<String> sdata = Arrays.makeStringsFromChar(data);

    String[] dateStrings = new String[ncoords];
    for (int i = 0; i < ncoords; i++) {
      String coordValue = sdata.get(i);
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      dateStrings[i] = coordValue;
      result.add(cd);
    }
    return result;
  }

  private List<CalendarDate> makeTimesFromStrings(VariableDS org, Formatter errMessages) throws IOException {
    int ncoords = (int) org.getSize();
    List<CalendarDate> result = new ArrayList<>(ncoords);

    Array<String> data = (Array<String>) org.readArray();
    for (int i = 0; i < ncoords; i++) {
      String coordValue = data.get(i);
      CalendarDate cd = makeCalendarDateFromStringCoord(coordValue, org, errMessages);
      result.add(cd);
    }

    return result;
  }

  private CalendarDate makeCalendarDateFromStringCoord(String coordValue, VariableDS org, Formatter errMessages) {
    CalendarDate cd = helper.makeCalendarDateFromOffset(coordValue);
    if (cd == null) {
      if (errMessages != null) {
        errMessages.format("String time coordinate must be ISO formatted= %s%n", coordValue);
        logger.info("Char time coordinate must be ISO formatted= {} file = {}", coordValue, org.getDatasetLocation());
      }
      throw new IllegalArgumentException();
    }
    return cd;
  }


  ////////////////////////////////////////////////////////////////////////////////////////////
  private final CoordinateAxisTimeHelper helper;
  private final List<CalendarDate> cdates;

  protected CoordinateAxis1DTime(Builder<?> builder, Group parentGroup) {
    super(builder, parentGroup);
    this.helper = builder.helper;

    try {
      Formatter errMessages = new Formatter();
      if (getArrayType() == ArrayType.CHAR) {
        cdates = makeTimesFromChar((VariableDS) builder.orgVar, errMessages);
      } else if (getArrayType() == ArrayType.STRING) {
        cdates = makeTimesFromStrings((VariableDS) builder.orgVar, errMessages);
      } else {
        cdates = builder.cdates;
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends Builder<?>> b) {
    return (Builder<?>) super.addLocalFieldsToBuilder(b);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  @Deprecated
  public static abstract class Builder<T extends Builder<T>> extends CoordinateAxis1D.Builder<T> {
    private boolean built;
    private CoordinateAxisTimeHelper helper;
    private List<CalendarDate> cdates;

    protected abstract T self();

    public T setTimeHelper(CoordinateAxisTimeHelper helper) {
      this.helper = helper;
      return self();
    }

    public T setCalendarDates(List<CalendarDate> cdates) {
      this.cdates = cdates;
      return self();
    }

    public CoordinateAxis1DTime build(Group parentGroup) {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      if (axisType == null) {
        axisType = AxisType.Time;
      }
      return new CoordinateAxis1DTime(this, parentGroup);
    }
  }
}
