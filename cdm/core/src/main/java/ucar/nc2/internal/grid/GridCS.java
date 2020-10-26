/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ucar.ma2.RangeIterator;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.grid.GridAxis;
import ucar.nc2.grid.GridAxis1D;
import ucar.nc2.grid.GridAxis1DTime;
import ucar.nc2.grid.GridCoordinateSystem;
import ucar.nc2.grid.GridSubset;
import ucar.nc2.internal.dataset.DatasetClassifier;
import ucar.unidata.geoloc.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base implementation of GridCoordinateSystem.
 * TODO its possible this is general, could be in API instead of interface.
 */
@Immutable
class GridCS implements GridCoordinateSystem {

  @Override
  public String getName() {
    return name;
  }

  // needed?
  public FeatureType getFeatureType() {
    return featureType;
  }

  @Override
  public ImmutableList<GridAxis> getGridAxes() {
    return this.axes;
  }

  public GridAxis findCoordAxis(String shortName) {
    for (GridAxis axis : axes) {
      if (axis.getName().equals(shortName))
        return axis;
    }
    return null;
  }

  // search in order given
  public GridAxis findCoordAxis(AxisType... axisType) {
    for (AxisType type : axisType) {
      for (GridAxis axis : axes) {
        if (axis.getAxisType() == type)
          return axis;
      }
    }
    return null;
  }

  @Override
  public ImmutableList<CoordinateTransform> getCoordTransforms() {
    return transforms;
  }

