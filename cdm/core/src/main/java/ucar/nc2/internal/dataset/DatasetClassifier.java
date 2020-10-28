/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.dataset;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import ucar.nc2.Dimension;
import ucar.nc2.Dimensions;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.*;
import ucar.nc2.units.SimpleUnit;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.projection.RotatedPole;

import java.util.*;
import java.util.stream.Collectors;

/** Coordinate System classification. TODO Here or Grid? */
public class DatasetClassifier {
  private final NetcdfDataset ds;
  private final Formatter errlog;
  private ArrayList<CoordSysClassifier> coordSysUsed = new ArrayList<>();

  HashMap<String, CoordinateAxis> axesUsed = new HashMap<>();
  FeatureType featureType;

  public DatasetClassifier(NetcdfDataset ds, Formatter errlog) {
    Preconditions.checkNotNull(ds);
    Preconditions.checkNotNull(errlog);

    this.ds = ds;
    this.errlog = errlog;
    errlog.format("DatasetClassifier for '%s'%n", ds.getLocation());

    // sort by largest number of coord axes first
    List<CoordinateSystem> css = new ArrayList<>(ds.getCoordinateSystems());
    css.sort((o1, o2) -> o2.getCoordinateAxes().size() - o1.getCoordinateAxes().size());

    for (CoordinateSystem cs : css) {
      classifyCoordSys(cs);
    }
    errlog.format("Dataset featureType = %s%n", featureType);
  }

  public List<CoordinateAxis> getAxesUsed() {
    List<CoordinateAxis> other = new ArrayList<>(axesUsed.values());
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

    if (this.featureType == null) {
      this.featureType = csc.featureType;
    }

    if (this.featureType == csc.featureType) {
      coordSysUsed.add(csc);
      csc.usedAxes.stream().forEach(a -> axesUsed.put(a.getShortName(), a));
    }

    return csc;
  }

  public class CoordSysClassifier {
    CoordinateSystem cs;
    FeatureType featureType;
    boolean isLatLon;
    CoordinateAxis xaxis, yaxis, timeAxis;
    CoordinateAxis vertAxis, ensAxis, timeOffsetAxis;
    CoordinateAxis rtAxis;
    List<CoordinateAxis> usedAxes = new ArrayList<>();
    List<CoordinateTransform> coordTransforms;
    Projection orgProj;

    private CoordSysClassifier(CoordinateSystem cs) {
      this.cs = cs;

      // must be at least 2 dimensions
      if (cs.getRankDomain() < 2) {
        errlog.format("CoordinateSystem '%s': domain rank < 2%n", cs.getName());
        return;
      }

      //////////////////////////////////////////////////////////////
      // horiz
      // must be lat/lon or have x,y and projection
      if (!cs.isLatLon()) {
        // do check for GeoXY
        if ((cs.getXaxis() == null) || (cs.getYaxis() == null)) {
          errlog.format("%s: NO Lat,Lon or X,Y axis%n", cs.getName());
          return;
        }
        if (null == cs.getProjection()) {
          errlog.format("%s: NO projection found%n", cs.getName());
          return;
        }
      }

      // obtain the x,y or lat/lon axes. x,y normally must be convertible to km
      if (cs.isGeoXY()) {
        usedAxes.add(xaxis = cs.getXaxis());
        usedAxes.add(yaxis = cs.getYaxis());

        Projection p = cs.getProjection();
        if (!(p instanceof RotatedPole)) {
          if (!SimpleUnit.kmUnit.isCompatible(xaxis.getUnitsString())) {
            errlog.format("%s: X axis units are not convertible to km%n", cs.getName());
          }
          if (!SimpleUnit.kmUnit.isCompatible(yaxis.getUnitsString())) {
            errlog.format("%s: Y axis units are not convertible to km%n", cs.getName());
          }
        }
      } else {
        usedAxes.add(xaxis = cs.getLonAxis());
        usedAxes.add(yaxis = cs.getLatAxis());
        isLatLon = true;
      }

      // check x,y rank <= 2
      if ((xaxis.getRank() > 2) || (yaxis.getRank() > 2)) {
        errlog.format("%s: X and Y axis rank must be <= 2%n", cs.getName());
        return;
      }

      // check x,y with size 1
      if ((xaxis.getSize() < 2) || (yaxis.getSize() < 2)) {
        errlog.format("%s: X and Y axis size must be >= 2%n", cs.getName());
        return;
      }

      // check that the x,y have at least 2 dimensions between them ( this eliminates point data)
      int xyDomainSize = CoordinateSystem.countDomain(new CoordinateAxis[] {xaxis, yaxis});
      if (xyDomainSize < 2) {
        errlog.format("%s: X and Y axis must have 2 or more dimensions%n", cs.getName());
        return;
      }

      //////////////////////////////////////////////////////////////
      // vert
      CoordinateAxis zAxis = cs.getHeightAxis();
      if ((zAxis == null) || (zAxis.getRank() > 1)) {
        if (cs.getPressureAxis() != null)
          zAxis = cs.getPressureAxis();
      }
      if ((zAxis == null) || (zAxis.getRank() > 1)) {
        if (cs.getZaxis() != null)
          zAxis = cs.getZaxis();
      }
      if (zAxis != null) {
        if (zAxis instanceof CoordinateAxis1D)
          usedAxes.add(vertAxis = (CoordinateAxis1D) zAxis);
      }

      //////////////////////////////////////////////////////////////
      // time
      CoordinateAxis rt = cs.findAxis(AxisType.RunTime);
      if (rt != null) {
        if (rt.getRank() > 1) { // A runtime axis must be scalar or one-dimensional
          errlog.format("%s: RunTime axis must be 1D or scalar%n", cs.getName());
          // return; // LOOK
        } else {
          usedAxes.add(rtAxis = rt);
        }
      }

      CoordinateAxis t = cs.getTaxis();
      if ((t != null) && t.getRank() > 1) { // If time axis is two-dimensional...
        if (rtAxis != null && rtAxis.getRank() == 1) {
          // time first dimension must agree with runtime
          if (!rtAxis.getDimension(0).equals(t.getDimension(0))) {
            errlog.format("%s: 2D Time axis first dimension must be runtime%n", cs.getName());
            return; // TODO
          }
        }
      }

      if (t != null) {
        usedAxes.add(timeAxis = t);
      }

      CoordinateAxis toAxis = cs.findAxis(AxisType.TimeOffset);
      if (toAxis != null) {
        if (toAxis.getRank() == 1) {
          usedAxes.add(timeOffsetAxis = (CoordinateAxis1D) toAxis);
        }
      }

      if (t == null && rtAxis != null && timeOffsetAxis != null) {
        // LOOK create time coord ??
      }

      CoordinateAxis eAxis = cs.findAxis(AxisType.Ensemble);
      if (eAxis != null) {
        if (eAxis instanceof CoordinateAxis1D) {
          usedAxes.add(ensAxis = (CoordinateAxis1D) eAxis);
        }
      }

      this.featureType = classify();
      this.coordTransforms = new ArrayList<>(cs.getCoordinateTransforms());
      this.orgProj = cs.getProjection();
      this.usedAxes.sort(new CoordinateAxis.AxisComparator()); // canonical ordering of axes
    }

