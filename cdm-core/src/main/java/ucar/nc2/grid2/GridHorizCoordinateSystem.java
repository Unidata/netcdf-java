package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import ucar.nc2.grid.GridSubset;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Manages Projection GeoX/GeoY or Lat/Lon CoordinateSystem. */
public interface GridHorizCoordinateSystem {

  /** Get the 1D X axis (either GeoX or Lon). */
  GridAxisPoint getXHorizAxis();

  /** Get the 1D Y axis (either GeoY or Lat). */
  GridAxisPoint getYHorizAxis();

  default List<Integer> getShape() {
    return ImmutableList.of(getYHorizAxis().getNominalSize(), getXHorizAxis().getNominalSize());
  }

  default List<ucar.array.Range> getSubsetRanges() {
    return ImmutableList.of(getYHorizAxis().getSubsetRange(), getXHorizAxis().getSubsetRange());
  }

  /** True if both X and Y axes are regularly spaced. */
  boolean isRegular();

  /** Does this use lat/lon horizontal axes? */
  boolean isLatLon();

  /** Is this a global coverage over longitude ? */
  boolean isGlobalLon();

  /** Get horizontal bounding box in lat, lon coordinates. For projection, only an approximation based on corners. */
  ucar.unidata.geoloc.LatLonRect getLatLonBoundingBox();

  /** Get horizontal bounding box in projection coordinates. */
  ucar.unidata.geoloc.ProjectionRect getBoundingBox();

  /** Get the horizontal Projection. */
  ucar.unidata.geoloc.Projection getProjection();

  /**
   * Get the horizontal coordinate units, null for latlon. Needed to convert projection units.
   * TODO add to projection directly ??
   */
  @Nullable
  String getGeoUnits();

  /** Horizontal staggering (currently based on WRF). LOOK needed? */
  String getHorizStaggerType();

  /** Return value from findXYindexFromCoord(). */
  class CoordReturn {
    /** The data index */
    public int xindex, yindex;
    /** The x,y grid coordinate. */
    public double xcoord, ycoord;

    @Override
    public String toString() {
      return String.format("CoordReturn{xindex=%d, yindex=%d, xcoord=%f, ycoord=%f", xindex, yindex, xcoord, ycoord);
    }
  }

  /** From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid. */
  Optional<CoordReturn> findXYindexFromCoord(double xpt, double ypt);

  /** Subset both x and y axis based on the given parameters. */
  Optional<GridHorizCoordinateSystem> subset(GridSubset params, Formatter errlog);

}
