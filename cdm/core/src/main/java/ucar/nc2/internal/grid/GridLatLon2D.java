package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Range;
import ucar.nc2.grid.GridAxis2D;
import ucar.nc2.util.Misc;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.sat.Geostationary;
import ucar.unidata.geoloc.projection.sat.MSGnavigation;
import ucar.unidata.geoloc.projection.sat.VerticalPerspectiveView;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

/** A GridHorizCoordinateSystem with 2d lat/lon axes. */
public class GridLatLon2D extends GridHorizCS {
  private final GridAxis2D lon2D;
  private final GridAxis2D lat2D;

  GridLatLon2D(GridAxis2D xaxis, GridAxis2D yaxis, @Nullable Projection projection, String horizStaggerType) {
    super(null, null, projection, horizStaggerType);
    Preconditions.checkArgument(Misc.compare(xaxis.getShape(), yaxis.getShape(), new Formatter()));
    this.lon2D = xaxis;
    this.lat2D = yaxis;
  }

  public int[] getShape() {
    return lon2D.getShape();
  }

  public GridAxis2D getLatAxis() {
    return lat2D;
  }

  public GridAxis2D getLonAxis() {
    return lon2D;
  }

  @Override
  public boolean isLatLon() {
    return true;
  }

  @Override
  public boolean isRegular() {
    return false;
  }

  @Override
  @Nullable
  public String getGeoUnits() {
    return null;
  }

  @Override
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {
      mapArea = new ProjectionRect(lon2D.getCoordMin(), lat2D.getCoordMin(), lon2D.getCoordMax(), lat2D.getCoordMax());
    }
    return mapArea;
  }

  @Override
  public LatLonPoint getLatLon(int xindex, int yindex) {
    double x = lon2D.getCoordValue(yindex, xindex);
    double y = lat2D.getCoordValue(yindex, xindex);
    return LatLonPoint.create(y, x);
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    return Optional.empty();
  }

  @Override
  public List<Range> getRangesFromLatLonRect(LatLonRect rect) throws InvalidRangeException {
    double minx, maxx, miny, maxy;

    Projection proj = getProjection();
    if (proj != null && !(proj instanceof VerticalPerspectiveView) && !(proj instanceof MSGnavigation)
        && !(proj instanceof Geostationary)) { // LOOK kludge - how to do this generrally ??
      // first clip the request rectangle to the bounding box of the grid
      LatLonRect bb = getLatLonBoundingBox();
      LatLonRect rect2 = bb.intersect(rect);
      if (null == rect2)
        throw new InvalidRangeException("Request Bounding box does not intersect Grid ");
      rect = rect2;
    }

    LatLonPoint llpt = rect.getLowerLeftPoint();
    LatLonPoint urpt = rect.getUpperRightPoint();
    LatLonPoint lrpt = rect.getLowerRightPoint();
    LatLonPoint ulpt = rect.getUpperLeftPoint();

    minx = getMinOrMaxLon(llpt.getLongitude(), ulpt.getLongitude(), true);
    miny = Math.min(llpt.getLatitude(), lrpt.getLatitude());
    maxx = getMinOrMaxLon(urpt.getLongitude(), lrpt.getLongitude(), false);
    maxy = Math.min(ulpt.getLatitude(), urpt.getLatitude());

    // normalize to [minLon,minLon+360]
    double minLon = lon2D.getCoordMin();
    minx = LatLonPoints.lonNormalFrom(minx, minLon);
    maxx = LatLonPoints.lonNormalFrom(maxx, minLon);

    int[] shape = new int[] {0, 0}; // TODO
    int nj = shape[0];
    int ni = shape[1];

    int mini = Integer.MAX_VALUE, minj = Integer.MAX_VALUE;
    int maxi = -1, maxj = -1;

    // margolis 2/18/2010
    // minx = LatLonPointImpl.lonNormal( minx ); // <-- THIS IS NEW
    // maxx = LatLonPointImpl.lonNormal( maxx ); // <-- THIS IS NEW

    // brute force, examine every point LOOK BAD
    for (int j = 0; j < nj; j++) {
      for (int i = 0; i < ni; i++) {
        double lat = lat2D.getCoordValue(j, i);
        double lon = lon2D.getCoordValue(j, i);
        // lon = LatLonPointImpl.lonNormal( lon ); // <-- THIS IS NEW

        if ((lat >= miny) && (lat <= maxy) && (lon >= minx) && (lon <= maxx)) {
          if (i > maxi)
            maxi = i;
          if (i < mini)
            mini = i;
          if (j > maxj)
            maxj = j;
          if (j < minj)
            minj = j;
        }
      }
    }

    // this is the case where no points are included
    if ((mini > maxi) || (minj > maxj)) {
      mini = 0;
      minj = 0;
      maxi = -1;
      maxj = -1;
    }

    ArrayList<Range> list = new ArrayList<>();
    list.add(new Range(minj, maxj));
    list.add(new Range(mini, maxi));
    return list;
  }

}
