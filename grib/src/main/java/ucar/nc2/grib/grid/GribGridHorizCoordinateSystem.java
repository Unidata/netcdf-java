/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.grid;

import ucar.nc2.constants.AxisType;
import ucar.nc2.grib.GdsHorizCoordSys;
import ucar.nc2.grid2.GridAxis;
import ucar.nc2.grid2.GridAxisPoint;
import ucar.nc2.grid2.GridHorizCoordinateSystem;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionRect;

import javax.annotation.Nullable;
import java.util.Formatter;
import java.util.List;
import java.util.Optional;

public class GribGridHorizCoordinateSystem implements GridHorizCoordinateSystem {
  private final GdsHorizCoordSys hcs;
  private final GridAxisPoint xaxis;
  private final GridAxisPoint yaxis;

  public GribGridHorizCoordinateSystem(GdsHorizCoordSys hcs) {
    this.hcs = hcs;
    if (hcs.isLatLon()) {
      this.xaxis = GridAxisPoint.builder().setName("lonaxis").setAxisType(AxisType.Lon)
          .setRegular(hcs.nx, hcs.startx, hcs.dx).build();
      this.yaxis = GridAxisPoint.builder().setName("lataxis").setAxisType(AxisType.Lat)
          .setRegular(hcs.ny, hcs.starty, hcs.dy).build();
    } else {
      this.xaxis = GridAxisPoint.builder().setName("xaxis").setAxisType(AxisType.GeoX)
          .setRegular(hcs.nx, hcs.startx, hcs.dx).build();
      this.yaxis = GridAxisPoint.builder().setName("yaxis").setAxisType(AxisType.GeoY)
          .setRegular(hcs.ny, hcs.starty, hcs.dy).build();
    }
  }

  @Override
  public GridAxisPoint getXHorizAxis() {
    return xaxis;
  }

  @Override
  public GridAxisPoint getYHorizAxis() {
    return yaxis;
  }

  @Override
  public boolean isRegular() {
    return true;
  }

  @Override
  public boolean isLatLon() {
    return hcs.isLatLon();
  }

  @Override
  public Projection getProjection() {
    return hcs.proj;
  }

  // LOOK not done below

  @Override
  public boolean isGlobalLon() {
    return false;
  }

  @Override
  public LatLonRect getLatLonBoundingBox() {
    return null;
  }

  @Override
  public ProjectionRect getBoundingBox() {
    return null;
  }

  @Nullable
  @Override
  public String getGeoUnits() {
    return null;
  }

  @Override
  public String getHorizStaggerType() {
    return null;
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double xpt, double ypt) {
    return Optional.empty();
  }

  @Override
  public GribGridHorizCoordinateSystem subset(GridSubset params, Formatter errlog) {
    return null;
  }
}
