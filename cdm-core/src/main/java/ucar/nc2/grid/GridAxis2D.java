package ucar.nc2.grid;

import ucar.array.Array;
import ucar.array.RangeIterator;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Optional;

/** A 2 dimensional GridAxis, used for curvilinear lat(i, j) and lon(i, j). No intervals. */
public class GridAxis2D extends GridAxis {
  private int nx, ny;

  protected GridAxis2D(Builder<?> builder) {
    super(builder);
  }

  @Override
  public int[] getNominalShape() {
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

  // @Override
  public Array<Double> getCoordsAsArray() {
    return null;
  }

  // @Override
  public Array<Double> getCoordBoundsAsArray() {
    return null;
  }

  @Nullable
  @Override
  public GridAxis subset(GridSubset params, Formatter errlog) {
    return null;
  }

  @Override
  public Optional<GridAxis> subsetDependent(GridAxis1D subsetIndAxis, Formatter errlog) {
    return Optional.empty(); // TODO
  }

  // @Override
  public RangeIterator getRangeIterator() {
    return null;
  }

  @Override
  public Iterator<Object> iterator() {
    return null;
  }
}
