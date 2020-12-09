/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
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
import ucar.nc2.internal.grid.GridAxis1DHelper;
import ucar.nc2.internal.grid.TimeHelper;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.Indent;
import ucar.nc2.write.NcdumpArray;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;

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
@Immutable
public class GridAxisOffsetTimeRegular extends GridAxis {

  @Override
  public Array<Double> getCoordsAsArray() {
    return this.midpoints;
  }

  @Override
  public Array<Double> getCoordBoundsAsArray() {
    return this.bounds;
  }

  public ImmutableList<Integer> getHourOffsets() {
    return hourOffsets;
  }

  public int getNOffsetPerRun() {
    return this.midpoints.getShape()[1];
  }

  /** Get the associated Runtime Axis. */
  public GridAxis1DTime getRunTimeAxis() {
    return this.runtimeAxis;
  }

  /** Get the Time Offset axis for the given runtime. */
  @Nullable
  public GridAxis1D getTimeOffsetAxisForRun(CalendarDate rundate) {
    double rundateTarget = runtimeAxis.makeValue(rundate);
    int run_index = new GridAxis1DHelper(runtimeAxis).findCoordElement(rundateTarget, false); // LOOK not Bounded
    try {
      return (run_index < 0 || run_index >= runtimeAxis.getNcoords()) ? null : getTimeOffsetAxisForRun(run_index);
    } catch (InvalidRangeException e) {
      throw new RuntimeException(e); // cant happen
    }
  }

  /** Get the Time Offset axis for the given index into the Runtime Axis. */
  @Nullable
  public GridAxis1D getTimeOffsetAxisForRun(int run_index) throws InvalidRangeException {
    GridAxis1D.Builder<?> builder = GridAxis1D.builder().setName(name).setUnits(units).setDescription(description)
        .setAxisType(axisType).setAttributes(AttributeContainer.filter(attributes, "_Coordinate"))
        .setDependenceType(DependenceType.independent).setDependsOn(dependsOn).setSpacing(spacing); // TODO spacing ?

    Array<Double> data;
    if (spacing == Spacing.irregularPoint) {
      data = getCoordsAsArray();
    } else if (spacing == Spacing.discontiguousInterval) {
      data = getCoordBoundsAsArray();
    } else {
      // TODO what about the other cases ??
      throw new RuntimeException("getTimeAxisForRun spacing=" + spacing);
    }

    // find which hour offset to use
    CalendarDate runtime = runtimeAxis.getCalendarDate(run_index);
    int hourOffset = runtime.getHourOfDay(); // hour from 0z
    int hourOffsetIdx = hourOffsets.indexOf(hourOffset);
    if (hourOffsetIdx < 0) {
      throw new IllegalStateException(String.format("Cant find runtime %s hour offset %d in %s hours %s", runtime,
          hourOffset, getName(), hourOffsets));
    }

    // eliminate NaNs: make sure all NaNs are at the end
    Array<Double> subset = Arrays.slice(data, 0, hourOffsetIdx);
    int countNonNaN = 0;
    boolean hasNaN = false;
    for (double dval : subset) {
      if (Double.isNaN(dval)) {
        hasNaN = true;
      } else {
        if (hasNaN) {
          throw new IllegalStateException("Coordinate NaNs must be at the end for " + this.getName());
        }
        countNonNaN++;
      }
    }

    int count = 0;
    double[] values = new double[countNonNaN];
    for (double dval : subset) {
      values[count++] = dval;
      if (count == countNonNaN) {
        break;
      }
    }
    int ncoords = spacing == Spacing.irregularPoint ? values.length : values.length / 2;

    builder.setValues(values);
    builder.setNcoords(ncoords);
    builder.setIsSubset(true);
    return builder.build();
  }

  @Nullable
  @Override
  public GridAxis subset(GridSubset params, Formatter errlog) {
    if (params == null) {
      return this;
    }

    CalendarDate rundate = params.getRunTime();
    boolean runtimeAll = params.getRunTimeAll();
    boolean latest = (rundate == null) && !runtimeAll; // default is latest

    int run_index = -1;
    if (latest) {
      run_index = runtimeAxis.getNcoords() - 1;
    } else if (rundate != null) {
      GridAxis1D time1D = getTimeOffsetAxisForRun(rundate);
      return time1D.subset(params, errlog);
    }

    if (run_index >= 0) {
      try {
        GridAxis1D time1D = getTimeOffsetAxisForRun(run_index);
        return time1D.subset(params, errlog);
      } catch (InvalidRangeException e) {
        throw new RuntimeException(e);
      }
    }

    // no subsetting needed
    return this;
  }

