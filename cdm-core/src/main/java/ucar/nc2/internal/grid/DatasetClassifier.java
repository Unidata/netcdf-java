/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.grid;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateSystem;
import ucar.nc2.dataset.CoordinateTransform;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.CurvilinearProjection;
import ucar.unidata.geoloc.projection.RotatedPole;
import ucar.unidata.geoloc.projection.sat.Geostationary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Coordinate System classification. */
public class DatasetClassifier {
  private final Formatter infolog;
  private final ArrayList<CoordSysClassifier> coordSysUsed = new ArrayList<>();

  private final HashMap<String, CoordinateAxis> indAxes = new HashMap<>(); // eliminate duplicates
  private final HashMap<String, CoordinateAxis> depAxes = new HashMap<>(); // eliminate duplicates
  private FeatureType featureType;

  public DatasetClassifier(NetcdfDataset ds, Formatter infolog) {
    Preconditions.checkNotNull(ds);
    Preconditions.checkNotNull(infolog);

    this.infolog = infolog;
    infolog.format("DatasetClassifier for '%s'%n", ds.getLocation());

    // sort by largest number of coord axes first
    List<CoordinateSystem> css = new ArrayList<>(ds.getCoordinateSystems());
    css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());

    for (CoordinateSystem cs : css) {
      classifyCoordSys(cs);
    }
    infolog.format("Dataset featureType = %s%n", featureType);
  }

  public List<CoordinateAxis> getIndependentAxes() {
    List<CoordinateAxis> other = new ArrayList<>(indAxes.values());
    other.sort(new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    return other;
  }

  public List<CoordinateAxis> getDependentAxes() {
    List<CoordinateAxis> other = new ArrayList<>(depAxes.values());
    other.sort(new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    return other;
  }

  public List<CoordSysClassifier> getCoordinateSystemsUsed() {
    return coordSysUsed;
  }

  public FeatureType getFeatureType() {
    return featureType;
  }

  private CoordSysClassifier classifyCoordSys(CoordinateSystem cs) {
    CoordSysClassifier csc = new CoordSysClassifier(cs);

    // Use the first (largest) one that has a classification
    if (this.featureType == null) {
      this.featureType = csc.featureType;
    }

    // Then add only those types to coordSysUsed
    if (this.featureType == csc.featureType) {
      coordSysUsed.add(csc);
      csc.indAxes.forEach(a -> indAxes.put(a.getFullName(), a));
      csc.depAxes.forEach(a -> depAxes.put(a.getFullName(), a));
    }

    return csc;
  }

  public class CoordSysClassifier {

    public String getName() {
      return cs.getName();
    }

    public CoordinateSystem getCoordinateSystem() {
      return cs;
    }

    public FeatureType getFeatureType() {
      return featureType;
    }

    public List<CoordinateTransform> getCoordTransforms() {
      return coordTransforms;
    }

    public Projection getProjection() {
      return orgProj;
    }

    public List<CoordinateAxis> getAxesUsed() {
      ArrayList<CoordinateAxis> result = new ArrayList<>(indAxes);
      result.addAll(depAxes);
      return result;
    }

    //////////////////////////////////////////////////////////////////////
    CoordinateSystem cs;
    FeatureType featureType;
    boolean standardGeoXY, standardLatLon, curvilinear, curvilinearWith1D;
    CoordinateAxis xaxis, yaxis, lataxis, lonaxis, timeAxis, timeOffsetAxis; // may be 1 or 2 dimensional
    CoordinateAxis vertAxis, ensAxis, rtAxis; // must be 1 dimensional
    List<CoordinateAxis> indAxes = new ArrayList<>();
    List<CoordinateAxis> depAxes = new ArrayList<>();
    List<CoordinateTransform> coordTransforms;
    @Nullable
    Projection orgProj;

    private CoordSysClassifier(CoordinateSystem cs) {
      this.cs = cs;

      // must be at least 2 dimensions
      if (cs.getDomain().size() < 2) {
        infolog.format(" '%s': domain rank < 2%n", cs.getName());
        return;
      }

      //////////////////////////////////////////////////////////////
      // horiz
      xaxis = cs.findAxis(AxisType.GeoX);
      yaxis = cs.findAxis(AxisType.GeoY);
      lataxis = cs.findAxis(AxisType.Lat);
      lonaxis = cs.findAxis(AxisType.Lon);

      standardGeoXY = cs.isGeoXY() && xaxis.getRank() == 1 && yaxis.getRank() == 1
          && 2 == Dimensions.makeDomain(ImmutableList.of(xaxis, yaxis), true).size();

      standardLatLon = cs.isLatLon() && lataxis.getRank() == 1 && lonaxis.getRank() == 1
          && 2 == Dimensions.makeDomain(ImmutableList.of(lataxis, lonaxis), true).size();

      curvilinear = cs.isLatLon() && lataxis.getRank() == 2 && lonaxis != null && lonaxis.getRank() == 2
          && 2 == Dimensions.makeDomain(ImmutableList.of(lataxis, lonaxis), true).size();

      curvilinearWith1D = curvilinear && xaxis != null && xaxis.getRank() == 1 && yaxis != null && yaxis.getRank() == 1;

      if (standardGeoXY) {
        indAxes.add(xaxis);
        indAxes.add(yaxis);
        this.orgProj = cs.getProjection();

        if (curvilinearWith1D) {
          depAxes.add(lonaxis);
          depAxes.add(lataxis);
        }

        Projection p = cs.getProjection();
        if (!(p instanceof RotatedPole) && !(p instanceof Geostationary)) {
          if (!SimpleUnit.kmUnit.isCompatible(xaxis.getUnitsString())) {
            infolog.format(" %s: X axis units are not convertible to km%n", cs.getName());
          }
          if (!SimpleUnit.kmUnit.isCompatible(yaxis.getUnitsString())) {
            infolog.format(" %s: Y axis units are not convertible to km%n", cs.getName());
          }
        }
      } else if (curvilinearWith1D) {
        indAxes.add(xaxis);
        indAxes.add(yaxis);
        depAxes.add(lonaxis);
        depAxes.add(lataxis);
        this.orgProj = new CurvilinearProjection();

      } else if (curvilinear) {
        depAxes.add(lonaxis);
        depAxes.add(lataxis);
        this.orgProj = new CurvilinearProjection();

      } else if (standardLatLon) {
        indAxes.add(lonaxis);
        indAxes.add(lataxis);
        this.orgProj = cs.getProjection();

      } else { // add log messages on failure
        // must be lat/lon or have x,y and projection
        if (!cs.isLatLon()) {
          // do check for GeoXY
          if ((cs.findAxis(AxisType.GeoX) == null) || (cs.findAxis(AxisType.GeoY) == null)) {
            infolog.format(" %s: NO Lat,Lon or X,Y axis%n", cs.getName());
            return;
          }
          if (null == cs.getProjection()) {
            infolog.format(" %s: NO projection found%n", cs.getName());
            return;
          }
        }

        // check x,y rank <= 2
        if ((xaxis.getRank() > 2) || (yaxis.getRank() > 2)) {
          infolog.format(" %s: X and Y axis rank must be <= 2%n", cs.getName());
          return;
        }

        // check x,y with size 1
        if ((xaxis.getSize() < 2) || (yaxis.getSize() < 2)) {
          infolog.format(" %s: X and Y axis size must be >= 2%n", cs.getName());
          return;
        }

        // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
        int xyDomainSize = Dimensions.makeDomain(ImmutableList.of(xaxis, yaxis), true).size();
        if (xyDomainSize < 2) {
          infolog.format(" %s: X and Y axis must have 2 or more dimensions%n", cs.getName());
          return;
        }
        // general failure
        infolog.format(" %s: not a grid or curvilinear%n", cs.getName());
        return;
      }

      //////////////////////////////////////////////////////////////
      // vert
      CoordinateAxis zAxis = cs.findAxis(AxisType.Height);
      if ((zAxis == null) || (zAxis.getRank() > 1)) {
        zAxis = cs.findAxis(AxisType.Pressure);
      }
      if ((zAxis == null) || (zAxis.getRank() > 1)) {
        zAxis = cs.findAxis(AxisType.GeoZ);
      }
      if (zAxis != null && zAxis.getRank() < 2) {
        indAxes.add(vertAxis = zAxis);
      }

      //////////////////////////////////////////////////////////////
      // time
      CoordinateAxis toAxis = cs.findAxis(AxisType.TimeOffset);
      if (toAxis != null) {
        if (toAxis.getRank() < 2) {
          indAxes.add(timeOffsetAxis = toAxis);
        } else {
          depAxes.add(timeOffsetAxis = toAxis);
        }
      }

      CoordinateAxis rt = cs.findAxis(AxisType.RunTime);
      if (rt != null) {
        if (rt.getRank() == 0) {
          depAxes.add(rtAxis = rt);
        } else if (rt.getRank() == 1) {
          indAxes.add(rtAxis = rt);
        } else { // A runtime axis must be scalar or one-dimensional
          infolog.format(" %s: RunTime axis must be 1D or scalar%n", cs.getName());
          // return; // LOOK
        }
      }

      boolean hasRuntimeAndOffset = (timeOffsetAxis != null) && (rtAxis != null);
      if (!hasRuntimeAndOffset) { // ignore time if we have runtime and offset
        CoordinateAxis t = cs.findAxis(AxisType.Time);
        if ((t != null) && t.getRank() > 1) { // If time axis is two-dimensional...
          if (rtAxis != null && rtAxis.getRank() == 1) {
            // time first dimension must agree with runtime
            if (!rtAxis.getDimension(0).equals(t.getDimension(0))) {
              infolog.format(" %s: 2D Time axis first dimension must be runtime%n", cs.getName());
              return; // TODO
            }
          }
        }
        if (t != null) {
          boolean sharedDimension = (rt != null) && t.getDimension(0).equals(rt.getDimension(0));
          if (t.getRank() == 1 && sharedDimension) {
            depAxes.add(t);
          } else {
            indAxes.add(timeAxis = t);
          }
        }
      }

      CoordinateAxis eAxis = cs.findAxis(AxisType.Ensemble);
      if (eAxis != null) {
        if (eAxis.getRank() == 1) {
          indAxes.add(ensAxis = eAxis);
        }
      }

      this.featureType = classify();
      this.coordTransforms = new ArrayList<>(cs.getCoordinateTransforms());
      this.indAxes.sort(new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    }

    @Nullable
    private FeatureType classify() {
      FeatureType result = null;

      // FMRC is when we have 2D timeAxis and no timeOffset
      boolean is2Dtime = (rtAxis != null) && (timeOffsetAxis == null) && (timeAxis != null && timeAxis.getRank() == 2);

      if (is2Dtime) {
        result = FeatureType.FMRC; // LOOK this would allow 2d horiz

      } else if (!standardGeoXY && curvilinearWith1D) {
        Set<Dimension> xyDomain = Dimensions.makeDomain(Lists.newArrayList(xaxis, yaxis), true);
        if (timeAxis != null && Dimensions.isSubset(Dimensions.makeDimensionsAll(timeAxis), xyDomain)) {
          result = FeatureType.SWATH; // LOOK prob not exactly right
        } else {
          result = FeatureType.CURVILINEAR;
        }

      } else if (!standardGeoXY && curvilinear) {
        Set<Dimension> xyDomain = Dimensions.makeDomain(Lists.newArrayList(lonaxis, lataxis), true);
        if (timeAxis != null && Dimensions.isSubset(Dimensions.makeDimensionsAll(timeAxis), xyDomain)) {
          result = FeatureType.SWATH; // LOOK prob not exactly right
        } else {
          result = FeatureType.CURVILINEAR;
        }

      } else {
        // what makes it a grid?
        // each dimension must have its own coordinate variable
        List<CoordinateAxis> axes = indAxes.stream().filter(a -> a.getRank() == 1).collect(Collectors.toList());
        Set<Dimension> domain = Dimensions.makeDomain(axes, false);
        if (domain.size() == axes.size()) {
          result = FeatureType.GRID;
        }
      }

      infolog.format(" %s: classified as %s%n", cs.getName(), result);
      return result;
    }

    @Override
    public String toString() {
      Formatter f2 = new Formatter();
      f2.format("%s ", cs.getName());
      f2.format("%s", featureType == null ? "" : featureType.toString());
      f2.format("%n xAxis=  %s", xaxis == null ? "" : xaxis.getNameAndDimensions());
      f2.format("%n yAxis=  %s", yaxis == null ? "" : yaxis.getNameAndDimensions());
      f2.format("%n latAxis=  %s", lataxis == null ? "" : lataxis.getNameAndDimensions());
      f2.format("%n lonAxis=  %s", lonaxis == null ? "" : lonaxis.getNameAndDimensions());
      f2.format("%n zAxis=  %s", vertAxis == null ? "" : vertAxis.getNameAndDimensions());
      f2.format("%n tAxis=  %s", timeAxis == null ? "" : timeAxis.getNameAndDimensions());
      f2.format("%n rtAxis= %s", rtAxis == null ? "" : rtAxis.getNameAndDimensions());
      f2.format("%n toAxis= %s", timeOffsetAxis == null ? "" : timeOffsetAxis.getNameAndDimensions());
      f2.format("%n ensAxis=%s", ensAxis == null ? "" : ensAxis.getNameAndDimensions());
      if (featureType == null)
        return f2.toString();

      f2.format("%n%n axes=(");
      for (CoordinateAxis axis : indAxes)
        f2.format("%s, ", axis.getShortName());
      f2.format(") {");

      return f2.toString();
    }

  }

}
