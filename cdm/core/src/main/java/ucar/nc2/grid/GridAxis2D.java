package ucar.nc2.grid;

import ucar.array.Array;
import ucar.ma2.RangeIterator;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.Iterator;

/** Used for curvilinear lat(i, j) and lon(i, j). No intervals. */
public class GridAxis2D extends GridAxis {
  private int nx, ny;

  protected GridAxis2D(Builder<?> builder) {
    super(builder);
  }

  public int[] getShape() {
    return new int[] {ny, nx};
  }

  public double getCoordValue(int yindex, int xindex) {
    return Double.NaN;
  }

  public double getCoordMin() {
    return Double.NaN;
  }

  public double getCoordMax() {
    return Double.NaN;
  }

  @Override
  public Array<Double> getCoordsAsArray() {
    return null;
  }

  @Override
  public Array<Double> getCoordBoundsAsArray() {
    return null;
  }

  @Nullable
  @Override
  public GridAxis subset(GridSubset params, Formatter errlog) {
    return null;
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
}
