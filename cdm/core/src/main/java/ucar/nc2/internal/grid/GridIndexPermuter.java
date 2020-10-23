package ucar.nc2.internal.grid;

import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.Dimension;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridCoordinateSystem;

import javax.annotation.concurrent.Immutable;
import java.util.Arrays;
import java.util.List;

@Immutable
class GridIndexPermuter {
  private final int rank;
  private final int xDimOrgIndex, yDimOrgIndex, zDimOrgIndex, eDimOrgIndex, tDimOrgIndex, toDimOrgIndex, rtDimOrgIndex;

  GridIndexPermuter(GridCoordinateSystem gcs, VariableDS vds) {
    this.rank = vds.getRank();
    this.xDimOrgIndex = findDimension(vds, gcs.getXHorizAxis());
    this.yDimOrgIndex = findDimension(vds, gcs.getYHorizAxis());
    this.zDimOrgIndex = findDimension(vds, gcs.getVerticalAxis());
    this.eDimOrgIndex = findDimension(vds, gcs.getEnsembleAxis());
    this.tDimOrgIndex = findDimension(vds, gcs.getTimeAxis());
    this.toDimOrgIndex = findDimension(vds, gcs.getTimeOffsetAxis());
    this.rtDimOrgIndex = findDimension(vds, gcs.getRunTimeAxis());
  }

  private int findDimension(VariableDS vds, GridAxis want) {
    if (want == null) {
      return -1;
    }
    List<Dimension> dims = vds.getDimensions();
    for (int i = 0; i < dims.size(); i++) {
      Dimension d = dims.get(i);
      if (d.getShortName().equals(want.getName())) // LOOK
        return i;
    }
    return -1;
  }

  Section permute(Section subset) {
    // get the ranges list in the order of the variable; a null range means "all" to vs.read()
    Range[] varRange = new Range[this.rank];
    for (Range r : subset.getRanges()) {
      AxisType type = AxisType.valueOf(r.getName());
      switch (type) {
        case Lon:
        case GeoX:
          varRange[xDimOrgIndex] = r;
          break;
        case Lat:
        case GeoY:
          varRange[yDimOrgIndex] = r;
          break;
        case Height:
        case Pressure:
        case GeoZ:
          varRange[zDimOrgIndex] = r;
          break;
        case Time:
          varRange[tDimOrgIndex] = r;
          break;
        case TimeOffset:
          varRange[toDimOrgIndex] = r;
          break;
        case RunTime:
          varRange[rtDimOrgIndex] = r;
          break;
        case Ensemble:
          varRange[eDimOrgIndex] = r;
          break;
        default:
          throw new RuntimeException("Unknown axis type " + type);
      }
    }
    return new Section(Arrays.asList(varRange));
  }

  @Override
  public String toString() {
    return "GridIndexPermuter{" + "rank=" + rank + ", eDimOrgIndex=" + eDimOrgIndex + ", rtDimOrgIndex=" + rtDimOrgIndex
        + ", toDimOrgIndex=" + toDimOrgIndex + ", tDimOrgIndex=" + tDimOrgIndex + ", zDimOrgIndex=" + zDimOrgIndex
        + ", yDimOrgIndex=" + yDimOrgIndex + ", xDimOrgIndex=" + xDimOrgIndex + '}';
  }
}
