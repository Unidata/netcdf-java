/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id:Doradeheader.java 63 2006-07-12 21:50:51Z edavis $

package ucar.nc2.iosp.dorade;


import ucar.nc2.*;

import ucar.nc2.constants.*;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;

import ucar.atd.dorade.*;

import java.io.*;
import java.util.*;

public class Doradeheader {
  private ucar.nc2.NetcdfFile ncfile;
  private float[] lat_min, lat_max, lon_min, lon_max, hi_max, hi_min;

  static public boolean isValidFile(ucar.unidata.io.RandomAccessFile raf) {
    try {
      java.io.RandomAccessFile file = raf.getRandomAccessFile();
      if (file == null) return false;
      boolean t = DoradeSweep.isDoradeSweep(file);
      if (!t) return false;
    } catch (DoradeSweep.DoradeSweepException ex) {
      return false;
    }
    return true;
  }

  void read(DoradeSweep mySweep, ucar.nc2.NetcdfFile ncfile, PrintStream out) {

    this.ncfile = ncfile;
    DoradePARM[] parms = mySweep.getParamList();
    int nRays = mySweep.getNRays();

    int numSensor = mySweep.getNSensors();
    int[] ncells = new int[numSensor];
    Dimension[] gateDim = new Dimension[numSensor];
    for (int i = 0; i < numSensor; i++) {
      try {
        int j = i + 1;
        ncells[i] = mySweep.getNCells(i);
        gateDim[i] = new Dimension("gate_" + j, ncells[i]);
        ncfile.addDimension(null, gateDim[i]);
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }

    ArrayList<Dimension>[] dims = new ArrayList[numSensor];
    ArrayList<Dimension> dims1 = new ArrayList<>();
    ArrayList<Dimension>[] dims2 = new ArrayList[numSensor];
    int nCells;

    //  Dimension sensorDim = new Dimension("sensor", numSensor, true);
    //  ncfile.addDimension( null, sensorDim);
    Dimension radialDim = new Dimension("radial", nRays);
    ncfile.addDimension(null, radialDim);

    for (int i = 0; i < numSensor; i++) {
      dims[i] = new ArrayList<>();
      dims2[i] = new ArrayList<>();
      dims[i].add(radialDim);
      dims[i].add(gateDim[i]);
      dims2[i].add(gateDim[i]);
    }

    // dims1.add( sensorDim);
    dims1.add(radialDim);


    float[][] altitudes = new float[numSensor][];
    float[][] latitudes = new float[numSensor][];
    float[][] longitudes = new float[numSensor][];
    lat_min = new float[numSensor];
    lat_max = new float[numSensor];
    lon_min = new float[numSensor];
    lon_max = new float[numSensor];
    hi_min = new float[numSensor];
    hi_max = new float[numSensor];
    MAMath.MinMax[] latMinMax = new MAMath.MinMax[numSensor];
    MAMath.MinMax[] lonMinMax = new MAMath.MinMax[numSensor];
    MAMath.MinMax[] hiMinMax = new MAMath.MinMax[numSensor];
    boolean[] isMoving = new boolean[numSensor];
    for (int i = 0; i < numSensor; i++) {
      try {
        int nc = mySweep.getNCells(i);
        altitudes[i] = new float[nRays];
        latitudes[i] = new float[nRays];
        longitudes[i] = new float[nRays];
        isMoving[i] = mySweep.sensorIsMoving(i);
        altitudes[i] = mySweep.getAltitudes(i);
        latitudes[i] = mySweep.getLatitudes(i);
        longitudes[i] = mySweep.getLongitudes(i);
      } catch (Exception ex) {
        ex.printStackTrace();
      }

      latMinMax[i] = getMinMaxData(latitudes[i]);
      lonMinMax[i] = getMinMaxData(longitudes[i]);
      hiMinMax[i] = getMinMaxData(altitudes[i]);
      float dis = (float) ((mySweep.getRangeToFirstCell(i) + (mySweep.getNCells(i) - 1) * mySweep.getCellSpacing(i)) * 1.853 / 111.26 / 1000);
      lat_min[i] = (float) (latMinMax[i].min - dis);
      lat_max[i] = (float) (latMinMax[i].max + dis);
      lon_min[i] = (float) (lonMinMax[i].min + dis * Math.cos(latitudes[i][0]));
      lon_max[i] = (float) (lonMinMax[i].max - dis * Math.cos(latitudes[i][0]));
      hi_min[i] = (float) hiMinMax[i].min;
      hi_max[i] = (float) hiMinMax[i].max;
    }

    //adding the global nc attribute
    addNCAttributes(ncfile, mySweep);


    // add elevation coordinate variable
    String vName = "elevation";
    String lName = "elevation angle in degres: 0 = parallel to pedestal base, 90 = perpendicular";
    Attribute att = new Attribute(_Coordinate.AxisType, AxisType.RadialElevation.toString());
    addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

    // add azimuth coordinate variable
    vName = "azimuth";
    lName = "azimuth angle in degrees: 0 = true north, 90 = east";
    att = new Attribute(_Coordinate.AxisType, AxisType.RadialAzimuth.toString());
    addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

    // add gate coordinate variable
    for (int i = 0; i < numSensor; i++) {
      int j = i + 1;
      vName = "distance_" + j;
      lName = "Radial distance to the start of gate";
      att = new Attribute(_Coordinate.AxisType, AxisType.RadialDistance.toString());
      addParameter(vName, lName, ncfile, dims2[i], att, DataType.FLOAT, "meters");
    }

    // add radial coordinate variable

    for (int i = 0; i < numSensor; i++) {
      int j = i + 1;
      vName = "latitudes_" + j;
      lName = "Latitude of the instrument " + j;
      att = new Attribute(_Coordinate.AxisType, AxisType.Lat.toString());
      addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

      vName = "longitudes_" + j;
      lName = "Longitude of the instrument " + j;
      att = new Attribute(_Coordinate.AxisType, AxisType.Lon.toString());
      addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "degrees");

      vName = "altitudes_" + j;
      lName = "Altitude in meters (asl) of the instrument " + j;
      att = new Attribute(_Coordinate.AxisType, AxisType.Height.toString());
      addParameter(vName, lName, ncfile, dims1, att, DataType.FLOAT, "meters");

      vName = "rays_time";
      lName = "rays time";
      att = new Attribute(_Coordinate.AxisType, AxisType.Time.toString());
      addParameter(vName, lName, ncfile, dims1, att, DataType.DOUBLE, "milliseconds since 1970-01-01 00:00 UTC");

    }

    vName = "Range_to_First_Cell";
    lName = "Range to the center of the first cell";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");

    vName = "Cell_Spacing";
    lName = "Distance between cells";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");


    vName = "Fixed_Angle";
    lName = "Targeted fixed angle for this scan";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "degrees");

