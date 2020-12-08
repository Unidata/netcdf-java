/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.grid.*;
import ucar.nc2.internal.dataset.DatasetClassifier;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import java.io.IOException;
import java.util.*;

/** GridDataset implementation wrapping a NetcdfDataset. */
public class GridNetcdfDataset implements GridDataset {
  private static Logger log = LoggerFactory.getLogger(GridNetcdfDataset.class);

  public static Optional<GridNetcdfDataset> create(NetcdfDataset ncd, Formatter errInfo) throws IOException {
    Set<Enhance> enhance = ncd.getEnhanceMode();
    if (enhance == null || !enhance.contains(Enhance.CoordSystems)) {
      enhance = NetcdfDataset.getDefaultEnhanceMode();
      ncd = NetcdfDatasets.enhance(ncd, enhance, null);
    }

    DatasetClassifier facc = new DatasetClassifier(ncd, errInfo);
    return (facc.getFeatureType() == FeatureType.GRID) ? Optional.of(new GridNetcdfDataset(ncd, facc, errInfo))
        : Optional.empty();
  }

  ///////////////////////////////////////////////////////////////////
  // TODO make Immutable

  private final NetcdfDataset ncd;
  private final FeatureType featureType;

  private final ArrayList<GridCS> coordsys = new ArrayList<>();

  private final Map<String, GridAxis> gridAxes;
  private final ArrayList<Grid> grids = new ArrayList<>();
  private final Multimap<GridCS, Grid> gridsets;

  private GridNetcdfDataset(NetcdfDataset ncd, DatasetClassifier classifier, Formatter errInfo) {
    this.ncd = ncd;
    this.featureType = classifier.getFeatureType();

    this.gridAxes = new HashMap<>();

    // Do all the independent axes first
    for (CoordinateAxis axis : classifier.getIndependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis gridAxis = Grids.extractGridAxis1D(ncd, axis, GridAxis.DependenceType.independent);
        gridAxes.put(axis.getFullName(), gridAxis);
      } else {
        log.warn("Independent gridAxis {} rank > 1", axis.getFullName());
        errInfo.format("Independent gridAxis %s rank > 1", axis.getFullName());
      }
    }

    // Now we can do dependent, knowing we have all the independent ones in gridAxes
    for (CoordinateAxis axis : classifier.getDependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis gridAxis = Grids.extractGridAxis1D(ncd, axis, GridAxis.DependenceType.dependent);
        gridAxes.put(axis.getFullName(), gridAxis);
      } else if (axis.getAxisType() == AxisType.TimeOffset && axis.getRank() == 2) {
        GridAxis gridAxis = Grids.extractGridAxisOffset2D(axis, GridAxis.DependenceType.dependent, gridAxes);
        gridAxes.put(axis.getFullName(), gridAxis);
      }
    }

    // Convert coordsys
    Map<String, TrackGridCS> trackCsConverted = new HashMap<>();
    for (DatasetClassifier.CoordSysClassifier csc : classifier.getCoordinateSystemsUsed()) {
      if (csc.getName().startsWith("Best/")) {
        continue;
      }
      GridCS gcs = new GridCS(csc, this.gridAxes);
      coordsys.add(gcs);
      trackCsConverted.put(csc.getName(), new TrackGridCS(csc, gcs));
    }
    // Largest Coordinate Systems come first
    coordsys.sort((o1, o2) -> o2.getGridAxes().size() - o1.getGridAxes().size());

    // Assign coordsys to grids
    this.gridsets = ArrayListMultimap.create();
    for (Variable v : ncd.getVariables()) {
      if (v.getFullName().startsWith("Best/")) { // TODO remove Best from grib generation code
        continue;
      }
      VariableEnhanced ve = (VariableEnhanced) v;
      List<CoordinateSystem> css = new ArrayList<>(ve.getCoordinateSystems());
      if (css.isEmpty()) {
        continue;
      }
      // Use the largest (# axes) coordsys
      css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());
      for (CoordinateSystem cs : css) {
        TrackGridCS track = trackCsConverted.get(cs.getName());
        if (track == null) {
          continue; // not used
        }
        GridCS gcs = track.gridCS;
        Set<Dimension> domain = Dimensions.makeDomain(track.csc.getAxesUsed(), false);
        if (gcs != null && gcs.getFeatureType() == this.featureType && Dimensions.isCoordinateSystemFor(domain, v)) {
          Grid grid = new GridVariable(gcs, (VariableDS) ve);
          grids.add(grid);
          this.gridsets.put(gcs, grid);
          break;
        }
      }
    }
  }

  private class TrackGridCS {
    DatasetClassifier.CoordSysClassifier csc;
    GridCS gridCS;

    public TrackGridCS(DatasetClassifier.CoordSysClassifier csc, GridCS gridCS) {
      this.csc = csc;
      this.gridCS = gridCS;
    }
  }

  private void makeHorizRanges() {
    LatLonRect.Builder llbbBuilder = null;

    ProjectionRect.Builder projBBbuilder = null;
    for (GridCoordinateSystem gcs : this.gridsets.keySet()) {
      ProjectionRect bb = gcs.getHorizCoordSystem().getBoundingBox();
      if (projBBbuilder == null)
        projBBbuilder = bb.toBuilder();
      else if (bb != null)
        projBBbuilder.add(bb);

      LatLonRect llbb = gcs.getHorizCoordSystem().getLatLonBoundingBox();
      if (llbbBuilder == null)
        llbbBuilder = llbb.toBuilder();
      else if (llbb != null)
        llbbBuilder.extend(llbb);
    }

    if (llbbBuilder != null) {
      LatLonRect llbbMax = llbbBuilder.build();
    }
  }

  public FeatureType getCoverageType() {
    return featureType;
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
  public AttributeContainer attributes() {
    return ncd.getRootGroup().attributes();
  }

  @Override
  public ImmutableList<GridCoordinateSystem> getGridCoordinateSystems() {
    return ImmutableList.copyOf(coordsys);
  }

  @Override
  public ImmutableList<GridAxis> getGridAxes() {
    return ImmutableList.copyOf(gridAxes.values());
  }

  @Override
  public ImmutableList<Grid> getGrids() {
    return ImmutableList.copyOf(grids);
  }

  @Override
  public Optional<Grid> findGrid(String name) {
    return grids.stream().filter(g -> g.getName().equals(name)).findFirst();
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  };

  @Override
  public String toString() {
    Formatter f = new Formatter();
    toString(f);
    return f.toString();
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