    private FeatureType classify() {
      // now to classify
      boolean is2Dtime = (rtAxis != null) && (timeOffsetAxis != null || (timeAxis != null && timeAxis.getRank() == 2));
      if (is2Dtime) {
        return FeatureType.FMRC; // LOOK this would allow 2d horiz
      }

      boolean is2Dhoriz = isLatLon && (xaxis.getRank() == 2) && (yaxis.getRank() == 2);
      if (is2Dhoriz) {
        Set<Dimension> xyDomain = CoordinateSystem.makeDomain(Lists.newArrayList(xaxis, yaxis));
        if (timeAxis != null && CoordinateSystem.isSubset(Dimensions.makeDimensionsAll(timeAxis), xyDomain))
          return FeatureType.SWATH; // LOOK prob not exactly right
        else
          return FeatureType.CURVILINEAR;
      }

      // what makes it a grid?
      // each dimension must have its own coordinate variable
      List<CoordinateAxis> axes = usedAxes.stream().filter(a -> a.getRank() > 0).collect(Collectors.toList());
      Set<Dimension> domain = Dimensions.makeDomain(axes);
      if (domain.size() == axes.size()) {
        return FeatureType.GRID;
      }

      // default TODO what does this mean?
      return FeatureType.COVERAGE;
    }

    public String getName() {
      return cs.getName();
    }

    public FeatureType getFeatureType() {
      return featureType;
    }

    public boolean isLatLon() {
      return isLatLon;
    }

    public List<CoordinateTransform> getCoordTransforms() {
      return coordTransforms;
    }

    public Projection getProjection() {
      return orgProj;
    }

    public List<CoordinateAxis> getAxesUsed() {
      return usedAxes;
    }

    @Override
    public String toString() {
      Formatter f2 = new Formatter();
      f2.format("%s ", cs.getName());
      f2.format("%s", featureType == null ? "" : featureType.toString());
      f2.format("%n xAxis=  %s", xaxis == null ? "" : xaxis.getNameAndDimensions());
      f2.format("%n yAxis=  %s", yaxis == null ? "" : yaxis.getNameAndDimensions());
      f2.format("%n zAxis=  %s", vertAxis == null ? "" : vertAxis.getNameAndDimensions());
      f2.format("%n tAxis=  %s", timeAxis == null ? "" : timeAxis.getNameAndDimensions());
      f2.format("%n rtAxis= %s", rtAxis == null ? "" : rtAxis.getNameAndDimensions());
      f2.format("%n toAxis= %s", timeOffsetAxis == null ? "" : timeOffsetAxis.getNameAndDimensions());
      f2.format("%n ensAxis=%s", ensAxis == null ? "" : ensAxis.getNameAndDimensions());
      if (featureType == null)
        return f2.toString();

      f2.format("%n%n axes=(");
      for (CoordinateAxis axis : usedAxes)
        f2.format("%s, ", axis.getShortName());
      f2.format(") {");

      return f2.toString();
    }

    public String showSummary() {
      if (featureType == null)
        return "";

      Formatter f2 = new Formatter();
      f2.format("%s", featureType.toString());

      f2.format("(");
      int count = 0;
      for (CoordinateAxis axis : usedAxes) {
        if (count++ > 0)
          f2.format(",");
        f2.format("%s", axis.getAxisType() == null ? axis.getShortName() : axis.getAxisType().getCFAxisName());
      }
      f2.format(")");

      return f2.toString();
    }

  }

}
