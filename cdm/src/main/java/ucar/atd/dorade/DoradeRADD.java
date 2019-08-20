/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.atd.dorade;

import ucar.nc2.constants.CDM;
import ucar.nc2.util.Misc;

import java.io.*;
import java.util.Arrays;

class DoradeRADD extends DoradeDescriptor {

  /**
   * <p>Title: RadarType</p>
   * <p>Description: nested top level class for defining DORADE radar
   * types</p>
   */
  static class RadarType {
    private String name;

    protected RadarType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  /**
   * ground-based radar type
   */
  public static final RadarType TYPE_GROUND = new RadarType("ground");
  /**
   * airborne tail-mounted radar, forward beam type
   */
  public static final RadarType TYPE_AIR_FORE = new RadarType("airborne (fore)");
  /**
   * airborne tail-mounted radar, aft beam type
   */
  public static final RadarType TYPE_AIR_AFT = new RadarType("airborne (aft)");
  /**
   * airborne tail-mounted radar type
   */
  public static final RadarType TYPE_AIR_TAIL = new RadarType("airborne (tail)");
  /**
   * airborne lower fuselage radar type
   */
  public static final RadarType TYPE_AIR_LF = new RadarType("airborne (lower fuselage)");
  /**
   * shipborne radar type
   */
  public static final RadarType TYPE_SHIP = new RadarType("shipborne");
  /**
   * airborne nose radar type
   */
  public static final RadarType TYPE_AIR_NOSE = new RadarType("airborne (nose)");
  /**
   * satellite radar type
   */
  public static final RadarType TYPE_SATELLITE = new RadarType("satellite");
  /**
   * fixed lidar type
   */
  public static final RadarType TYPE_FIXED_LIDAR = new RadarType("fixed lidar");
  /**
   * moving lidar type
   */
  public static final RadarType TYPE_MOVING_LIDAR = new RadarType("moving lidar");

  private static RadarType[] radarTypes = {
          TYPE_GROUND,       // 0
          TYPE_AIR_FORE,     // 1
          TYPE_AIR_AFT,      // 2
          TYPE_AIR_LF,       // 3
          TYPE_AIR_TAIL,     // 4
          TYPE_SHIP,         // 5
          TYPE_AIR_NOSE,     // 6
          TYPE_SATELLITE,    // 7
          TYPE_MOVING_LIDAR, // 8
          TYPE_FIXED_LIDAR,  // 9
  };

  //
  // lookup table mapping DORADE scan mode integer values to ScanMode-s
  //
  private static ScanMode[] scanModeTable = {
          ScanMode.MODE_calibration, // 0
          ScanMode.MODE_PPI,         // 1
          ScanMode.MODE_coplane,     // 2
          ScanMode.MODE_RHI,         // 3
          ScanMode.MODE_vertical,    // 4
          ScanMode.MODE_target,      // 5
          ScanMode.MODE_manual,      // 6
          ScanMode.MODE_idle,        // 7
          ScanMode.MODE_SUR,         // 8
          ScanMode.MODE_air,         // 9
          ScanMode.MODE_horizontal,  // 10
  };


  private String radarName;
  private float radarConstant;
  private float peakPower;
  private float noisePower; // dBm
  private float rcvrGain; // dB
  private float antennaGain; // dB
  private float systemGain; // (antenna gain - waveguide loss) dB
  private float hBeamWidth; // degrees
  private float vBeamWidth; // degrees
  private short radarTypeNdx; // map into radarTypes
  private ScanMode scanMode;
  private float rotVelocity; // requested rotational velocity, deg/s
  private float scanParam0; // mode-specific scan parameter
  private float scanParam1; // mode-specific scan parameter
  private short nParams;
  private short nAdditionalDescriptors;

  //
  // Compression scheme and known legal values
  //
  private short compressionScheme;
  public static final int COMPRESSION_NONE = 0;
  public static final int COMPRESSION_HRD = 1;

  //
  // Data reduction method and known legal values
  //
  private short dataReductionMethod;
  public static final int DATA_REDUCTION_NONE = 1;
  public static final int DATA_REDUCTION_BY_AZIMUTH = 2;
  public static final int DATA_REDUCTION_BY_RANGE = 3;
  public static final int DATA_REDUCTION_BY_ALTITUDE = 4;

