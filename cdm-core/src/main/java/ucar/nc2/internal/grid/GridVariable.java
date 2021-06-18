package ucar.nc2.internal.grid;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.grid.MaterializedCoordinateSystem;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.*;

/** Wraps a VariableDS, turns into a Grid */
@Immutable
public class GridVariable implements Grid {
  private final GridCS cs;
  private final VariableDS vds;
  private final GridIndexPermuter permuter;

  GridVariable(GridCS cs, VariableDS vds) {
    this.cs = cs;
    this.vds = vds;
    this.permuter = new GridIndexPermuter(cs, vds);
  }

  @Override
  public GridCoordinateSystem getCoordinateSystem() {
    return this.cs;
  }

  @Override
  public String getName() {
    return vds.getShortName();
  }

  @Override
  public String getUnits() {
    return vds.getUnitsString() == null ? "" : vds.getUnitsString();
  }

  @Override
  public String getDescription() {
    return vds.getDescription() == null ? "" : vds.getDescription();
  }

  @Override
  public AttributeContainer attributes() {
    return vds.attributes();
  }

  @Override
  public ArrayType getArrayType() {
    return vds.getArrayType();
  }

  @Override
  public boolean hasMissing() {
    return vds.hasMissing();
  }

  @Override
  public boolean isMissing(double val) {
    return vds.isMissing(val);
  }

  @Override
  public String toString() {
    return vds + "\n permuter=" + permuter + '}';
  }

  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, ucar.array.InvalidRangeException {
    Formatter errlog = new Formatter();
    Optional<MaterializedCoordinateSystem> opt = this.cs.subset(subset, errlog);
    if (opt.isEmpty()) {
      throw new ucar.array.InvalidRangeException(errlog.toString());
    }

    MaterializedCoordinateSystem subsetCoordSys = opt.get();
    List<ucar.array.RangeIterator> rangeIters = subsetCoordSys.getRanges();
    List<ucar.array.Range> ranges = new ArrayList<>();

    boolean hasComposite = false;
    for (ucar.array.RangeIterator ri : rangeIters) {
      // if (ri instanceof ucar.array.RangeComposite) // TODO LOOK
      // hasComposite = true;
      // else
      ranges.add((ucar.array.Range) ri);
    }

    // if (!hasComposite) {
    Array<Number> data = readDataSection(new ucar.array.Section(ranges), true);
    // LOOK
    return GridReferencedArray.create(getName(), getArrayType(), data, subsetCoordSys);
    // }

    // throw new UnsupportedOperationException();
  }

  /**
   * This reads an arbitrary data section, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param subset - each Range must be named by the axisType that its used for. order not important
   *
   * @return data[rt, e, t, z, y, x], eliminating missing dimensions. length=1 not eliminated
   */
  private Array<Number> readDataSection(ucar.array.Section subset, boolean canonicalOrder)
      throws ucar.array.InvalidRangeException, IOException {
    // read it
    Array<?> dataVolume = vds.readArray(this.permuter.permute(subset));

    /*
     * permute to canonical order if needed; order rt,e,t,z,y,x
     * if (canonicalOrder) {
     * int[] permuteIndex = calcPermuteIndex();
     * if (permuteIndex != null)
     * dataVolume = dataVolume.permute(permuteIndex);
     * }
     */

    // LOOK
    return (Array<Number>) dataVolume;
  }
}