  @Override
  public GridAxis1D getXHorizAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.GeoX, AxisType.Lon);
  }

  @Override
  public GridAxis1D getYHorizAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.GeoY, AxisType.Lat);
  }

  @Override
  @Nullable
  public GridAxis1D getVerticalAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.GeoZ, AxisType.Height, AxisType.Pressure);
  }

  @Override
  @Nullable
  public GridAxis1DTime getTimeAxis() {
    return (GridAxis1DTime) findCoordAxis(AxisType.Time);
  }

  @Override
  @Nullable
  public GridAxis1DTime getRunTimeAxis() {
    return (GridAxis1DTime) findCoordAxis(AxisType.RunTime);
  }

  @Override
  @Nullable
  public GridAxis1DTime getTimeOffsetAxis() {
    return (GridAxis1DTime) findCoordAxis(AxisType.TimeOffset);
  }

  @Override
  @Nullable
  public GridAxis1D getEnsembleAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.Ensemble);
  }

  @Override
  @Nullable
  public Projection getProjection() {
    return projection;
  }

  @Override
  public boolean isLatLon() {
    return isLatLon;
  }

  @Override
  public boolean isGlobalLon() {
    if (!isLatLon)
      return false;
    GridAxis1D lon = getXHorizAxis();
    double first = lon.getCoordEdgeFirst();
    double last = lon.getCoordEdgeLast();
    double min = Math.min(first, last);
    double max = Math.max(first, last);
    return (max - min) >= 360;
  }

  @Override
  public boolean isRegularSpatial() {
    if (!isRegularSpatial(getXHorizAxis()))
      return false;
    return isRegularSpatial(getYHorizAxis());
  }

  @Override
  public Optional<CoordReturn> findXYindexFromCoord(double x, double y) {
    return Optional.empty();
  }

  private boolean isRegularSpatial(GridAxis1D axis) {
    if (axis == null)
      return true;
    return axis.isRegular();
  }

  private String horizStaggerType;

  public String getHorizStaggerType() {
    return horizStaggerType;
  }

  public void setHorizStaggerType(String horizStaggerType) {
    this.horizStaggerType = horizStaggerType;
  }

  private ProjectionRect mapArea;

  @Override
  public ProjectionRect getBoundingBox() {
    if (mapArea == null) {
      GridAxis1D xaxis1 = getXHorizAxis();
      GridAxis1D yaxis1 = getYHorizAxis();

      mapArea = new ProjectionRect(xaxis1.getCoordEdgeFirst(), yaxis1.getCoordEdgeFirst(), xaxis1.getCoordEdgeLast(),
          yaxis1.getCoordEdgeLast());
    }
    return mapArea;
  }

  /*
   * Get the Lat/Lon coordinates of the midpoint of a grid cell, using the x,y indices
   *
   * @param xindex x index
   * 
   * @param yindex y index
   * 
   * @return lat/lon coordinate of the midpoint of the cell
   *
   * public LatLonPoint getLatLon(int xindex, int yindex) {
   * double x, y;
   *
   * GridAxis1D horizXaxis = getXHorizAxis();
   * GridAxis1D horizYaxis = getYHorizAxis();
   * if (horizXaxis instanceof CoordinateAxis1D) {
   * CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizXaxis;
   * x = horiz1D.getCoordValue(xindex);
   * } else {
   * CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizXaxis;
   * x = horiz2D.getCoordValue(yindex, xindex);
   * }
   *
   * if (horizYaxis instanceof CoordinateAxis1D) {
   * CoordinateAxis1D horiz1D = (CoordinateAxis1D) horizYaxis;
   * y = horiz1D.getCoordValue(yindex);
   * } else {
   * CoordinateAxis2D horiz2D = (CoordinateAxis2D) horizYaxis;
   * y = horiz2D.getCoordValue(yindex, xindex);
   * }
   *
   * return isLatLon() ? LatLonPoint.create(y, x) : getLatLon(x, y);
   * }
   *
   * public LatLonPoint getLatLon(double xcoord, double ycoord) {
   * Projection dataProjection = getProjection();
   * return dataProjection.projToLatLon(ProjectionPoint.create(xcoord, ycoord));
   * }
   */

  private LatLonRect llbb;

  @Override
  public LatLonRect getLatLonBoundingBox() {

    if (llbb == null) {
      GridAxis1D horizXaxis = getXHorizAxis();
      GridAxis1D horizYaxis = getYHorizAxis();
      if (isLatLon()) {
        double startLat = horizYaxis.getCoordEdgeFirst();
        double startLon = horizXaxis.getCoordEdgeFirst();

        double deltaLat = horizYaxis.getCoordEdgeLast() - startLat;
        double deltaLon = horizXaxis.getCoordEdgeLast() - startLon;

        LatLonPoint llpt = LatLonPoint.create(startLat, startLon);
        llbb = new LatLonRect(llpt, deltaLat, deltaLon);

      } else {
        Projection dataProjection = getProjection();
        ProjectionRect bb = getBoundingBox();
        if (dataProjection != null && bb != null) {
          llbb = dataProjection.projToLatLonBB(bb);
        }
      }
    }
    return llbb;
  }

  @Override
  public String toString() {
    Formatter buff = new Formatter();
    show(buff, false);
    return buff.toString();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (projection != null) {
      f.format(" Projection: %s %s%n", projection.getName(), projection.paramsToString());
    }

    f.format(" LLbb=%s%n", getLatLonBoundingBox());
    if ((getProjection() != null) && !getProjection().isLatLon()) {
      f.format(" bb= %s%n", getBoundingBox());
    }
  }

  private void showCoordinateAxis(GridAxis1D axis, Formatter f, boolean showCoords) {
    if (axis == null)
      return;
    f.format(" rt=%s (%s)", axis.getName(), axis.getClass().getName());
    if (showCoords)
      showCoords(axis, f);
    f.format("%n");
  }

  private void showCoords(GridAxis axis, Formatter f) {
    if (axis instanceof GridAxis1D && axis.getDataType().isNumeric()) {
      GridAxis1D axis1D = (GridAxis1D) axis;
      if (!axis1D.isInterval()) {
        for (double anE : axis1D.getCoordsAsArray()) {
          f.format("%f,", anE);
        }
      } else {
        for (int i = 0; i < axis1D.getNcoords(); i++) {
          f.format("(%f,%f) = %f%n", axis1D.getCoordEdge1(i), axis1D.getCoordEdge2(i),
              axis1D.getCoordEdge2(i) - axis1D.getCoordEdge1(i));
        }
      }
    }
    f.format(" %s%n", axis.getUnits());
  }

  @Override
  public Iterable<Dimension> getDomain() {
    return domain;
  }

  @Override
  public boolean isProductSet() {
    return true;
  }

  @Override
  public List<RangeIterator> getRanges() {
    List<RangeIterator> result = new ArrayList<>();
    for (GridAxis axis : axes) {
      if (axis.getDependenceType() == GridAxis.DependenceType.independent) {
        result.add(axis.getRangeIterator());
      }
    }
    return result;
  }

  /////////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Optional<GridCoordinateSystem> subset(GridSubset params, Formatter errLog) {
    Formatter errMessages = new Formatter();

    Builder<?> builder = this.toBuilder();
    builder.clearAxes();
    for (GridAxis axis : getGridAxes()) {
      if (axis.getDependenceType() == GridAxis.DependenceType.dependent) {
        continue;
      }
      /*
       * if (axis.getAxisType().isHoriz())
       * continue;
       * if (isTime2D(axis))
       * continue;
       */

      GridAxis subsetAxis = axis.subset(params, errMessages);
      if (subsetAxis != null) {
        GridAxis1D subsetInd = (GridAxis1D) subsetAxis; // independent always 1D
        builder.addAxis(subsetInd);

        /*
         * subset any dependent axes
         * for (CoverageCoordAxis dependent : getDependentAxes(subsetInd)) {
         * Optional<GridAxis1D> depo = dependent.subsetDependent(subsetInd, errMessages);
         * depo.ifPresent(subsetAxes::add);
         * }
         */
      }
    }

    AtomicBoolean isConstantForecast = new AtomicBoolean(false); // need a mutable boolean
    /*
     * if (time2DCoordSys != null) {
     * Optional<List<CoverageCoordAxis>> time2Do =
     * time2DCoordSys.subset(params, isConstantForecast, makeCFcompliant, errMessages);
     * time2Do.ifPresent(subsetAxes::addAll);
     * }
     */

    /*
     * Optional<HorizCoordSys> horizo = horizCoordSys.subset(params, errMessages);
     * if (horizo.isPresent()) {
     * HorizCoordSys subsetHcs = horizo.get();
     * subsetAxes.addAll(subsetHcs.getCoordAxes());
     * }
     */

    String errs = errMessages.toString();
    if (!errs.isEmpty()) {
      errLog.format("%s", errs);
      return Optional.empty();
    }

    return Optional.of(builder.build());
  }

  ////////////////////////////////////////////////////////////////////////////////////////////
  private final ImmutableList<GridAxis> axes;
  private final ImmutableSet<Dimension> domain;
  private final FeatureType featureType;
  private final boolean isLatLon;
  private final String name;
  private final Projection projection;
  private final ImmutableList<CoordinateTransform> transforms;

  /**
   * Create a GridCoordinateSystem from a DatasetClassifier.CoordSysClassifier.
   * 
   * @param classifier
   * @param gridAxes The gridAxes already built, so there are no duplicates as we make the coordSys.
   */
  GridCS(DatasetClassifier.CoordSysClassifier classifier, Map<String, GridAxis> gridAxes) {
    // make name based on coordinate
    this.name = classifier.getName();
    this.featureType = classifier.getFeatureType();
    this.isLatLon = classifier.isLatLon();
    this.projection = classifier.getProjection();
    this.transforms = ImmutableList.copyOf(classifier.getCoordTransforms());

    ImmutableList.Builder<GridAxis> axesb = ImmutableList.builder();
    for (CoordinateAxis axis : classifier.getAxesUsed()) {
      if (axis.getAxisType() == null) {
        continue;
      }
      if (gridAxes.get(axis.getFullName()) == null) {
        String full = axis.getFullName();
        continue;
      }
      axesb.add(Preconditions.checkNotNull(gridAxes.get(axis.getFullName())));
    }
    this.axes = axesb.build();

    this.domain = Dimensions.makeDomain(classifier.getAxesUsed());
  }

  public GridCS.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected GridCS.Builder<?> addLocalFieldsToBuilder(GridCS.Builder<? extends GridCS.Builder<?>> builder) {
    builder.setName(this.name).setDomain(this.domain).setFeatureType(this.featureType).setLatLon(this.isLatLon)
        .setProjection(this.projection).setTransforms(this.transforms).setAxes(this.axes);

    return builder;
  }

  GridCS(Builder<?> builder) {
    this.name = builder.name;
    this.domain = ImmutableSet.copyOf(builder.domain);
    this.featureType = builder.featureType;
    this.isLatLon = builder.isLatLon;
    this.projection = builder.projection;
    this.transforms = builder.transforms;
    this.axes = ImmutableList.copyOf(builder.axes);
  }

  public static Builder<?> builder() {
    return new Builder2();
  }

  private static class Builder2 extends Builder<Builder2> {
    @Override
    protected Builder2 self() {
      return this;
    }
  }

  public static abstract class Builder<T extends GridCS.Builder<T>> {
    private String name;
    private Set<Dimension> domain;
    private FeatureType featureType;
    private boolean isLatLon;
    private Projection projection;
    private ImmutableList<CoordinateTransform> transforms;
    private ArrayList<GridAxis> axes = new ArrayList<>();

    private boolean built;

    protected abstract T self();

    public T setName(String name) {
      this.name = name;
      return self();
    }

    public T setDomain(Set<Dimension> domain) {
      this.domain = domain;
      return self();
    }

    public T setFeatureType(FeatureType featureType) {
      this.featureType = featureType;
      return self();
    }

    public T setLatLon(boolean latLon) {
      isLatLon = latLon;
      return self();
    }

    public T setProjection(Projection projection) {
      this.projection = projection;
      return self();
    }

    public T setTransforms(ImmutableList<CoordinateTransform> transforms) {
      this.transforms = transforms;
      return self();
    }

    public T clearAxes() {
      this.axes = new ArrayList<>();
      return self();
    }

    public T setAxes(List<GridAxis> axes) {
      this.axes = new ArrayList<>(axes);
      return self();
    }

    public T addAxis(GridAxis axis) {
      this.axes.add(axis);
      return self();
    }

    public GridCS build() {
      if (built)
        throw new IllegalStateException("already built");
      built = true;
      return new GridCS(this);
    }
  }

}
