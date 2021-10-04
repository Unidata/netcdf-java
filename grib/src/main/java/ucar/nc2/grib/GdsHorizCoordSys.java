/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib;

import com.google.common.base.MoreObjects;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.unidata.geoloc.*;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.util.GaussianLatitudes;
import ucar.unidata.util.StringUtil2;
import javax.annotation.concurrent.Immutable;

/**
 * A Horizontal coordinate system generated from a GRIB1 or GRIB2 GDS.
 */
@Immutable
public class GdsHorizCoordSys {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GdsHorizCoordSys.class);

  private final String name;
  public final int template, gdsNumberPoints, scanMode;
  public final ucar.unidata.geoloc.Projection proj;
  public final double startx, dx; // km
  public final double starty, dy; // km
  public final int nx, ny; // regridded
  public final int nxRaw, nyRaw; // raw
  public final int[] nptsInLine; // non-null id thin grid

  // hmmmm
  private Array<Double> gaussLats;
  private Array<Double> gaussw;

  public GdsHorizCoordSys(String name, int template, int gdsNumberPoints, int scanMode, Projection proj, double startx,
      double dx, double starty, double dy, int nxRaw, int nyRaw, int[] nptsInLine) {

    this.name = name;
    this.template = template;
    this.gdsNumberPoints = gdsNumberPoints; // only used by GRIB2
    this.scanMode = scanMode;
    this.proj = proj;
    this.startx = startx;
    this.dx = dx;
    this.starty = starty;
    this.dy = dy;
    this.nxRaw = nxRaw;
    this.nyRaw = nyRaw;
    this.nptsInLine = nptsInLine;

    // thin grids
    if (nptsInLine != null) {
      if (nxRaw > 0) {
        nx = nxRaw;
        ny = QuasiRegular.getMax(nptsInLine);
      } else if (nyRaw > 0) {
        ny = nyRaw;
        nx = QuasiRegular.getMax(nptsInLine);
      } else {
        throw new IllegalArgumentException("Quasi Grids nx,ny=" + nxRaw + "," + nyRaw);
      }
    } else {
      nx = nxRaw;
      ny = nyRaw;
    }
  }

  public String getName() {
    return name;
  }

  public double getStartX() {
    return startx;
  }

  public double getStartY() {
    if (gaussLats != null) {
      return gaussLats.get(0);
    }
    return starty;
  }

  public double getEndX() {
    return startx + dx * (nx - 1);
  }

  public double getEndY() {
    if (gaussLats != null) {
      return gaussLats.get((int) gaussLats.getSize() - 1);
    }
    return starty + dy * (ny - 1);
  }

  public int getScanMode() {
    return scanMode;
  }

  public boolean isLatLon() {
    return proj instanceof LatLonProjection;
  }

  public LatLonPoint getCenterLatLon() {
    return proj.projToLatLon(startx + dx * nx / 2, starty + dy * ny / 2);
  }

  public String makeDescription() {
    return name + "_" + ny + "X" + nx + " (Center " + getCenterLatLon() + ")";
  }

  public String makeId() {
    LatLonPoint center = getCenterLatLon();
    StringBuilder result = new StringBuilder(name + "_" + ny + "X" + nx + "-" + LatLonPoints.toString(center, 2));
    StringUtil2.replace(result, ". ", "p-");
    return result.toString();
  }

  public ProjectionRect getProjectionBB() {
    return new ProjectionRect(ProjectionPoint.create(getStartX(), getStartY()), getEndX() - getStartX(),
        getEndY() - getStartY());
  }

  public LatLonRect getLatLonBB() {
    if (isLatLon()) {
      return LatLonRect.builder(LatLonPoint.create(getStartY(), getStartX()), dy * (ny - 1), dx * (nx - 1)).build();
    } else {
      return proj.projToLatLonBB(getProjectionBB());
    }
  }

  ////////////////////////////////////////////////

  // set gaussian weights based on nparallels
  // some weird adjustment for la1 and la2.
  public void setGaussianLats(int nparallels, float la1, float la2) {
    if (this.gaussLats != null) {
      throw new RuntimeException("Cant modify GdsHorizCoordSys");
    }

    int nlats = (2 * nparallels);
    GaussianLatitudes gaussLats = GaussianLatitudes.factory(nlats);

    int bestStartIndex = 0, bestEndIndex = 0;
    double bestStartDiff = Double.MAX_VALUE;
    double bestEndDiff = Double.MAX_VALUE;
    for (int i = 0; i < nlats; i++) {
      double diff = Math.abs(gaussLats.getLatitude(i) - la1);
      if (diff < bestStartDiff) {
        bestStartDiff = diff;
        bestStartIndex = i;
      }
      diff = Math.abs(gaussLats.getLatitude(i) - la2);
      if (diff < bestEndDiff) {
        bestEndDiff = diff;
        bestEndIndex = i;
      }
    }

    if (Math.abs(bestEndIndex - bestStartIndex) + 1 != nyRaw) {
      log.warn("GRIB gaussian lats: NP != NY, use NY"); // see email from Toussaint@dkrz.de datafil:
      nlats = nyRaw;
      gaussLats = GaussianLatitudes.factory(nlats);
      bestStartIndex = 0;
      bestEndIndex = nyRaw - 1;
    }
    boolean goesUp = bestEndIndex > bestStartIndex;

    // create the data
    int useIndex = bestStartIndex;
    double[] data = new double[nyRaw];
    double[] gaussw = new double[nyRaw];
    for (int i = 0; i < nyRaw; i++) {
      data[i] = gaussLats.getLatitude(useIndex);
      gaussw[i] = gaussLats.getGaussWeight(useIndex);

      if (goesUp) {
        useIndex++;
      } else {
        useIndex--;
      }
    }

    this.gaussLats = Arrays.factory(ArrayType.DOUBLE, new int[] {nyRaw}, data);
    this.gaussw = Arrays.factory(ArrayType.DOUBLE, new int[] {nyRaw}, gaussw);
  }

  public boolean hasGaussianLats() {
    return gaussLats != null;
  }

  public Array<Double> getGaussianLats() {
    return gaussLats;
  }

  public double[] getGaussianLatsArray() {
    return (double[]) Arrays.copyPrimitiveArray(gaussLats);
  }

  public Array<Double> getGaussianWeights() {
    return gaussw;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("name", name).add("template", template)
        .add("gdsNumberPoints", gdsNumberPoints).add("scanMode", scanMode).add("proj", proj).add("startx", startx)
        .add("dx", dx).add("starty", starty).add("dy", dy).add("nx", nx).add("ny", ny).add("nxRaw", nxRaw)
        .add("nyRaw", nyRaw).add("nptsInLine", nptsInLine).add("gaussLats", gaussLats).add("gaussw", gaussw).toString();
  }
}
