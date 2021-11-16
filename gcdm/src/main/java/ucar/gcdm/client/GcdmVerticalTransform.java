/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.gcdm.client;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.Range;
import ucar.nc2.geoloc.vertical.VerticalTransform;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Implementation of VerticalTransform that makes a gcmd to get the 3D array.
 * Not immutable because we need to set the GcdmGridDataset after construction.
 */
public class GcdmVerticalTransform implements VerticalTransform {
  private GcdmGridDataset gridDataset;
  private final int id;
  private final String name;
  private final String ctvName;
  private final String units;

  public GcdmVerticalTransform(int id, String name, String ctvName, String units) {
    this.id = id;
    this.name = name;
    this.ctvName = ctvName;
    this.units = units;
  }

  void setDataset(GcdmGridDataset gridDataset) {
    this.gridDataset = gridDataset;
  }

  public int getId() {
    return id;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getCtvName() {
    return ctvName;
  }

  @Nullable
  @Override
  public String getUnitString() {
    return units;
  }

  @Override
  public Array<Number> getCoordinateArray3D(int timeIndex) {
    return gridDataset.getVerticalTransform(this.id, this.name, timeIndex);
  }

  // Implementation that reads the full 3D array and just picks out the specified x, y.
  // Optimization could cache at least one time index
  @Override
  public Array<Number> getCoordinateArray1D(int timeIndex, int xIndex, int yIndex) {
    Array<Number> array3D = getCoordinateArray3D(timeIndex);
    int nz = array3D.getShape()[0];
    double[] result = new double[nz];

    int count = 0;
    for (int z = 0; z < nz; z++) {
      result[count++] = array3D.get(z, yIndex, xIndex).doubleValue();
    }

    return Arrays.factory(ArrayType.DOUBLE, new int[] {nz}, result);
  }
}