  private float reductionBound0; // left azimuth, inner circle diameter, or minimum altitude
  private float reductionBound1; // right azimuth, outer circle diameter, or maximum altitude
  private float longitude;
  private float latitude;
  private float altitude; // km MSL
  private float unambiguousVelocity; // m/s
  private float unambiguousRange; // km
  private short nFrequencies;
  private short nPRTs;
  private float[] frequencies; // GHz
  private float[] PRTs; // ms
  private DoradePARM[] myPARMs;
  private DoradeCELV myCELV;
  private DoradeCFAC myCFAC;

  private int nCells;  // extracted from our CELV


  public DoradeRADD(RandomAccessFile file, boolean littleEndianData) throws DescriptorException {
    byte[] data = readDescriptor(file, littleEndianData, "RADD");

    //
    // unpack
    //
    radarName = new String(data, 8, 8, CDM.utf8Charset).trim();
    radarConstant = grabFloat(data, 16);
    peakPower = grabFloat(data, 20);
    noisePower = grabFloat(data, 24);
    rcvrGain = grabFloat(data, 28);
    antennaGain = grabFloat(data, 32);
    systemGain = grabFloat(data, 36);
    hBeamWidth = grabFloat(data, 40);
    vBeamWidth = grabFloat(data, 44);
    radarTypeNdx = grabShort(data, 48);
    scanMode = scanModeTable[grabShort(data, 50)];
    rotVelocity = grabFloat(data, 52);
    scanParam0 = grabFloat(data, 56);
    scanParam1 = grabFloat(data, 60);
    nParams = grabShort(data, 64);
    nAdditionalDescriptors = grabShort(data, 66);
    compressionScheme = grabShort(data, 68);
    dataReductionMethod = grabShort(data, 70);
    reductionBound0 = grabFloat(data, 72);
    reductionBound1 = grabFloat(data, 76);
    longitude = grabFloat(data, 80);
    latitude = grabFloat(data, 84);
    altitude = grabFloat(data, 88);
    unambiguousVelocity = grabFloat(data, 92);
    unambiguousRange = grabFloat(data, 96);
    nFrequencies = grabShort(data, 100);
    nPRTs = grabShort(data, 102);

    frequencies = new float[5];
    for (int i = 0; i < 5; i++)
      frequencies[i] = grabFloat(data, 104 + 4 * i);

    PRTs = new float[5];
    for (int i = 0; i < 5; i++)
      PRTs[i] = grabFloat(data, 124 + 4 * i);

    //
    // debugging output
    //
    if (verbose)
      System.out.println(this);

    myPARMs = new DoradePARM[nParams];
    for (int i = 0; i < nParams; i++)
      myPARMs[i] = new DoradePARM(file, littleEndianData, this);

    //
    // Find the cell vector (CELV or CSFD)
    //
    try {
      long startpos = file.getFilePointer();
      //  try
      //   {
      String dName = peekName(file);
      switch (dName) {
        case "CELV":
          myCELV = new DoradeCELV(file, littleEndianData);
          break;
        case "CSFD":
          file.seek(startpos);
          myCELV = new DoradeCSFD(file, littleEndianData);
          break;
        default:
          throw new DescriptorException("Expected " + dName + " descriptor not found!");
      }
      //    } catch (DescriptorException ex) {
      //    file.seek(startpos);
      //	myCELV = new DoradeCSFD(file, littleEndianData);
      //   }
    } catch (IOException ioex) {
      throw new DescriptorException(ioex);
    }

    nCells = myCELV.getNCells();

    if (nAdditionalDescriptors > (nParams + 1)) {
      myCFAC = new DoradeCFAC(file, littleEndianData);
    } else {
      myCFAC = null;
    }
  }

