/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import ucar.array.Array;
import ucar.array.Arrays;
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
 * A TimeOffset Axis where the offsets depend on the runtime hour since 0z.
 * A real life example from NAM-Polar_90km.ncx4:
 * 
 * <pre>
 * validtime1 runtime=reftime nruns=123 ntimes=52 isOrthogonal=false isRegular=true
 * hour 0:  timeIntv: (0,1), (0,2), (0,3), (0,4), (0,5), (0,6), (6,7), (6,8), (6,9), (6,10), (6,11), (6,12), (12,13), (12,14), (12,15), (12,16), (12,17), (12,18), (18,19), (18,20), (18,21), (18,22), (18,23), (18,24), (24,25), (24,26), (24,27), (24,28), (24,29), (24,30), (30,31), (30,32), (30,33), (30,34), (30,35), (30,36), (36,39), (36,42), (42,45), (42,48), (48,51), (48,54), (54,57), (54,60), (60,63), (60,66), (66,69), (66,72), (72,75), (72,78), (78,81), (78,84), (52)
 * hour 6:  timeIntv: (0,1), (0,2), (0,3), (3,4), (3,5), (3,6), (6,7), (6,8), (6,9), (9,10), (9,11), (9,12), (12,13), (12,14), (12,15), (15,16), (15,17), (15,18), (18,19), (18,20), (18,21), (21,22), (21,23), (21,24), (24,25), (24,26), (24,27), (27,28), (27,29), (27,30), (30,31), (30,32), (30,33), (33,34), (33,35), (33,36), (36,39), (39,42), (42,45), (45,48), (48,51), (51,54), (54,57), (57,60), (60,63), (63,66), (66,69), (69,72), (72,75), (75,78), (78,81), (81,84), (52)
 * hour 12: timeIntv: (0,1), (0,2), (0,3), (0,4), (0,5), (0,6), (6,7), (6,8), (6,9), (6,10), (6,11), (6,12), (12,13), (12,14), (12,15), (12,16), (12,17), (12,18), (18,19), (18,20), (18,21), (18,22), (18,23), (18,24), (24,25), (24,26), (24,27), (24,28), (24,29), (24,30), (30,31), (30,32), (30,33), (30,34), (30,35), (30,36), (36,39), (36,42), (42,45), (42,48), (48,51), (48,54), (54,57), (54,60), (60,63), (60,66), (66,69), (66,72), (72,75), (72,78), (78,81), (78,84), (52)
 * hour 18: timeIntv: (0,1), (0,2), (0,3), (3,4), (3,5), (3,6), (6,7), (6,8), (6,9), (9,10), (9,11), (9,12), (12,13), (12,14), (12,15), (15,16), (15,17), (15,18), (18,19), (18,20), (18,21), (21,22), (21,23), (21,24), (24,25), (24,26), (24,27), (27,28), (27,29), (27,30), (30,31), (30,32), (30,33), (33,34), (33,35), (33,36), (36,39), (39,42), (42,45), (45,48), (48,51), (51,54), (54,57), (57,60), (60,63), (63,66), (66,69), (69,72), (72,75), (75,78), (78,81), (81,84), (52)
 * </pre>
 * 
 * An orthogonal TimeOffset means the same offsets for any runtime, so both runtime and timeOffset are 1D.
 * A regular Timeoffset means it varies based on the hour of the runtime from 0Z.
 */
public class GridAxisOffsetTimeRegular extends GridAxis {

  @Override
  public Array<Double> getCoordsAsArray() {
    return this.midpoints;
  }

  @Override
  public Array<Double> getCoordBoundsAsArray() {
    return this.bounds;
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

  @Override
  public void toString(Formatter f, Indent indent) {
    super.toString(f, indent);
    f.format("%s  %s%n", indent, java.util.Arrays.toString(getCoordsAsArray().getShape()));
    f.format("%s%n", NcdumpArray.printArray(getCoordsAsArray(), getName() + " values", null));
    if (getCoordBoundsAsArray() != null) {
      f.format("%n%s%n", NcdumpArray.printArray(getCoordBoundsAsArray(), getName() + " bounds", null));
    }
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final TimeHelper timeHelper; // AxisType = Time, RunTime only
  private final GridAxis1DTime runtimeAxis;
  private final Array<Double> midpoints;
  private final Array<Double> bounds;
  private final Array<Integer> hourOffsets;

  protected GridAxisOffsetTimeRegular(Builder<?> builder) {
    super(builder);
    this.runtimeAxis = builder.runtimeAxis;
    this.midpoints = builder.midpoints;
    this.bounds = builder.bounds;
    this.hourOffsets = builder.hourOffsets;
    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      this.timeHelper = TimeHelper.factory(this.units, this.attributes);
    }
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
    builder.setTimeHelper(this.timeHelper);
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

  public static abstract class Builder<T extends Builder<T>> extends GridAxis.Builder<T> {
    private boolean built;
    private TimeHelper timeHelper;
    private GridAxis1DTime runtimeAxis;
    private Array<Double> midpoints;
    private Array<Double> bounds;
    private Array<Integer> hourOffsets;

    protected abstract T self();

    public T setTimeHelper(TimeHelper timeHelper) {
      this.timeHelper = timeHelper;
      return self();
    }

    public T setRuntimeAxis(GridAxis1DTime runtimeAxis) {
      this.runtimeAxis = runtimeAxis;
      return self();
    }

    public T setMidpoints(Array<Double> midpoints) {
      this.midpoints = midpoints;
      return self();
    }

    public T setBounds(Array<Double> bounds) {
      this.bounds = bounds;
      return self();
    }

    public T setHourOffsets(Array<Integer> hourOffsets) {
      this.hourOffsets = hourOffsets;
      return self();
    }

    public GridAxisOffsetTimeRegular build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;

      this.dependenceType = DependenceType.fmrcReg;
      this.setDependsOn(runtimeAxis.getName());
      this.axisType = AxisType.TimeOffset;
      return new GridAxisOffsetTimeRegular(this);
    }
  }

}
