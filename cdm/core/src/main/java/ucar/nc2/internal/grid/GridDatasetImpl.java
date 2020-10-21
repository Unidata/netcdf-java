/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import ucar.nc2.*;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.time.CalendarDateRange;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.*;

/** GridDataset implementation wrapping a NetcdfDataset. */
public class GridDatasetImpl implements GridDataset {

  public static Optional<GridDatasetImpl> create(NetcdfDataset ncd, Formatter errInfo) throws IOException {
    Set<Enhance> enhance = ncd.getEnhanceMode();
    if (enhance == null || !enhance.contains(Enhance.CoordSystems)) {
      enhance = NetcdfDataset.getDefaultEnhanceMode();
      ncd = NetcdfDatasets.enhance(ncd, enhance, null);
    }

    GridCoordSystemBuilder facc = GridCoordSystemBuilder.classify(ncd, errInfo);
    return (facc == null) ? Optional.empty() : Optional.of(new GridDatasetImpl(ncd, facc, errInfo));
  }

  ///////////////////////////////////////////////////////////////////

  private final NetcdfDataset ncd;
  private final FeatureType coverageType;

  private final ArrayList<GridCoordinateSystemImpl> coordsys = new ArrayList<>();

  private final Map<String, GridAxis> gridAxes;
  private final ArrayList<Grid> grids = new ArrayList<>();
  private final Multimap<GridCoordinateSystem, Grid> gridsets;

  private LatLonRect llbbMax;
  private CalendarDateRange dateRangeMax;
  private ProjectionRect projBB;

  private GridDatasetImpl(NetcdfDataset ncd, GridCoordSystemBuilder facc, Formatter errInfo) throws IOException {
    this.ncd = ncd;
    this.coverageType = facc.type;

    // Convert axes
    this.gridAxes = new HashMap<>();
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      if (axis.getAxisType() == null) {
        continue;
      }
      if (axis.getAxisType().isTime()) {
        gridAxes.put(axis.getFullName(), Grids.makeGridAxis1DTime(axis));
      } else {
        gridAxes.put(axis.getFullName(), Grids.extractGridAxis1D(axis));
      }
    }

    // Convert coordsys
    Map<CoordinateSystem, GridCoordinateSystemImpl> trackCsConverted = new HashMap<>();
    for (CoordinateSystem cs : ncd.getCoordinateSystems()) {
      GridCoordSystemBuilder fac = new GridCoordSystemBuilder(ncd, cs, errInfo);
      if (fac.type == null) {
        continue;
      }
      GridCoordinateSystemImpl gcs = fac.build(this.gridAxes);
      if (gcs == null) {
        continue;
      }
      coordsys.add(gcs);
      trackCsConverted.put(cs, gcs);
    }

    this.gridsets = ArrayListMultimap.create();
    for (Variable v : ncd.getVariables()) {
      VariableEnhanced ve = (VariableEnhanced) v;
      List<CoordinateSystem> css = new ArrayList<>(ve.getCoordinateSystems());
      if (css.isEmpty()) {
        continue;
      }
      css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
      CoordinateSystem cs = css.get(0); // the largest one
      GridCoordinateSystemImpl gcs = Preconditions.checkNotNull(trackCsConverted.get(cs));
      Grid grid = new GridVariable(gcs, (VariableDS) ve);
      grids.add(grid);
      this.gridsets.put(gcs, grid);
    }
  }

  private void makeHorizRanges() {
    LatLonRect.Builder llbbBuilder = null;

    ProjectionRect.Builder projBBbuilder = null;
    for (GridCoordinateSystem gcs : this.gridsets.keys()) {
      ProjectionRect bb = gcs.getBoundingBox();
      if (projBBbuilder == null)
        projBBbuilder = bb.toBuilder();
      else if (bb != null)
        projBBbuilder.add(bb);

      LatLonRect llbb = gcs.getLatLonBoundingBox();
      if (llbbBuilder == null)
        llbbBuilder = llbb.toBuilder();
      else if (llbb != null)
        llbbBuilder.extend(llbb);
    }

    if (llbbBuilder != null) {
      llbbMax = llbbBuilder.build();
    }
  }

  public FeatureType getCoverageType() {
    return coverageType;
  }

  @Override
  public String getName() {
    String loc = ncd.getLocation();
    int pos = loc.lastIndexOf('/');
    if (pos < 0)
      pos = loc.lastIndexOf('\\');
    return (pos < 0) ? loc : loc.substring(pos + 1);
  }

  @Override
  public String getLocation() {
    return ncd.getLocation();
  }

  @Override
  public Iterable<GridCoordinateSystem> getCoordSystems() {
    return gridsets.keys();
  }

  @Override
  public Iterable<GridAxis> getCoordAxes() {
    return gridAxes.values(); // LOOK sort ?
  }

  @Override
  public Iterable<Grid> getGrids() {
    return grids;
  }

  @Override
  public Grid findGrid(String name) {
    return null;
  }

  public String getDetailInfo() {
    Formatter buff = new Formatter();
    getDetailInfo(buff);
    return buff.toString();
  }

  public void getDetailInfo(Formatter buff) {
    toString(buff);
    buff.format("%n%n----------------------------------------------------%n");
    buff.format("%s", ncd.toString());
    buff.format("%n%n----------------------------------------------------%n");
  }

  @Override
  public void toString(Formatter buf) {
    int countGridset = 0;

    for (GridCoordinateSystem gcs : gridsets.keys()) {
      buf.format("%nGridset %d  coordSys=%s", countGridset, gcs);
      buf.format(" LLbb=%s ", gcs.getLatLonBoundingBox());
      if ((gcs.getProjection() != null) && !gcs.getProjection().isLatLon())
        buf.format(" bb= %s", gcs.getBoundingBox());
      buf.format("%n");
      buf.format("Name__________________________Unit__________________________hasMissing_Description%n");
      for (Grid grid : gridsets.get(gcs)) {
        buf.format("%s%n", grid);
      }
      countGridset++;
      buf.format("%n");
    }

    buf.format("%nGeoReferencing Coordinate Axes%n");
    buf.format("Name__________________________Units_______________Type______Description%n");
    for (CoordinateAxis axis : ncd.getCoordinateAxes()) {
      if (axis.getAxisType() == null)
        continue;
      axis.getInfo(buf);
      buf.format("%n");
    }
  }

  private boolean wasClosed = false;

  @Override
  public synchronized void close() throws IOException {
    try {
      if (!wasClosed)
        ncd.close();
    } finally {
      wasClosed = true;
    }
  }
}