  @Override
  public String toString() {
    String sb = "DoradeRADD{" + "radarName='" + radarName + '\''
        + ", radarConstant=" + radarConstant
        + ", peakPower=" + peakPower
        + ", noisePower=" + noisePower
        + ", rcvrGain=" + rcvrGain
        + ", antennaGain=" + antennaGain
        + ", systemGain=" + systemGain
        + ", hBeamWidth=" + hBeamWidth
        + ", vBeamWidth=" + vBeamWidth
        + ", radarTypeNdx=" + radarTypeNdx
        + ", scanMode=" + scanMode
        + ", rotVelocity=" + rotVelocity
        + ", scanParam0=" + scanParam0
        + ", scanParam1=" + scanParam1
        + ", nParams=" + nParams
        + ", nAdditionalDescriptors=" + nAdditionalDescriptors
        + ", compressionScheme=" + compressionScheme
        + ", dataReductionMethod=" + dataReductionMethod
        + ", reductionBound0=" + reductionBound0
        + ", reductionBound1=" + reductionBound1
        + ", longitude=" + longitude
        + ", latitude=" + latitude
        + ", altitude=" + altitude
        + ", unambiguousVelocity=" + unambiguousVelocity
        + ", unambiguousRange=" + unambiguousRange
        + ", nFrequencies=" + nFrequencies
        + ", nPRTs=" + nPRTs
        + ", frequencies=" + Arrays.toString(frequencies)
        + ", PRTs=" + Arrays.toString(PRTs)
        + ", myPARMs=" + Arrays.toString(myPARMs)
        + ", myCELV=" + myCELV
        + ", myCFAC=" + myCFAC
        + ", nCells=" + nCells
        + '}';
    return sb;
  }

  /**
   * Get the compression scheme being used.
   *
   * @return the integer defining the compression scheme:
   * <ul><code>
   * <li>COMPRESSION_NONE
   * <li>COMPRESSION_HRD
   * </code></ul>
   */
  public int getCompressionScheme() {
    return compressionScheme;
  }

  /**
   * Get the number of cells in a ray.
   *
   * @return the number of cells in a ray
   */
  public int getNCells() {
    return nCells;
  }

  public DoradePARM[] getParamList() {
    return myPARMs;
  }

  public int getNParams() {
    return nParams;
  }

  public String getRadarName() {
    return radarName;
  }

  /**
   * Get radar latitude
   *
   * @return radar latitude in degrees
   */
  public float getLatitude() {
    return latitude;
  }

  /**
   * Get radar longitude
   *
   * @return radar longitude in degrees
   */
  public float getLongitude() {
    return longitude;
  }

  /**
   * Get radar altitude
   *
   * @return radar altitude in km MSL
   */
  public float getAltitude() {
    return altitude;
  }

  /**
   * Get the range to the first cell
   *
   * @return range to the first cell, in meters
   */
  public float getRangeToFirstCell() {
    return myCELV.getCellRanges()[0];
  }

  /**
   * Get the cell spacing.  An exception is thrown if the cell spacing
   * is not constant.
   *
   * @return the cell spacing, in meters
   * @throws DescriptorException if the cell spacing is not constant.
   */
  public float getCellSpacing() throws DescriptorException {
    float[] cellRanges = myCELV.getCellRanges();
    //
    // use the first cell spacing as our expected value
    //
    float cellSpacing = cellRanges[1] - cellRanges[0];
    //
    // Check the rest of the cells against the expected value, allowing
    // 1% fudge
    //
    for (int i = 2; i < cellRanges.length; i++) {
      float space = cellRanges[i] - cellRanges[i - 1];
      if (!Misc.nearlyEquals(space, cellSpacing) && (Math.abs(space / cellSpacing - 1.0) > 0.01)) {
        throw new DescriptorException("variable cell spacing");
      }
    }
    return cellSpacing;
  }

  /**
   * Get the scan mode
   *
   * @return The scan mode
   */
  public ScanMode getScanMode() {
    return scanMode;
  }

  // unidata added
  public float getUnambiguousVelocity() {
    return unambiguousVelocity;
  }

  // unidata added
  public float getunambiguousRange() {
    return unambiguousRange;
  }

  // unidata added
  public float getradarConstant() {
    return radarConstant;
  }

  // unidata added
  public float getrcvrGain() {
    return rcvrGain;
  }

  // unidata added
  public float getantennaGain() {
    return antennaGain;
  }

  // unidata added
  public float getsystemGain() {
    return systemGain;
  }

  // unidata added
  public float gethBeamWidth() {
    return hBeamWidth;
  }

  // unidata added
  public float getpeakPower() {
    return peakPower;
  }

  // unidata added
  public float getnoisePower() {
    return noisePower;
  }
}
