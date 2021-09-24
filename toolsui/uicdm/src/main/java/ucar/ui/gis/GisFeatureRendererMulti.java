/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.gis;

import ucar.ui.gis.GisFeatureRendererMulti.FeatureMD.Part;
import ucar.unidata.geoloc.*;
import ucar.ui.prefs.Debug;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

/**
 * Superclass for rendering collections of GisFeatures.
 *
 * @author John Caron
 */
public abstract class GisFeatureRendererMulti extends GisFeatureRenderer {
  private static boolean useDiscretization;
  private static double pixelMatch = 2.0;

  public static void setDiscretization(boolean b) {
    useDiscretization = b;
  }

  public static void setPixelMatch(double d) {
    pixelMatch = d;
  }

  private ArrayList<FeatureSet> featSetList; // list of featureSets for progressive disclosure

  ////// this is what the subclasses have to implement (besides the constructor)
  @Override
  public abstract LatLonRect getPreferredArea();

  @Override
  protected abstract java.util.List<GisFeature> getFeatures(); // collection of AbstractGisFeature

  @Override
  protected abstract Projection getDataProjection(); // what projection is the data in?

  /**
   * Sets new projection for subsequent drawing.
   *
   * @param project the new projection
   */
  @Override
  public void setProjection(Projection project) {
    displayProject = project;

    if (featSetList == null) {
      return;
    }
    for (FeatureSet fs : featSetList) {
      fs.newProjection = true;
    }
  }

  public void createFeatureSet(double minDist) {
    // make a FeatureSet out of this, defer actually creating the points
    FeatureSet fs = new FeatureSet(null, minDist);

    // add to the list of featureSets
    if (featSetList == null)
      initFeatSetList();
    featSetList.add(fs);
  }

  ////////////////////////////

  // get the set of shapes to draw.
  // we have to deal with both projections and resolution-dependence
  protected Iterator<Shape> iterator(java.awt.Graphics2D g, AffineTransform normal2device) {
    long startTime = System.currentTimeMillis();

    if (featSetList == null) {
      initFeatSetList();
      assert !featSetList.isEmpty();
    }

    // which featureSet should we ue?
    FeatureSet fs = featSetList.get(0);
    if (featSetList.size() > 1) {
      // compute scale
      double scale = 1.0;
      try {
        AffineTransform world2device = g.getTransform();
        AffineTransform world2normal = normal2device.createInverse();
        world2normal.concatenate(world2device);
        scale = Math.max(Math.abs(world2normal.getScaleX()), Math.abs(world2normal.getShearX())); // drawing or printing
        if (Debug.isSet("GisFeature/showTransform")) {
          System.out.println("GisFeature/showTransform: " + world2normal + "\n scale = " + scale);
        }
      } catch (java.awt.geom.NoninvertibleTransformException e) {
        System.out.println(" GisRenderFeature: NoninvertibleTransformException on " + normal2device);
      }
      if (!displayProject.isLatLon()) {
        scale *= 111.0; // km/deg
      }
      double minD = Double.MAX_VALUE;
      for (Object aFeatSetList : featSetList) {
        FeatureSet tryfs = (FeatureSet) aFeatSetList;
        double d = Math.abs(scale * tryfs.minDist - pixelMatch); // we want min features ~ 2 pixels
        if (d < minD) {
          minD = d;
          fs = tryfs;
        }
      }
      if (Debug.isSet("GisFeature/MapResolution")) {
        System.out.println("GisFeature/MapResolution: scale = " + scale + " minDist = " + fs.minDist);
      }
    }

    // we may have deferred the actual creation of the points
    if (fs.featureList == null) {
      fs.createFeatures();
    }

    // ok, now see if we need to project
    if (!displayProject.equals(fs.project)) {
      fs.setProjection(displayProject);
    } else { // deal with LatLon
      if (fs.newProjection && displayProject.isLatLon()) {
        fs.setProjection(displayProject);
      }
    }
    fs.newProjection = false;

    if (Debug.isSet("GisFeature/timing/getShapes")) {
      long tookTime = System.currentTimeMillis() - startTime;
      System.out.println("timing.getShapes: " + tookTime * .001 + " seconds");
    }

    // so return it, already
    return fs.iterator();
  }

  // make an ArrayList of Shapes from the given featureList and current display Projection
  private ArrayList<Shape> makeShapes(Iterator<GisFeature> featList) {
    Shape shape;
    ArrayList<Shape> shapeList = new ArrayList<>();
    Projection dataProject = getDataProjection();

    if (Debug.isSet("GisFeature/MapDraw")) {
      System.out.println("GisFeature/MapDraw: makeShapes with " + displayProject);
    }

    while (featList.hasNext()) {
      AbstractGisFeature feature = (AbstractGisFeature) featList.next();
      if (dataProject.isLatLon()) // always got to run it through if its lat/lon
        shape = feature.getProjectedShape(displayProject);
      else if (dataProject == displayProject)
        shape = feature.getShape();
      else
        shape = feature.getProjectedShape(dataProject, displayProject);

      shapeList.add(shape);
    }

    return shapeList;
  }

  private void initFeatSetList() {
    featSetList = new ArrayList<>();
    featSetList.add(new FeatureSet(getFeatures(), 0.0)); // full resolution set
  }

  private class FeatureSet implements Iterable<Shape> {
    List<GisFeature> featureList;
    double minDist;
    Projection project;
    ArrayList<Shape> shapeList;
    boolean newProjection = true;

    FeatureSet(List<GisFeature> featureList, double minDist) {
      this.featureList = featureList;
      this.minDist = minDist;
    }

    void setProjection(Projection project) {
      this.project = project;
      shapeList = makeShapes(featureList.iterator());
    }

    @Override
    public Iterator<Shape> iterator() {
      return shapeList.iterator();
    }

