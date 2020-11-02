/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.ma2.RangeIterator;
import ucar.nc2.AttributeContainer;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.nc2.write.NcdumpArray;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

/**
 * Grid 2D TimeAxis: time(runtime, time)
 * values will contain nruns * ntimes values
 * NOT DONE YET
 */
public class GridAxisFmrcTime extends GridAxis {

  @Override
  public Array<Double> getCoordsAsArray() {
    double[] values = getValues();
    return Arrays.factory(DataType.DOUBLE, shape, values);
  }

  @Override
  public Array<Double> getCoordBoundsAsArray() {
    double[] values = getValues();
    int[] shapeB = new int[3];
    System.arraycopy(shape, 0, shapeB, 0, 2);
    shapeB[2] = 2;
    return Arrays.factory(DataType.DOUBLE, shapeB, values);
  }

  public GridAxis1DTime getRunTimeAxis() {
    return this.runtimeAxis;
  }

  public GridAxis1DTime getTimeAxisForRun(CalendarDate rundate) {
    double rundateTarget = runtimeAxis.makeValue(rundate);
    int run_index = new GridAxis1DHelper(runtimeAxis).findCoordElement(rundateTarget, false); // LOOK not Bounded
    try {
      return (run_index < 0 || run_index >= runtimeAxis.getNcoords()) ? null : getTimeAxisForRun(run_index);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  public GridAxis1DTime getTimeAxisForRun(int run_index) throws InvalidRangeException {
    GridAxis1DTime.Builder<?> builder =
        GridAxis1DTime.builder().setName(name).setUnits(units).setDescription(description).setDataType(dataType)
            .setAxisType(axisType).setAttributes(AttributeContainer.filter(attributes, "_Coordinate"))
            .setDependenceType(dependenceType).setDependsOn(dependsOn).setSpacing(spacing).setReader(reader);

    double[] values = null;
    if (spacing == Spacing.irregularPoint) {
      Array<Double> data = getCoordsAsArray();
      Array<Double> subset = Arrays.slice(data, 0, run_index);

      int count = 0;
      int n = (int) subset.length();
      values = new double[n];
      for (double dval : subset) {
        values[count++] = dval;
      }

    } else if (spacing == Spacing.discontiguousInterval) {
      Array<Double> data = getCoordBoundsAsArray();
      Array<Double> subset = Arrays.slice(data, 0, run_index);

      int count = 0;
      int n = (int) subset.length();
      values = new double[n];
      for (double dval : subset) {
        values[count++] = dval;
      }
    }

    // TODO what about the other cases ??
    builder.setValues(values);
    builder.setIsSubset(true);
    return builder.build();
  }

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  %s%n", indent, java.util.Arrays.toString(shape));
    Array<?> data = getCoordsAsArray();
    f.format("%s%n", NcdumpArray.printArray(data, getName() + " values", null));
  }

  @Nullable
  @Override
  public GridAxis subset(GridSubset params, Formatter errlog) {
    if (params == null) {
      return this;
    }

    CalendarDate rundate = (CalendarDate) params.get(GridSubset.runtime);
    boolean runtimeAll = (Boolean) params.get(GridSubset.runtimeAll);
    boolean latest = (rundate == null) && !runtimeAll; // default is latest

    int run_index = -1;
    if (latest) {
      run_index = runtimeAxis.getNcoords() - 1;

    } else if (rundate != null) {
      GridAxis1DTime time1D = getTimeAxisForRun(rundate);
      return time1D.subset(params, errlog);
    }
    if (run_index >= 0) {
      try {
        GridAxis1DTime time1D = getTimeAxisForRun(run_index);
        return time1D.subset(params, errlog);
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }
    }

    // no subsetting needed
    return this;
  }

  @Nullable
  @Override
  public GridAxis subsetDependent(GridAxis1D dependsOn, Formatter errlog) {
    return null;
  }

  @Override
  public RangeIterator getRangeIterator() {
    return null;
  }

  @Override
  public Iterator<Object> iterator() {
    return null;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final TimeHelper timeHelper; // AxisType = Time, RunTime only
  private final ImmutableList<CalendarDate> cdates;

  // can only be set once, needed for subsetting
  private int[] shape;
  private final GridAxis1DTime runtimeAxis;

  protected GridAxisFmrcTime(Builder<?> builder) {
    super(builder);
    this.runtimeAxis = builder.runtimeAxis;
    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      this.timeHelper = TimeHelper.factory(this.units, this.attributes);
    }
    this.cdates = ImmutableList.copyOf(builder.cdates);
    Preconditions.checkArgument(cdates.size() == this.getNcoords());
  }

  private ImmutableList<CalendarDate> subsetDatesByRange(List<CalendarDate> dates, Range range) {
    ImmutableList.Builder<CalendarDate> builder = ImmutableList.builder();
    for (int index : range) {
      builder.add(dates.get(index));
    }
    return builder.build();
  }

  public Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the passed - in builder.
  protected Builder<?> addLocalFieldsToBuilder(Builder<? extends GridAxis.Builder<?>> builder) {
    builder.setTimeHelper(this.timeHelper).setCalendarDates(this.cdates);
    return (Builder<?>) super.addLocalFieldsToBuilder(builder);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  /** A builder taking fields from a VariableDS */
  public static Builder<?> builder(VariableDS vds) {
    return builder().initFromVariableDS(vds);
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends GridAxisFmrcTime.Builder<T>> extends GridAxis.Builder<T> {
    private boolean built;
    private TimeHelper timeHelper;
    private List<CalendarDate> cdates;
    private GridAxis1DTime runtimeAxis;

    protected abstract T self();

    public T setTimeHelper(TimeHelper timeHelper) {
      this.timeHelper = timeHelper;
      return self();
    }

    public T setCalendarDates(List<CalendarDate> cdates) {
      this.cdates = cdates;
      return self();
    }

    public void setRuntimeAxis(GridAxis1DTime runtimeAxis) {
      this.runtimeAxis = runtimeAxis;
    }

    public GridAxisFmrcTime build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      if (axisType == null) {
        axisType = AxisType.Time;
      }
      return new GridAxisFmrcTime(this);
    }
  }

}
