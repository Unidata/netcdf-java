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
import ucar.nc2.AttributeContainer;
import ucar.nc2.AttributeContainerMutable;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.Variable;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDataset.Enhance;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dataset.VariableEnhanced;
import ucar.nc2.geoloc.vertical.VerticalTransform;
import ucar.nc2.geoloc.vertical.VerticalTransformFactory;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxisDependenceType;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridDataset;

import javax.annotation.concurrent.Immutable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** GridDataset implementation wrapping a NetcdfDataset. */
@Immutable
public class GridNetcdfDataset implements GridDataset {
  private static final Logger log = LoggerFactory.getLogger(GridNetcdfDataset.class);

  public static Optional<GridNetcdfDataset> create(NetcdfDataset ncd, Formatter errInfo) throws IOException {
    Set<Enhance> enhance = ncd.getEnhanceMode();
    if (enhance == null || !enhance.contains(Enhance.CoordSystems)) {
      enhance = NetcdfDataset.getDefaultEnhanceMode();
      ncd = NetcdfDatasets.enhance(ncd, enhance, null);
    }

    DatasetClassifier classifier = new DatasetClassifier(ncd, errInfo);
    if (classifier.getFeatureType() == FeatureType.GRID || classifier.getFeatureType() == FeatureType.CURVILINEAR) {
      return createGridDataset(ncd, classifier, errInfo);
    } else {
      return Optional.empty();
    }
  }

  private static Optional<GridNetcdfDataset> createGridDataset(NetcdfDataset ncd, DatasetClassifier classifier,
      Formatter errInfo) throws IOException {
    FeatureType featureType = classifier.getFeatureType();

    Map<String, GridAxis<?>> gridAxes = new HashMap<>();
    ArrayList<GridCoordinateSystem> coordsys = new ArrayList<>();
    Multimap<GridCoordinateSystem, Grid> gridsets = ArrayListMultimap.create();

    // Do all the independent axes first
    for (CoordinateAxis axis : classifier.getIndependentAxes()) {
      if (axis.getFullName().startsWith("Best/")) {
        continue;
      }
      if (axis.getRank() < 2) {
        GridAxis<?> gridAxis = CoordAxisToGridAxis
            .create(axis, GridAxisDependenceType.independent, ncd.isIndependentCoordinate(axis)).extractGridAxis();
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
        GridAxis<?> gridAxis = CoordAxisToGridAxis
            .create(axis, GridAxisDependenceType.dependent, ncd.isIndependentCoordinate(axis)).extractGridAxis();
        gridAxes.put(axis.getFullName(), gridAxis);
      }
    }

    // vertical transforms
    VerticalTransformFinder finder = new VerticalTransformFinder(ncd, errInfo);
    Set<TrackVerticalTransform> verticalTransforms = finder.findVerticalTransforms();

    // Convert CoordinateSystem to GridCoordinateSystem
    Set<String> alreadyDone = new HashSet<>();
    Map<String, TrackGridCS> trackCsConverted = new HashMap<>();
    for (DatasetClassifier.CoordSysClassifier csc : classifier.getCoordinateSystemsUsed()) {
      if (csc.getName().startsWith("Best/")) {
        continue;
      }
      GridNetcdfCSBuilder.createFromClassifier(csc, gridAxes, verticalTransforms, errInfo).ifPresent(gcs -> {
        coordsys.add(gcs);
        trackCsConverted.put(csc.getName(), new TrackGridCS(csc, gcs));
      });
    }

    // Largest Coordinate Systems come first
    coordsys.sort((o1, o2) -> o2.getGridAxes().size() - o1.getGridAxes().size());

    // Assign coordsys to grids
    for (Variable v : ncd.getVariables()) {
      if (v.getFullName().startsWith("Best/")) { // TODO remove Best from grib generation code
        continue;
      }
      if (alreadyDone.contains(v.getFullName())) {
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
        GridCoordinateSystem gcs = track.gridCS;
        Set<Dimension> domain = Dimensions.makeDomain(track.csc.getAxesUsed(), false);
        if (gcs != null && gcs.getFeatureType() == featureType && Dimensions.isCoordinateSystemFor(domain, v)) {
          Grid grid = new GridVariable(gcs, (VariableDS) ve);
          gridsets.put(gcs, grid);
          alreadyDone.add(v.getFullName());
          break;
        }
      }
    }

    if (gridsets.isEmpty()) {
      errInfo.format("gridsets is empty%n");
      return Optional.empty();
    }

    HashSet<Grid> ugrids = new HashSet<>(gridsets.values());
    ArrayList<Grid> grids = new ArrayList<>(ugrids);
    grids.sort((g1, g2) -> CharSequence.compare(g1.getName(), g2.getName()));
    return Optional.of(new GridNetcdfDataset(ncd, featureType, coordsys, gridAxes.values(), grids));
  }