    vName = "Nyquist_Velocity";
    lName = "Effective unambigous velocity";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "m/s");

    vName = "Unambiguous_Range";
    lName = "Effective unambigous range";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "meters");

    vName = "Radar_Constant";
    lName = "Radar constant";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "c");

    vName = "rcvr_gain";
    lName = "Receiver Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "ant_gain";
    lName = "Antenna Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "sys_gain";
    lName = "System Gain";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "db");

    vName = "bm_width";
    lName = "Beam Width";
    addParameter(vName, lName, ncfile, null, null, DataType.FLOAT, "degrees");

    /* Variable ct = new Variable(ncfile, null, null, "radialCoordinateTransform");
      ct.setDataType(DataType.CHAR);
      ct.setDimensions(""); // scalar
      ct.addAttribute( new Attribute("transform_type", "Radial"));
      ct.addAttribute( new Attribute("_CoordinateTransformType", "Radial"));
      ct.addAttribute( new Attribute("_CoordinateAxes", "elevation azimuth distance_1"));
      ncfile.addVariable(null, ct);   */

    try {
      for (int p = 0; p < parms.length; p++) {
        String pval = parms[p].getDescription();
        nCells = parms[p].getNCells();
        int ii = getGateDimsIndex(nCells, gateDim, numSensor);

        addVariable(ncfile, dims[ii], parms[p]);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    // finish
    ncfile.finish();

  }

  public MAMath.MinMax getMinMaxData(float[] data) {
    int[] shape = new int[1];
    shape[0] = data.length;
    Array a = Array.factory(DataType.FLOAT, shape, data);
    return MAMath.getMinMax(a);
  }

  int getGateDimsIndex(int cell, Dimension[] dList, int numSensor) {
    int j = 0;
    for (int i = 0; i < numSensor; i++) {
      Dimension d = new Dimension("gate_" + i, cell);
      if (dList[i].equals(d)) {
        j = i;
        break;
      }
    }
    return j;
  }


  private void makeCoordinateData(Variable elev, Variable azim, DoradeSweep mySweep) {
    Object ele =  mySweep.getElevations();
    Object azi =  mySweep.getAzimuths();

    Array elevData = Array.factory(elev.getDataType(), elev.getShape(), ele);
    Array aziData = Array.factory(azim.getDataType(), azim.getShape(), azi);

    elev.setCachedData(elevData, false);
    azim.setCachedData(aziData, false);
  }

  void addParameter(String pName, String longName, NetcdfFile nc, ArrayList dims, Attribute att, DataType dtype, String ut) {
    Variable vVar = new Variable(nc, null, null, pName);
    vVar.setDataType(dtype);
    if (dims != null) vVar.setDimensions(dims);
    else vVar.setDimensions("");
    if (att != null) vVar.addAttribute(att);
    vVar.addAttribute(new Attribute(CDM.UNITS, ut));
    vVar.addAttribute(new Attribute(CDM.LONG_NAME, longName));
    nc.addVariable(null, vVar);
  }

  void addVariable(NetcdfFile nc, ArrayList dims, DoradePARM dparm) {

    Variable v = new Variable(nc, null, null, dparm.getName());
    v.setDataType(DataType.FLOAT);
    v.setDimensions(dims);
    ncfile.addVariable(null, v);

    v.addAttribute(new Attribute(CDM.LONG_NAME, dparm.getDescription()));
    v.addAttribute(new Attribute(CDM.UNITS, dparm.getUnits()));
    String coordinates = "elevation azimuth distance_1 " + "latitudes_1 longitudes_1 altitudes_1";
    v.addAttribute(new Attribute(_Coordinate.Axes, coordinates));
      /*
      v.addAttribute( new Attribute(CDM.MISSING_VALUE, new Float(dparm.getBadDataFlag())));
      v.addAttribute( new Attribute("_FillValue", new Float(dparm.getBadDataFlag())));
      v.addAttribute( new Attribute("scale_factor", dparm.getUnits()));
      v.addAttribute( new Attribute("polarization", dparm.getUnits()));
      v.addAttribute( new Attribute("Frequencies_GHz", dparm.getUnits()));
      v.addAttribute( new Attribute("InterPulsePeriods_secs", dparm.getUnits()));
      v.addAttribute( new Attribute("ThresholdValue", new Float(dparm.getThresholdValue())));
      v.addAttribute( new Attribute("ThresholdParamName", dparm.getthresholdParamName()));
      v.addAttribute( new Attribute("usedPRTs", new Integer(dparm.getusedPRTs())));
      v.addAttribute( new Attribute("usedFrequencies", new Integer(dparm.getusedFrequencies())));
       */
  }

  void addNCAttributes(NetcdfFile nc, DoradeSweep mySweep) {
    nc.addAttribute(null, new Attribute("summary", "Dorade radar data " +
            "from radar " + mySweep.getSensorName(0) + " in the project " + mySweep.getProjectName()));
    nc.addAttribute(null, new Attribute("radar_name", mySweep.getSensorName(0)));
    nc.addAttribute(null, new Attribute("project_name", mySweep.getProjectName()));
    nc.addAttribute(null, new Attribute("keywords_vocabulary", "dorade"));
    nc.addAttribute(null, new Attribute("geospatial_lat_min", lat_min[0]));
    nc.addAttribute(null, new Attribute("geospatial_lat_max", lat_max[0]));
    nc.addAttribute(null, new Attribute("geospatial_lon_min", lon_min[0]));
    nc.addAttribute(null, new Attribute("geospatial_lon_max", lon_max[0]));
    nc.addAttribute(null, new Attribute("geospatial_vertical_min", lon_min[0]));
    nc.addAttribute(null, new Attribute("geospatial_vertical_max", lon_max[0]));

    Date[] dd = mySweep.getTimes();
    if (dd != null) {
      nc.addAttribute(null, new Attribute("time_coverage_start", dd[0].toString()));
      nc.addAttribute(null, new Attribute("time_coverage_end", dd[dd.length - 1].toString()));
    }
    nc.addAttribute(null, new Attribute("Content", "This file contains one scan of remotely sensed data"));
    nc.addAttribute(null, new Attribute(CDM.CONVENTIONS, _Coordinate.Convention));
    nc.addAttribute(null, new Attribute(CF.FEATURE_TYPE, FeatureType.RADIAL.toString()));
    nc.addAttribute(null, new Attribute("format", "Unidata/netCDF/Dorade"));
    nc.addAttribute(null, new Attribute("Radar_Name", mySweep.getSensorName(0)));
    nc.addAttribute(null, new Attribute("Project_name", "" + mySweep.getProjectName()));
    nc.addAttribute(null, new Attribute("VolumeCoveragePatternName", mySweep.getScanMode(0).getName()));
    nc.addAttribute(null, new Attribute("Volume_Number", "" + mySweep.getVolumnNumber()));
    nc.addAttribute(null, new Attribute("Sweep_Number", "" + mySweep.getSweepNumber()));
    nc.addAttribute(null, new Attribute("Sweep_Date", DoradeSweep.formatDate(mySweep.getTime())));
    if (mySweep.sensorIsMoving(0))
      nc.addAttribute(null, new Attribute("IsStationary", "0"));
    else
      nc.addAttribute(null, new Attribute("IsStationary", "1"));
  }
}