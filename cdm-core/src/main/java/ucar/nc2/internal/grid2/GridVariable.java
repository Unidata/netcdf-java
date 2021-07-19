package ucar.nc2.internal.grid2;

import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid2.Grid;
import ucar.nc2.grid2.GridCoordinateSystem;
import ucar.nc2.grid2.GridReferencedArray;
import ucar.nc2.grid2.GridSubset;
import ucar.nc2.grid2.MaterializedCoordinateSystem;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.Formatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Wraps a VariableDS, turns into a Grid */
@Immutable
public class GridVariable implements Grid {
  private final GridNetcdfCS cs;
  private final VariableDS vds;
  private final GridIndexPermuter permuter;

  GridVariable(GridNetcdfCS cs, VariableDS vds) {
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

  /** Subsetting the coordinate system, then using that subset to do the read. Special to Netcdf, not general. */
  @Override
  public GridReferencedArray readData(GridSubset subset) throws IOException, ucar.array.InvalidRangeException {
    Formatter errlog = new Formatter();
    Optional<MaterializedCoordinateSystem> opt = this.cs.subset(subset, errlog);
    if (opt.isEmpty()) {
      throw new ucar.array.InvalidRangeException(errlog.toString());
    }

    MaterializedCoordinateSystem subsetCoordSys = opt.get();
    List<ucar.array.Range> ranges = subsetCoordSys.getSubsetRanges();
    Array<Number> data = readDataSection(new ucar.array.Section(ranges), true);
    return GridReferencedArray.create(getName(), getArrayType(), data, subsetCoordSys);
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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GridVariable that = (GridVariable) o;
    return vds.equals(that.vds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(vds);
  }

  @Override
  public String toString() {
    return vds + "\n permuter=" + permuter + '}';
  }

}
