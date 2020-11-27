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
import ucar.nc2.Variable;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.grid.*;
import ucar.nc2.internal.dataset.DatasetClassifier;
import ucar.unidata.geoloc.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Base implementation of GridCoordinateSystem.
 * TODO is this general enough to be in API instead of interface?
 */
@Immutable
class GridCS implements GridCoordinateSystem {

  @Override
  public String getName() {
    return name;
  }

  // needed? is it always GRID?
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

  /**
   * Can this be a coordinate system for v?
   * True if each dimension of v is in this domain, or is 1 dimensional.
   */
  public boolean isCoordinateSystemFor(Variable v) {
    for (Dimension d : Dimensions.makeDimensionsAll(v)) {
      if (!domain.contains(d) && (d.getLength() != 1)) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Nullable
  public GridAxis1D getEnsembleAxis() {
    return (GridAxis1D) findCoordAxis(AxisType.Ensemble);
  }

  @Override
  public GridAxis getXHorizAxis() {
    return findCoordAxis(AxisType.GeoX, AxisType.Lon);
  }

  @Override
  public GridAxis getYHorizAxis() {
    return findCoordAxis(AxisType.GeoY, AxisType.Lat);
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
  public GridAxis getTimeOffsetAxis() {
    return findCoordAxis(AxisType.TimeOffset);
  }

  @Override
  public GridHorizCoordinateSystem getHorizCoordSystem() {
    return horizCsys;
  }

  @Override
  @Nullable
  public VerticalCT getVerticalCT() {
    Optional<CoordinateTransform> result =
        transforms.stream().filter(t -> t.getTransformType() == TransformType.Vertical).findFirst();
    return (VerticalCT) result.orElse(null);
  }



  @Override
  public String toString() {
    return getName();
  }

  public void show(Formatter f, boolean showCoords) {
    f.format("Coordinate System (%s)%n", getName());

    showCoordinateAxis(getRunTimeAxis(), f, showCoords);
    showCoordinateAxis(getEnsembleAxis(), f, showCoords);
    showCoordinateAxis(getTimeAxis(), f, showCoords);
    showCoordinateAxis(getVerticalAxis(), f, showCoords);
    showCoordinateAxis(getYHorizAxis(), f, showCoords);
    showCoordinateAxis(getXHorizAxis(), f, showCoords);

    if (horizCsys.getProjection() != null) {
      f.format(" Projection: %s %s%n", horizCsys.getProjection().getName(), horizCsys.getProjection().paramsToString());
    }

    f.format(" LLbb=%s%n", horizCsys.getLatLonBoundingBox());
    if ((horizCsys.getProjection() != null) && !horizCsys.isLatLon()) {
      f.format(" bb= %s%n", horizCsys.getBoundingBox());
    }
  }

  private void showCoordinateAxis(GridAxis axis, Formatter f, boolean showCoords) {
    if (axis == null) {
      return;
    }
    f.format(" %s (%s) ", axis.getName(), axis.getClass().getName());
    if (showCoords) {
      showCoords(axis, f);
    }
    f.format("%n");
  }

  private void showCoords(GridAxis axis, Formatter f) {
    if (axis instanceof GridAxis1D) {
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
    f.format(" %s", axis.getUnits());
  }

  @Override
  public String showFnSummary() {
    if (featureType == null)
      return "";

    Formatter f2 = new Formatter();
    f2.format("%s(", featureType.toString());

    ArrayList<GridAxis> otherAxes = new ArrayList<>();
    int count = 0;
    for (GridAxis axis : axes) {
      if (axis.getDependenceType() != GridAxis.DependenceType.independent) {
        otherAxes.add(axis);
        continue;
      }
      if (count > 0) {
        f2.format(",");
      }
      f2.format("%s", axis.getAxisType() == null ? axis.getName() : axis.getAxisType().getCFAxisName());
      count++;
    }
    f2.format(")");

    count = 0;
    for (GridAxis axis : otherAxes) {
      f2.format(count == 0 ? ": " : ",");
      f2.format("%s", axis.getAxisType() == null ? axis.getName() : axis.getAxisType().getCFAxisName());
      count++;
    }
    return f2.toString();
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
  private final FeatureType featureType; // TODO redo FeatureType
  private final boolean isLatLon;
  private final String name;
  private final ImmutableList<CoordinateTransform> transforms;
  private final GridHorizCoordinateSystem horizCsys;

  /**
   * Create a GridCoordinateSystem from a DatasetClassifier.CoordSysClassifier.
   * 
   * @param classifier
   * @param gridAxes The gridAxes already built, so there are no duplicates as we make the coordSys.
   */
  GridCS(DatasetClassifier.CoordSysClassifier classifier, Map<String, GridAxis> gridAxes) {
    this.featureType = classifier.getFeatureType();
    this.isLatLon = classifier.isLatLon();
    this.transforms = ImmutableList.copyOf(classifier.getCoordTransforms());

    ArrayList<GridAxis> axesb = new ArrayList<>();
    for (CoordinateAxis axis : classifier.getAxesUsed()) {
      GridAxis gaxis = gridAxes.get(axis.getFullName());
      axesb.add(Preconditions.checkNotNull(gaxis));
    }
    Collections.sort(axesb, new Grids.AxisComparator());
    this.axes = ImmutableList.copyOf(axesb);
    List<String> names = axes.stream().map(a -> a.getName()).collect(Collectors.toList());
    this.name = String.join(" ", names);

    this.domain = Dimensions.makeDomain(classifier.getAxesUsed(), false);
    this.horizCsys = GridHorizCS.create(getXHorizAxis(), getYHorizAxis(), classifier.getProjection());
  }

  public GridCS.Builder<?> toBuilder() {
    return addLocalFieldsToBuilder(builder());
  }

  // Add local fields to the builder.
  protected GridCS.Builder<?> addLocalFieldsToBuilder(GridCS.Builder<? extends GridCS.Builder<?>> builder) {
    builder.setName(this.name).setDomain(this.domain).setFeatureType(this.featureType).setLatLon(this.isLatLon)
        .setProjection(horizCsys.getProjection()).setTransforms(this.transforms).setAxes(this.axes);

    return builder;
  }

  GridCS(Builder<?> builder) {
    this.name = builder.name;
    this.domain = ImmutableSet.copyOf(builder.domain);
    this.featureType = builder.featureType;
    this.isLatLon = builder.isLatLon;
    this.transforms = builder.transforms;
    this.axes = ImmutableList.copyOf(builder.axes);
    this.horizCsys = GridHorizCS.create(getXHorizAxis(), getYHorizAxis(), builder.projection);
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