  private static class VerticalTransformFinder {
    final NetcdfDataset ncd;
    final Formatter errlog;
    final Set<TrackVerticalTransform> result;

    VerticalTransformFinder(NetcdfDataset ncd, Formatter errlog) {
      this.ncd = ncd;
      this.errlog = errlog;
      this.result = new HashSet<>();
    }

    Set<TrackVerticalTransform> findVerticalTransforms() {
      for (Variable v : ncd.getVariables()) {
        Optional<String> transformNameOpt = VerticalTransformFactory.hasVerticalTransformFor(v.attributes());
        if (transformNameOpt.isPresent()) {
          String transformName = transformNameOpt.get();
          // A ctv that is also an axis.
          makeVerticalTransforms(transformName, v.getFullName(), v.attributes());
          // A ctv that has a _CoordinateAxes attribute pointing to an axis.
          String axesNames = v.attributes().findAttributeString(_Coordinate.Axes, null);
          if (axesNames != null) {
            makeVerticalTransforms(transformName, axesNames, v.attributes());
          }
        }

      }
      return result;
    }

    private void makeVerticalTransforms(String transform_name, String axisName, AttributeContainer attributes) {
      for (CoordinateSystem csys : ncd.getCoordinateSystems()) {
        if (csys.containsAxis(axisName)) {
          Optional<VerticalTransform> vto =
              VerticalTransformFactory.makeVerticalTransform(transform_name, ncd, csys, attributes, errlog);
          vto.ifPresent(vt -> result.add(new TrackVerticalTransform(axisName, vt, csys)));
        }
      }
    }
  }

  private static class TrackGridCS {
    final DatasetClassifier.CoordSysClassifier csc;
    final GridCoordinateSystem gridCS;

    public TrackGridCS(DatasetClassifier.CoordSysClassifier csc, GridCoordinateSystem gridCS) {
      this.csc = csc;
      this.gridCS = gridCS;
    }
  }

  static class TrackVerticalTransform {
    final String axisName;
    final VerticalTransform vertTransform;
    final CoordinateSystem csys;

    public TrackVerticalTransform(String axisName, VerticalTransform vertTransform, CoordinateSystem csys) {
      this.axisName = axisName;
      this.vertTransform = vertTransform;
      this.csys = csys;
    }

    boolean equals(String name, CoordinateSystem csys) {
      return this.axisName.equals(name) && this.csys.equals(csys);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      TrackVerticalTransform that = (TrackVerticalTransform) o;
      return vertTransform.getName().equals(that.vertTransform.getName()) && csys.equals(that.csys);
    }

    @Override
    public int hashCode() {
      return Objects.hash(vertTransform.getName(), csys);
    }
  }

  ///////////////////////////////////////////////////////////////////
  private final NetcdfDataset ncd;
  private final FeatureType featureType;

  private final ImmutableList<GridCoordinateSystem> coordsys;

  private final ImmutableList<GridAxis<?>> gridAxes;
  private final ImmutableList<Grid> grids;

  public GridNetcdfDataset(NetcdfDataset ncd, FeatureType featureType, List<GridCoordinateSystem> coordsys,
      Collection<GridAxis<?>> gridAxes, Collection<Grid> grids) {
    this.ncd = ncd;
    this.featureType = featureType;
    this.coordsys = ImmutableList.copyOf(coordsys);
    this.gridAxes = ImmutableList.copyOf(gridAxes);
    this.grids = ImmutableList.copyOf(grids);
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
    return AttributeContainerMutable.copyFrom(ncd.getRootGroup().attributes()).setName(getName()).toImmutable();
  }

  @Override
  public List<GridCoordinateSystem> getGridCoordinateSystems() {
    return ImmutableList.copyOf(coordsys);
  }

  @Override
  public List<GridAxis<?>> getGridAxes() {
    return gridAxes;
  }

  @Override
  public List<Grid> getGrids() {
    return ImmutableList.copyOf(grids);
  }

  @Override
  public FeatureType getFeatureType() {
    return featureType;
  }

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
