/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc.vertical;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayDouble.D1;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.unidata.geoloc.VerticalTransform;

/**
 * A subset of a vertical transform.
 * 
 * @deprecated use ucar.nc2.geoloc.vertical
 */
@Deprecated
@Immutable
public class VerticalTransformSubset extends AbstractVerticalTransform {
  private final VerticalTransform original;
  private final Range t_range;
  private final List<Range> subsetList = new ArrayList<>();

  /**
   * Create a subset of an existing VerticalTransform
   * 
   * @param original make a subset of this
   * @param t_range subset the time dimension, or null if you want all of it
   * @param z_range subset the vertical dimension, or null if you want all of it
   * @param y_range subset the y dimension, or null if you want all of it
   * @param x_range subset the x dimension, or null if you want all of it
   */
  VerticalTransformSubset(VerticalTransform original, Range t_range, Range z_range, Range y_range, Range x_range) {
    super(null, original.getUnitString()); // timeDim not used in this class

    this.original = original;
    this.t_range = t_range;
    subsetList.add(z_range);
    subsetList.add(y_range);
    subsetList.add(x_range);
  }

  @Override
  public ArrayDouble.D3 getCoordinateArray(int subsetIndex) throws IOException, InvalidRangeException {
    int orgIndex = subsetIndex;
    if (isTimeDependent() && (t_range != null)) {
      orgIndex = t_range.element(subsetIndex);
    }

    ArrayDouble.D3 data = original.getCoordinateArray(orgIndex);

    return (ArrayDouble.D3) data.sectionNoReduce(subsetList);
  }


  /**
   * Get the 1D vertical coordinate array for this time step and point
   * 
   * @param timeIndex the time index. Ignored if !isTimeDependent().
   * @param xIndex the x index
   * @param yIndex the y index
   * @return vertical coordinate array
   */
  @Override
  public D1 getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) throws IOException, InvalidRangeException {
    ArrayDouble.D3 data = original.getCoordinateArray(timeIndex);

    int[] origin = new int[3];
    int[] shape = new int[3];

    shape[0] = subsetList.get(0).length();
    shape[1] = 1;
    shape[2] = 1;

    origin[0] = timeIndex;
    if (isTimeDependent() && (t_range != null)) {
      origin[0] = t_range.element(timeIndex);
    }

    origin[1] = yIndex;
    origin[2] = xIndex;

    Array section = data.section(origin, shape);

    return (ArrayDouble.D1) section.reduce();
  }

  @Override
  public boolean isTimeDependent() {
    return original.isTimeDependent();
  }
}

