package ucar.nc2.grid;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** Manages Projection or Lat/Lon CoordinateSystem, assumed to be seperable; use of indices ok. */
public interface GridHorizCoordinateSystem {

  /** Get the 1D X axis (either GeoX or Lon); null if LatLon2D */
  @Nullable
  GridAxis1D getXHorizAxis();

  /** Get the 1D Y axis (either GeoY or Lat); null if LatLon2D */
  @Nullable
  GridAxis1D getYHorizAxis();

  /** True if both X and Y axes are 1 dimensional and are regularly spaced. */
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

  /** Horizontal staggering (currently based on WRF). */
  String getHorizStaggerType();

  /** Return value from findXYindexFromCoord(). */
  class CoordReturn {
    /** The data index */
    public int xindex, yindex;
    /** The x,y grid coordinate. */
    public double xcoord, ycoord;

    @Override
    public String toString() {
      return "CoordReturn{" + "xindex=" + xindex + ", yindex=" + yindex + ", xcoord=" + xcoord + ", ycoord=" + ycoord
          + '}';
    }
  }

  /** From the (x,y) projection point, find the indices and coordinates of the horizontal 2D grid. */
  Optional<CoordReturn> findXYindexFromCoord(double xpt, double ypt);

  /** Subset both x and y axis based on the given parameters. */
  List<GridAxis> subset(GridSubset params, Formatter errlog);

}
