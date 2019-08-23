/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dt;

import ucar.nc2.VariableSimpleIF;
import ucar.nc2.Variable;
import ucar.nc2.ft.FeatureDataset;
import java.io.IOException;
import java.util.Date;

/**
 * A RadialDataset in which the radials can be grouped into sweeps.
 * A sweep has the same gate geometry for all radials in the sweep, and has a RadialDatasetSweep.Type.
 *
 * @author yuan
 */
public interface RadialDatasetSweep extends FeatureDataset {
  /**
   * @return radar id
   */
  String getRadarID();

  /**
   * @return radar name
   */
  String getRadarName();

  /**
   * @return data format
   */
  String getDataFormat();

  /**
   * If all the sweeps are the same type, return it here, else NONE
   *
   * @return type of sweep, or NONE
   */
  RadialDatasetSweep.Type getCommonType();

  /**
   * If all sweeps have the same origin, return it here, else null
   *
   * @return common origin, or null
   */
  ucar.unidata.geoloc.EarthLocation getCommonOrigin();

  /**
   * Get the units of Calendar time.
   * To get a Date, from a time value, call DateUnit.getStandardDate(double value).
   * To get units as a String, call DateUnit.getUnitsString().
   *
   * @return units of Calendar time
   */
  ucar.nc2.units.DateUnit getTimeUnits();

  /**
   * Stationary versus moving radar.
   *
   * @return true if this is stationary.
   */
  boolean isStationary();

  /**
   * Remove cached data, if any.
   */
  void clearDatasetMemory();

  // public boolean isRadial();

  /**
   * Get the basic property of Radar,
   *
   * @return 0 if there is only one sweep
   */
  boolean isVolume();

  /**
   * The radial data variables available in the dataset.
   *
   * @return List of type RadialDatasetSweep.RadialVariable
   */
  java.util.List<VariableSimpleIF> getDataVariables();

  /**
   * A data Variable with radial geometry.
   */
  interface RadialVariable extends ucar.nc2.VariableSimpleIF {
    /**
     * @return the number of sweeps for this Variable
     */
    int getNumSweeps();

    /**
     * @param sweepNum which sweep
     * @return the sweepNum sweep
     */
    Sweep getSweep(int sweepNum);

    /**
     * @return data, of length getNumSweep() by getNumRadials() by getNumGates()
     * @throws java.io.IOException on io error
     */
    float[] readAllData() throws java.io.IOException;

    void clearVariableMemory();
  }

  /**
   * A sweep is 2D data using radial coordinate system (elevation, azimuth, radial distance)
   */
  interface Sweep {
    /**
     * @return the type of the Sweep
     */
    RadialDatasetSweep.Type getType();


    Variable getsweepVar();

    /**
     * @return the number of radials for this Sweep
     */
    int getRadialNumber();

    /**
     * @return the number of gates for all radials
     */
    int getGateNumber();

    /**
     * @return the beam width for all radials, in degrees
     */
    float getBeamWidth();

    /**
     * @return the Nyquist Frequency for all radials
     */
    float getNyquistFrequency();

    /**
     * Get the radial distance from origin to the start of the first data gate.
     * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
     *
     * @return distance to first gate in meters, for all radials
     */
    float getRangeToFirstGate();

    /**
     * Get the radial length of each data gate.
     * The radial distance to the ith gate = getRangeToFirstGate() + getGateSize() * i
     *
     * @return gate size in meters, for all radials
     */
    float getGateSize();

    /**
     * @return all the sweep data, of length getNumRadials() by getNumGates()
     * @throws java.io.IOException on io error
     */
    float[] readData() throws java.io.IOException;

    /**
     * @param radial which radial, must in interval [0,getRadialNumber())
     * @return the actual data, of length getNumGates()
     * @throws java.io.IOException on io error
     */
    float[] readData(int radial) throws java.io.IOException;

    /**
     * @param radial which radial, must in interval [0,getRadialNumber())
     * @return the elevation of the ith radial, in degrees
     * @throws java.io.IOException on io error
     */
    float getElevation(int radial) throws java.io.IOException;

    /**
     * @return all elevation in the sweep
     * @throws java.io.IOException on io error
     */
    float[] getElevation() throws java.io.IOException;

    /**
     * @return the average elevation of all the radials in the sweep, in degrees.
     *         Only valid if getType() == TYPE_
     */
    float getMeanElevation();

    /**
     * @param radial which radial, must in interval [0,getRadialNumber())
     * @return the azimuth of the ith radial, in degrees
     * @throws java.io.IOException on io error
     */
    float getAzimuth(int radial) throws java.io.IOException;

    /**
     * @return all azimuth in the sweep
     * @throws java.io.IOException on io error
     */
    float[] getAzimuth() throws java.io.IOException;

    /**
     * @return the average azimuth of all the radials in the sweep, in degrees.
     *         Only valid if getType() == TYPE_
     */
    float getMeanAzimuth();

    /**
     * @param radial which radial, must in interval [0,getRadialNumber())
     * @return the location of the origin of the ith radial.
     */
    ucar.unidata.geoloc.EarthLocation getOrigin(int radial);

    /**
     * @param radial which radial, must in interval [0,getRadialNumber())
     * @return the time of the ith radial, in units of getTimeUnits().
     * @throws java.io.IOException on io error
     */
    float getTime(int radial) throws IOException;

    /**
     * @return the starting time of the sweep, in units of getTimeUnits().
     */
    Date getStartingTime();

    /**
     * @return the ending time of the sweep, in units of getTimeUnits().
     */
    Date getEndingTime();

    /**
     * @return the index of sweep
     */
    int getSweepIndex();

    /**
     * deallocated memory of sweep
     */
    void clearSweepMemory();
  }

  /**
   * A Type of RadialSweep.
   */
  final class Type {
    private static java.util.List<Type> members = new java.util.ArrayList<>(20);
    public final static Type NONE = new Type("");

    private String name;

    private Type(String s) {
      this.name = s;
      members.add(this);
    }

    public static java.util.Collection getAllTypes() {
      return members;
    }

    /**
     * Find the DataType that matches this name, ignore case.
     *
     * @param name : match this name
     * @return DataType or null if no match.
     */
    public static Type getType(String name) {
      if (name == null)
        return null;
      for (Type m : members) {
        if (m.name.equalsIgnoreCase(name))
          return m;
      }
      return null;
    }

    /**
     * @return the type name.
     */
    public String toString() {
      return name;
    }

  }

}

