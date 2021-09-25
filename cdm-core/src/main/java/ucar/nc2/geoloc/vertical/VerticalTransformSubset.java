/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.geoloc.vertical;

import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.array.Section;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** A subset of a vertical transform. */
@Immutable
public class VerticalTransformSubset extends AbstractVerticalTransform {
  private final AbstractVerticalTransform original;
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
  VerticalTransformSubset(AbstractVerticalTransform original, Range t_range, Range z_range, Range y_range,
      Range x_range) {
    super(null, original.getUnitString()); // timeDim not used in this class

    this.original = original;
    this.t_range = t_range;
    subsetList.add(z_range);
    subsetList.add(y_range);
    subsetList.add(x_range);
  }

  @Override
  public Array<Number> getCoordinateArray3D(int subsetIndex) throws IOException, InvalidRangeException {
    int orgIndex = subsetIndex;
    if (isTimeDependent() && (t_range != null)) {
      orgIndex = t_range.element(subsetIndex);
    }

    Array<Number> data = original.getCoordinateArray3D(orgIndex);
    return Arrays.section(data, new Section(subsetList));
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
  public Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex)
      throws IOException, InvalidRangeException {
    Array<Number> data = original.getCoordinateArray3D(timeIndex);

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

    Section section = new Section(origin, shape);
    return Arrays.reduce(Arrays.section(data, section));

  }

  @Override
  public boolean isTimeDependent() {
    return original.isTimeDependent();
  }
}