  @Override
  public Optional<GridAxis> subsetDependent(GridAxis1D subsetIndAxis, Formatter errlog) {
    return Optional.empty(); // TODO is it ever dependent? Should it be dependent on runtime?
  }

  @Override
  public RangeIterator getRangeIterator() {
    return null; // TODO is it needed?
  }

  @Override
  public Iterator<Object> iterator() {
    return null; // TODO is it needed?
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
  private final ImmutableList<Integer> hourOffsets;

  protected GridAxisOffsetTimeRegular(Builder<?> builder) {
    super(builder);
    Preconditions.checkNotNull(builder.runtimeAxis);
    Preconditions.checkNotNull(builder.midpoints);
    Preconditions.checkNotNull(builder.bounds);
    Preconditions.checkNotNull(builder.hourOffsets);
    Preconditions.checkArgument(builder.midpoints.getRank() == 2);
    Preconditions.checkArgument(builder.bounds.getRank() == 3);
    int nhours = builder.hourOffsets.size();
    int noffsets = builder.midpoints.getShape()[1];
    Preconditions.checkArgument(java.util.Arrays.equals(builder.midpoints.getShape(), new int[] {nhours, noffsets}));
    Preconditions.checkArgument(java.util.Arrays.equals(builder.bounds.getShape(), new int[] {nhours, noffsets, 2}));

    this.runtimeAxis = builder.runtimeAxis;
    this.midpoints = builder.midpoints;
    this.bounds = builder.bounds;
    if (builder.timeHelper != null) {
      this.timeHelper = builder.timeHelper;
    } else {
      this.timeHelper = builder.runtimeAxis.getTimeHelper();
    }
    this.hourOffsets = ImmutableList.copyOf(builder.hourOffsets);
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
    private String runtimeAxisName;
    private Array<Double> midpoints;
    private Array<Double> bounds;
    private List<Integer> hourOffsets;

    protected abstract T self();

    public T setTimeHelper(TimeHelper timeHelper) {
      this.timeHelper = timeHelper;
      return self();
    }

    public T setRuntimeAxis(GridAxis1DTime runtimeAxis) {
      this.runtimeAxis = runtimeAxis;
      return self();
    }

    public T setRuntimeAxis(List<GridAxis1DTime> runtimeAxes) {
      if (runtimeAxisName == null) {
        throw new IllegalStateException("Runtime axis name not set");
      }
      runtimeAxes.stream().filter(a -> a.getName().equals(runtimeAxisName)).findFirst().ifPresent(a -> runtimeAxis = a);
      if (runtimeAxis == null) {
        throw new IllegalStateException("Cant find runtime axis " + runtimeAxisName);
      }
      return self();
    }

    public T setRuntimeAxisName(String runtimeAxisName) {
      this.runtimeAxisName = runtimeAxisName;
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

    public T setMidpointsBounds(List<Integer> shape, List<Double> midpoints, List<Double> bounds) {
      Preconditions.checkArgument(shape.size() == 2);
      int ntimes = shape.get(0);
      int nhours = shape.get(1);
      double[] mids = new double[ntimes * nhours];
      double[] bnds = new double[ntimes * nhours * 2];
      Preconditions.checkArgument(midpoints.size() == mids.length);
      Preconditions.checkArgument(bounds.size() == bnds.length);

      for (int i = 0; i < midpoints.size(); i++) {
        mids[i] = midpoints.get(i);
      }
      for (int i = 0; i < bounds.size(); i++) {
        bnds[i] = bounds.get(i);
      }
      this.midpoints = Arrays.factory(DataType.DOUBLE, new int[] {ntimes, nhours}, mids);
      this.bounds = Arrays.factory(DataType.DOUBLE, new int[] {ntimes, nhours, 2}, bnds);

      return self();
    }

    public T setHourOffsets(List<Integer> hourOffsets) {
      this.hourOffsets = hourOffsets;
      return self();
    }

    public T setHourOffsets(Array<Integer> hourOffsets) {
      this.hourOffsets = new ArrayList<>();
      for (int hour : hourOffsets) {
        this.hourOffsets.add(hour);
      }
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