    void createFeatures() {
      featureList = new ArrayList<>();

      for (Object o : GisFeatureRendererMulti.this.getFeatures()) {
        AbstractGisFeature feature = (AbstractGisFeature) o;
        FeatureMD featMD = new FeatureMD(minDist);

        for (GisPart gp : feature) {
          Part part = featMD.newPart(gp.getNumPoints());

          int np = gp.getNumPoints();
          double[] xx = gp.getX();
          double[] yy = gp.getY();

          part.set(xx[0], yy[0]);
          for (int i = 1; i < np - 1; i++) {
            part.setIfDistant(xx[i], yy[i]);
          }

          if (part.getNumPoints() > 1) {
            part.set(xx[np - 1], yy[np - 1]); // close polygons
            part.truncateArray();
            featMD.add(part);
          }
        } // loop over parts

        if (featSetList == null) {
          initFeatSetList();
        }
        if (featMD.getNumParts() > 0) {
          featureList.add(featMD);
        }
      } // loop over featuures

      getStats(featureList.iterator());
    } // createFeatures()

    private void discretizeArray(double[] d, int n) {
      if (minDist == 0.0)
        return;
      for (int i = 0; i < n; i++) {
        d[i] = (Math.rint(d[i] / minDist) * minDist) + minDist / 2;
      }
    }

  } // FeatureSet inner class

  // these are derived Features based on a mimimum distance between points
  static class FeatureMD extends AbstractGisFeature {
    private final ArrayList<GisPart> parts = new ArrayList<>();
    private int total_pts;
    private final double minDist;
    private double minDist2;

    FeatureMD(double minDist) {
      this.minDist = minDist;
      minDist2 = minDist * minDist;
    }

    void add(FeatureMD.Part part) {
      total_pts += part.getNumPoints();
      parts.add(part);
    }

    FeatureMD.Part newPart(int maxPts) {
      return new FeatureMD.Part(maxPts);
    }

    private double discretize(double d) {
      if (!useDiscretization || (minDist == 0.0))
        return d;
      return (Math.rint(d / minDist) * minDist) + minDist / 2;
    }

    // implement GisFeature
    @Override
    public java.awt.geom.Rectangle2D getBounds2D() {
      return null;
    }

    @Override
    public int getNumPoints() {
      return total_pts;
    }

    @Override
    public int getNumParts() {
      return parts.size();
    }

    @Override
    public java.util.Iterator<GisPart> iterator() {
      return parts.iterator();
    }

    class Part implements GisPart {
      private int size;
      private double[] wx; // lat/lon coords
      private double[] wy;

      // constructor
      Part(int maxPts) {
        wx = new double[maxPts];
        wy = new double[maxPts];
        size = 0;
        minDist2 = minDist * minDist;
      }

      void set(double x, double y) {
        wx[size] = discretize(x);
        wy[size] = discretize(y);
        size++;
      }

      private void setNoD(double x, double y) {
        wx[size] = x;
        wy[size] = y;
        size++;
      }


      void setIfDistant(double x, double y) {
        x = discretize(x);
        y = discretize(y);
        double dx = x - wx[size - 1];
        double dy = y - wy[size - 1];
        double dist2 = dx * dx + dy * dy;
        if (dist2 >= minDist2)
          // if ((x != wx[size-1]) || (y != wy[size-1]))
          setNoD(x, y);
      }

      void truncateArray() {
        double[] x = new double[size];
        double[] y = new double[size];

        for (int i = 0; i < size; i++) {
          x[i] = wx[i]; // arraycopy better?
          y[i] = wy[i]; // arraycopy better?
        }
        wx = x;
        wy = y;
      }

      // implement GisPart
      @Override
      public int getNumPoints() {
        return size;
      }

      @Override
      public double[] getX() {
        return wx;
      }

      @Override
      public double[] getY() {
        return wy;
      }
    }
  }

  protected double getStats(Iterator featList) {
    int total_pts = 0;
    int total_parts = 0;
    int total_feats = 0;
    int cross_pts = 0;
    double avgD = 0;
    double minD = Double.MAX_VALUE;
    double maxD = -Double.MAX_VALUE;

    Projection dataProject = getDataProjection();
    ProjectionPoint lastW;

    while (featList.hasNext()) {
      AbstractGisFeature feature = (AbstractGisFeature) featList.next();
      total_feats++;

      for (GisPart gp : feature) {
        total_parts++;

        double[] xx = gp.getX();
        double[] yy = gp.getY();
        int np = gp.getNumPoints();

        lastW = ProjectionPoint.create(xx[0], yy[0]);

        for (int i = 1; i < np; i++) {
          ProjectionPoint thisW = ProjectionPoint.create(xx[i], yy[i]);
          if (!dataProject.crossSeam(thisW, lastW)) {
            double dx = (xx[i] - xx[i - 1]);
            double dy = (yy[i] - yy[i - 1]);
            double dist = Math.sqrt(dx * dx + dy * dy);

            total_pts++;
            avgD += dist;
            minD = Math.min(minD, dist);
            maxD = Math.max(maxD, dist);
          } else
            cross_pts++;

          lastW = ProjectionPoint.create(xx[i], yy[i]);
        }
      }
    }

    avgD = total_pts == 0 ? 0 : (avgD / total_pts);
    if (Debug.isSet("GisFeature/MapResolution")) {
      System.out.println("Map.resolution: total_feats = " + total_feats);
      System.out.println(" total_parts = " + total_parts);
      System.out.println(" total_pts = " + total_pts);
      System.out.println(" cross_pts = " + cross_pts);
      System.out.println(" avg distance = " + avgD);
      System.out.println(" min distance = " + minD);
      System.out.println(" max distance = " + maxD);
    }

    return avgD;
  }
}
