package ucar.nc2.internal.grid;

import ucar.array.Array;
import ucar.ma2.*;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ft2.coverage.SubsetParams;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridReferencedArray;

import java.io.IOException;
import java.util.*;

/** Wraps a VariableDS, turns into a Grid */
public class GridVariable implements Grid {
  private final GridCoordinateSystem cs;
  private final VariableDS vds;

  GridVariable(GridCoordinateSystem cs, VariableDS vds) {
    this.cs = cs;
    this.vds = vds;
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
  public String getUnitsString() {
    return vds.getUnitsString();
  }

  @Override
  public String getDescription() {
    return vds.getDescription();
  }

  @Override
  public String getCoordSysName() {
    return cs.getName();
  }

  @Override
  public DataType getDataType() {
    return vds.getDataType();
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
    return vds.getShortName();
  }

  @Override
  public GridReferencedArray readData(SubsetParams subset) throws IOException, InvalidRangeException {
    GridCoordinateSystem orgCoordSys = this.cs;
    Formatter errlog = new Formatter();
    Optional<GridCoordinateSystem> opt = orgCoordSys.subset(subset, errlog);
    if (!opt.isPresent()) {
      throw new InvalidRangeException(errlog.toString());
    }

    GridCoordinateSystem subsetCoordSys = opt.get();
    List<RangeIterator> rangeIters = subsetCoordSys.getRanges();
    List<Range> ranges = new ArrayList<>();

    boolean hasComposite = false;
    for (RangeIterator ri : rangeIters) {
      if (ri instanceof RangeComposite) // TODO
        hasComposite = true;
      else
        ranges.add((Range) ri);
    }

    if (!hasComposite) {
      Array<Double> data = readDataSection(new Section(ranges), true);
      return GridReferencedArray.create(getName(), getDataType(), data, subsetCoordSys);
    }

    throw new UnsupportedOperationException();
  }

  /**
   * This reads an arbitrary data section, returning the data in
   * canonical order (rt-e-t-z-y-x). If any dimension does not exist, ignore it.
   *
   * @param section - each Range must be named by the axisType that its used for. order not important
   *
   * @return data[rt, e, t, z, y, x], eliminating missing dimensions. length=1 not eliminated
   */
  public Array<Double> readDataSection(Section section, boolean canonicalOrder)
      throws IOException, InvalidRangeException {



    /*
     * get the ranges list in the order of the variable; a null range means "all" to vs.read()
     * Range[] varRange = new Range[getRank()];
     * for (Range r : subset.getRanges()) {
     * AxisType type = AxisType.valueOf(r.getName());
     * switch (type) {
     * case Lon:
     * case GeoX:
     * varRange[xDimOrgIndex] = r;
     * break;
     * case Lat:
     * case GeoY:
     * varRange[yDimOrgIndex] = r;
     * break;
     * case Height:
     * case Pressure:
     * case GeoZ:
     * varRange[zDimOrgIndex] = r;
     * break;
     * case Time:
     * varRange[tDimOrgIndex] = r;
     * break;
     * case RunTime:
     * varRange[rtDimOrgIndex] = r;
     * break;
     * case Ensemble:
     * varRange[eDimOrgIndex] = r;
     * break;
     * }
     * }
     */

    // read it
    Array<Double> dataVolume = (Array<Double>) vds.readArray(section);

    /*
     * permute to canonical order if needed; order rt,e,t,z,y,x
     * if (canonicalOrder) {
     * int[] permuteIndex = calcPermuteIndex();
     * if (permuteIndex != null)
     * dataVolume = dataVolume.permute(permuteIndex);
     * }
     */

    return dataVolume;
  }

  /*
   * private int[] calcPermuteIndex() {
   * Dimension xdim = getXDimension();
   * Dimension ydim = getYDimension();
   * Dimension zdim = getZDimension();
   * Dimension tdim = getTimeDimension();
   * Dimension edim = getEnsembleDimension();
   * Dimension rtdim = getRunTimeDimension();
   * 
   * // LOOK: the real problem is the lack of named dimensions in the Array object
   * // figure out correct permutation for canonical ordering for permute
   * List<Dimension> oldDims = vs.getDimensions();
   * int[] permuteIndex = new int[vs.getRank()];
   * int count = 0;
   * if (oldDims.contains(rtdim))
   * permuteIndex[count++] = oldDims.indexOf(rtdim);
   * if (oldDims.contains(edim))
   * permuteIndex[count++] = oldDims.indexOf(edim);
   * if (oldDims.contains(tdim))
   * permuteIndex[count++] = oldDims.indexOf(tdim);
   * if (oldDims.contains(zdim))
   * permuteIndex[count++] = oldDims.indexOf(zdim);
   * if (oldDims.contains(ydim))
   * permuteIndex[count++] = oldDims.indexOf(ydim);
   * if (oldDims.contains(xdim))
   * permuteIndex[count] = oldDims.indexOf(xdim);
   * 
   * if (debugArrayShape) {
   * System.out.println("oldDims = ");
   * for (Dimension oldDim : oldDims)
   * System.out.println("   oldDim = " + oldDim.getShortName());
   * System.out.println("permute dims = ");
   * for (int aPermuteIndex : permuteIndex)
   * System.out.println("   oldDim index = " + aPermuteIndex);
   * }
   * 
   * // check to see if we need to permute
   * boolean needPermute = false;
   * for (int i = 0; i < permuteIndex.length; i++) {
   * if (i != permuteIndex[i])
   * needPermute = true;
   * }
   * 
   * return needPermute ? permuteIndex : null;
   * }
   */
}
