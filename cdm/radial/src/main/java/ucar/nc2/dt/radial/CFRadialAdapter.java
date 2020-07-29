/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt.radial;

import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.RadialDatasetSweep;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateUnit;
import ucar.nc2.units.DateUnit;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import java.io.IOException;
import java.util.*;
import static ucar.ma2.MAMath.nearlyEquals;

/** CF-Radial */
public class CFRadialAdapter extends AbstractRadialAdapter {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CFRadialAdapter.class);

  private NetcdfDataset ds;
  private double latv, lonv, elev;
  private double[] time;
  private float[] elevation;
  private float[] azimuth;
  private float[] range;
  private int[] rayStartIdx;
  private int[] rayEndIdx;
  private int[] ray_n_gates;
  private int[] ray_start_index;
  private int nsweeps;
  private boolean isStationary;
  private boolean isStationaryChecked;

  /////////////////////////////////////////////////
  // TypedDatasetFactoryIF

  public Object isMine(FeatureType wantFeatureType, NetcdfDataset ncd, Formatter errlog) {
    String convStr = ncd.getRootGroup().findAttributeString("Conventions", null);
    if ((null != convStr) && convStr.startsWith("CF/Radial"))
      return this;
    return null;
  }

  public FeatureDataset open(FeatureType ftype, NetcdfDataset ncd, Object analysis, ucar.nc2.util.CancelTask task,
      Formatter errlog) {
    return new CFRadialAdapter(ncd);
  }

  public FeatureType getScientificDataType() {
    return FeatureType.RADIAL;
  }

  // needed for FeatureDatasetFactory
  public CFRadialAdapter() {}

  /**
   * Constructor.
   *
   * @param ds Source NetCDF dataset
   */
  public CFRadialAdapter(NetcdfDataset ds) {
    this.ds = ds;
    desc = "CF/Radial radar dataset";
    init();

    for (Variable var : ds.getVariables()) {
      addRadialVariable(ds, var);
    }
  }

  public void init() {
    setEarthLocation();
    try {
      Variable t = ds.findVariable("time");
      Array tArray = t.read();
      time = (double[]) tArray.copyTo1DJavaArray();

      Variable ele = ds.findVariable("elevation");
      Array eArray = ele.read();
      elevation = (float[]) eArray.copyTo1DJavaArray();

      Variable azi = ds.findVariable("azimuth");
      Array aArray = azi.read();
      azimuth = (float[]) aArray.copyTo1DJavaArray();

      Variable rng = ds.findVariable("range");
      Array rArray = rng.read();
      range = (float[]) rArray.copyTo1DJavaArray();

      Variable sidx0 = ds.findVariable("sweep_start_ray_index");
      rayStartIdx = (int[]) sidx0.read().copyTo1DJavaArray();

      Variable sidx1 = ds.findVariable("sweep_end_ray_index");
      rayEndIdx = (int[]) sidx1.read().copyTo1DJavaArray();

      nsweeps = ds.findDimension("sweep").getLength();

      Variable var = ds.findVariable("ray_n_gates");
      if (var != null)
        ray_n_gates = (int[]) var.read().copyTo1DJavaArray();

      var = ds.findVariable("ray_start_index");
      if (var != null)
        ray_start_index = (int[]) var.read().copyTo1DJavaArray();

      setTimeUnits();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    setStartDate();
    setEndDate();
    setBoundingBox();
  }

  protected void setBoundingBox() {
    LatLonRect bb;

    if (origin == null) {
      return;
    }

    double dLat = Math.toDegrees(getMaximumRadialDist() / Earth.WGS84_EARTH_RADIUS_METERS);
    double latRadians = Math.toRadians(origin.getLatitude());
    double dLon = dLat * Math.cos(latRadians);

    double lat1 = origin.getLatitude() - dLat / 2;
    double lon1 = origin.getLongitude() - dLon / 2;
    bb = new LatLonRect.Builder(LatLonPoint.create(lat1, lon1), dLat, dLon).build();

    boundingBox = bb;
  }

  double getMaximumRadialDist() {
    double maxdist = 0.0;

    for (Object dataVariable : dataVariables) {
      RadialVariable rv = (RadialVariable) dataVariable;
      Sweep sp = rv.getSweep(0);
      double dist = sp.getGateNumber() * sp.getGateSize();

      if (dist > maxdist) {
        maxdist = dist;
      }
    }

    return maxdist;
  }

  protected void setEarthLocation() {
    try {
      Variable ga = ds.findVariable("latitude");
      if (ga != null) {
        if (ga.isScalar()) {
          latv = ga.readScalarDouble();
        } else {
          Array gar = ga.read();
          latv = gar.getDouble(0);
        }
      } else {
        latv = 0.0;
      }

      ga = ds.findVariable("longitude");

      if (ga != null) {
        if (ga.isScalar()) {
          lonv = ga.readScalarDouble();
        } else {
          Array gar = ga.read();
          lonv = gar.getDouble(0);
        }
      } else {
        lonv = 0.0;
      }

      ga = ds.findVariable("altitude");
      if (ga != null) {
        if (ga.isScalar()) {
          elev = ga.readScalarDouble();
        } else {
          Array gar = ga.read();
          elev = gar.getDouble(0);
        }
      } else {
        elev = 0.0;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    origin = EarthLocation.create(latv, lonv, elev);
  }

  @Override
  public ucar.unidata.geoloc.EarthLocation getCommonOrigin() {
    return origin;
  }

  @Override
  public String getRadarID() {
    Attribute ga = ds.findGlobalAttribute("Station");
    if (ga != null) {
      return ga.getStringValue();
    } else {
      return "XXXX";
    }
  }

  @Override
  public String getRadarName() {
    Attribute ga = ds.findGlobalAttribute("instrument_name");
    if (ga != null) {
      return ga.getStringValue();
    } else {
      return "Unknown Station";
    }
  }

  @Override
  public String getDataFormat() {
    return "CF/RadialNetCDF";
  }


  @Override
  public boolean isVolume() {
    return true;
  }

  public boolean isHighResolution(NetcdfDataset nds) {
    return true;
  }

  public boolean isStationary() {
    // only check once
    if (!isStationaryChecked) {
      Variable lat = ds.findVariable("latitude");
      if (lat != null) {
        if (lat.isScalar())
          isStationary = lat.getSize() == 1;
        else {
          // if array, check to see if all of the values are
          // approximately the same
          Array gar;
          try {
            gar = lat.read();
            Object firstVal = gar.getObject(0);
            Array gar2 = gar.copy();
            for (int i = 1; i < gar.getSize(); i++) {
              gar2.setObject(i, firstVal);
            }
            isStationary = nearlyEquals(gar, gar2);
          } catch (IOException e) {
            log.error("Error reading latitude variable {}. Cannot determine if "
                + "platform is stationary. Setting to default (false).", lat.getFullName());
          }
        }
      }
      isStationaryChecked = true;
    }

    return isStationary;
  }

  protected void setTimeUnits() throws Exception {
    Variable t = ds.findVariable("time");
    String ut = t.getUnitsString();
    dateUnits = new DateUnit(ut);
    calDateUnits = CalendarDateUnit.of(null, ut);
  }

  protected void setStartDate() {
    String datetime = ds.getRootGroup().findAttributeString("time_coverage_start", null);
    if (datetime != null) {
      startDate = CalendarDate.parseISOformat(null, datetime).toDate();
    } else {
      startDate = calDateUnits.makeCalendarDate(time[0]).toDate();
    }
  }

  protected void setEndDate() {
    String datetime = ds.getRootGroup().findAttributeString("time_coverage_end", null);
    if (datetime != null) {
      endDate = CalendarDate.parseISOformat(null, datetime).toDate();
    } else {
      endDate = calDateUnits.makeCalendarDate(time[time.length - 1]).toDate();
    }
  }

  public void clearDatasetMemory() {
    for (VariableSimpleIF rvar : getDataVariables()) {
      RadialVariable radVar = (RadialVariable) rvar;
      radVar.clearVariableMemory();
    }
  }

  protected void addRadialVariable(NetcdfDataset nds, Variable var) {

    RadialVariable rsvar = null;
    int tIdx = var.findDimensionIndex("time");
    int rIdx = var.findDimensionIndex("range");
    int ptsIdx = var.findDimensionIndex("n_points");

    if (((tIdx == 0) && (rIdx == 1)) || (ptsIdx == 0)) {
      rsvar = makeRadialVariable(nds, var);
    }

    if (rsvar != null) {
      dataVariables.add(rsvar);
    }
  }

  protected RadialVariable makeRadialVariable(NetcdfDataset nds, Variable v0) {
    // this function is null in level 2
    return new CFRadial2Variable(nds, v0);
  }

  public String getInfo() {
    return "CFRadial2Dataset\n" + super.getDetailInfo() + "\n\n" + parseInfo;
  }

  private class CFRadial2Variable extends MyRadialVariableAdapter implements RadialDatasetSweep.RadialVariable {
    ArrayList<CFRadial2Sweep> sweeps;

    private boolean flattened;

    private CFRadial2Variable(NetcdfDataset nds, Variable v0) {
      super(v0.getShortName(), v0);

      sweeps = new ArrayList<>();

      int[] shape = v0.getShape();
      int ngates = shape[v0.getRank() - 1];
      flattened = v0.findDimensionIndex("n_points") == 0;

      for (int i = 0; i < nsweeps; i++) {
        // For flattened (1D stored data) find max number of gates
        if (flattened) {
          ngates = ray_n_gates[rayStartIdx[i]];
          for (int ray = rayStartIdx[i]; ray <= rayEndIdx[i]; ++ray)
            ngates = ray_n_gates[ray] > ngates ? ray_n_gates[ray] : ngates;
        }
        sweeps.add(new CFRadial2Sweep(v0, i, ngates, rayStartIdx[i], rayEndIdx[i]));
      }
    }

    public String toString() {
      return name;
    }

    public int getNumSweeps() {
      return nsweeps;
    }

    public Sweep getSweep(int sweepNo) {
      return sweeps.get(sweepNo);
    }

    int getNumRadials() {
      return azimuth.length;
    }

    // a 3D array nsweep * nradials * ngates
    // if high resolution data, it will be transferred to the same dimension
    public float[] readAllData() throws IOException {
      Array allData;
      Sweep spn = sweeps.get(0);
      Variable v = spn.getsweepVar();
      float missingVal = (float) v.attributes().findAttributeDouble("_FillValue", Double.NaN);

      int minRadial = getMinRadialNumber();
      int radials = getNumRadials();
      int gates = range.length;
      try {
        allData = v.read();
      } catch (IOException e) {
        throw new IOException(e.getMessage());
      }
      if (flattened) {
        float[] fa0 = (float[]) allData.get1DJavaArray(float.class);
        float[] fa = new float[minRadial * gates * nsweeps];
        Arrays.fill(fa, missingVal);
        for (int s = 0; s < nsweeps; ++s) {
          for (int r = 0; r < minRadial; ++r) {
            System.arraycopy(fa0, ray_start_index[rayStartIdx[s] + r], fa, s * minRadial * gates + r * gates,
                ray_n_gates[rayStartIdx[s] + r]);
          }
        }

        return fa;
      } else if (minRadial == radials) {
        return (float[]) allData.get1DJavaArray(float.class);
      } else {
        float[] fa = new float[minRadial * gates * nsweeps];
        float[] fa0 = (float[]) allData.get1DJavaArray(float.class);
        int pos = 0;
        for (int i = 0; i < nsweeps; i++) {

          int startIdx = rayStartIdx[i];
          // int endIdx = rayEndIdx[i];
          int len = minRadial * gates;
          System.arraycopy(fa0, startIdx * gates, fa, pos, len);
          pos = pos + len;
        }
        return fa;
      }
    }


    public int getMinRadialNumber() {
      int minRadialNumber = Integer.MAX_VALUE;
      for (int i = 0; i < nsweeps; i++) {
        Sweep swp = this.sweeps.get(i);
        int radialNumber = swp.getRadialNumber();
        if (radialNumber < minRadialNumber) {
          minRadialNumber = radialNumber;
        }
      }

      return minRadialNumber;
    }

    public void clearVariableMemory() {
      for (int i = 0; i < nsweeps; i++) {
      }
    }


    //////////////////////////////////////////////////////////////////////
    // Checking all azi to make sure there is no missing data at sweep
    // level, since the coordinate is 1D at this level, this checking also
    // remove those missing radials within a sweep.

    private class CFRadial2Sweep implements RadialDatasetSweep.Sweep {
      double meanElevation = Double.NaN;
      double meanAzimuth = Double.NaN;
      int ngates;
      public int startIdx, endIdx, numRays;
      int sweepno;
      Variable sweepVar;

      CFRadial2Sweep(Variable v, int sweepno, int gates, int startIdx, int endIdx) {
        this.sweepVar = v;
        this.sweepno = sweepno;
        this.ngates = gates;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.numRays = endIdx - startIdx + 1;
      }

      public int getStartIdx() {
        return startIdx;
      }

      public int getEndIdx() {
        return endIdx;
      }

      public Variable getsweepVar() {
        return sweepVar;
      }

      /** read 2d sweep data nradials * ngates */
      public float[] readData() throws java.io.IOException {
        return sweepData();
      }

      private float[] sweepData() throws IOException {
        int[] origin;
        int[] shape;

        // init section
        try {
          if (flattened) {
            // Get the 1D data for the sweep
            origin = new int[1];
            origin[0] = ray_start_index[startIdx];
            shape = new int[1];
            shape[0] = ray_start_index[endIdx] + ray_n_gates[endIdx] - origin[0];
            Array tempArray = sweepVar.read(origin, shape).reduce();
            float[] tempD = (float[]) tempArray.get1DJavaArray(Float.TYPE);

            // Figure out what to use as the initializer
            float missingVal = (float) sweepVar.attributes().findAttributeDouble("_FillValue", Double.NaN);

            // Create evenly strided output array and fill
            float[] ret = new float[ngates * numRays];
            Arrays.fill(ret, missingVal);
            int srcInd = 0;
            for (int ray = 0; ray < numRays; ++ray) {
              int gates = ray_n_gates[startIdx + ray];
              System.arraycopy(tempD, srcInd, ret, ray * ngates, gates);
              srcInd += gates;
            }

            return ret;
          } else {
            origin = new int[2];
            origin[0] = startIdx;
            shape = sweepVar.getShape();
            shape[0] = numRays;
            Array sweepTmp = sweepVar.read(origin, shape).reduce();
            return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
          }
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e);
        }
      }

      /** Return data for 1 ray */
      public float[] readData(int ray) throws java.io.IOException {
        return rayData(ray);
      }

      /** read the radial data from the radial variable */
      public float[] rayData(int ray) throws java.io.IOException {
        int[] origin;
        int[] shape;

        // init section
        if (flattened) {
          origin = new int[1];
          origin[0] = ray_start_index[startIdx + ray];
          shape = new int[1];
          shape[0] = ray_n_gates[startIdx + ray];
        } else {
          origin = new int[2];
          origin[0] = startIdx + ray;
          shape = sweepVar.getShape();
          shape[0] = 1;
        }

        try {
          Array sweepTmp = sweepVar.read(origin, shape).reduce();
          return (float[]) sweepTmp.get1DJavaArray(Float.TYPE);
        } catch (ucar.ma2.InvalidRangeException e) {
          throw new IOException(e);
        }
      }

      public void setMeanElevation() {
        double sum = 0.0;
        int sumSize = 0;
        for (int i = 0; i < numRays; i++) {
          if (!Double.isNaN(elevation[i])) {
            sum = sum + elevation[startIdx + i];
            sumSize++;
          }
        }
        if (sumSize > 0)
          meanElevation = sum / sumSize;
      }

      public float getMeanElevation() {
        if (Double.isNaN(meanElevation)) {
          setMeanElevation();
        }
        return (float) meanElevation;
      }

      public int getGateNumber() {
        return ngates;
      }

      public int getRadialNumber() {
        return numRays;
      }

      public RadialDatasetSweep.Type getType() {
        return null;
      }

      public ucar.unidata.geoloc.EarthLocation getOrigin(int ray) {
        return origin;
      }

      public Date getStartingTime() {
        return startDate;
      }

      public Date getEndingTime() {
        return endDate;
      }

      public int getSweepIndex() {
        return sweepno;
      }

      public void setMeanAzimuth() {
        double sum = 0.0;
        int sumSize = 0;
        for (int i = 0; i < numRays; i++) {
          if (!Double.isNaN(azimuth[i])) {
            sum = sum + azimuth[startIdx + i];
            sumSize++;
          }
        }
        if (sumSize > 0)
          meanAzimuth = sum / sumSize;
      }

      public float getMeanAzimuth() {
        if (Double.isNaN(meanAzimuth)) {
          setMeanAzimuth();
        }
        return (float) meanAzimuth;
      }

      public boolean isConic() {
        return true;
      }

      public float getElevation(int ray) {
        return elevation[ray + startIdx];
      }

      public float[] getElevation() {
        float[] elev = new float[numRays];
        System.arraycopy(elevation, startIdx, elev, 0, numRays);
        return elev;
      }

      public float[] getAzimuth() {
        float[] azimu = new float[numRays];
        System.arraycopy(azimuth, startIdx, azimu, 0, numRays);
        return azimu;
      }

      public float getAzimuth(int ray) {
        return azimuth[ray + startIdx];
      }

      public float getRadialDistance(int gate) {
        return range[gate];
      }

      public float getTime(int ray) {
        return (float) time[ray + startIdx];
      }

      public float getBeamWidth() {
        return 0.95f; // degrees, info from Chris Burkhart
      }

      public float getNyquistFrequency() {
        return 0; // LOOK this may be radial specific
      }

      public float getRangeToFirstGate() {
        return getRadialDistance(0);
      }

      public float getGateSize() {
        return getRadialDistance(1) - getRadialDistance(0);
      }

      public boolean isGateSizeConstant() {
        return true;
      }

      public void clearSweepMemory() {}
    } // LevelII2Sweep class

  } // LevelII2Variable
}
