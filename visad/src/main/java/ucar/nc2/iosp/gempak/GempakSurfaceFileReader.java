/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */


package ucar.nc2.iosp.gempak;


import ucar.unidata.io.RandomAccessFile;
import ucar.unidata.util.Format;
import ucar.unidata.util.StringUtil2;

import java.io.*;

import java.util.List;


/**
 * Read a Gempak surface file
 */
public class GempakSurfaceFileReader extends AbstractGempakStationFileReader {

  /**
   * Surface Text identifier
   */
  private static final String SFTX = "SFTX";

  /**
   * Surface Data identifier
   */
  static final String SFDT = "SFDT";

  /**
   * Surface Data identifier
   */
  public static final String SFSP = "SFSP";

  /**
   * standard surface file id
   */
  static final String STANDARD = "standard";

  /**
   * climate surface file id
   */
  private static final String CLIMATE = "climate";

  /**
   * ship surface file id
   */
  static final String SHIP = "ship";

  /**
   * Default ctor
   */
  GempakSurfaceFileReader() {
  }

  /**
   * Initialize the file, read in all the metadata (ala DM_OPEN)
   *
   * @param raf       RandomAccessFile to read.
   * @param fullCheck if true, check entire structure
   * @return A GempakSurfaceFileReader
   * @throws IOException problem reading file
   */
  public static GempakSurfaceFileReader getInstance(RandomAccessFile raf,
                                                    boolean fullCheck)
          throws IOException {
    GempakSurfaceFileReader gsfr = new GempakSurfaceFileReader();
    gsfr.init(raf, fullCheck);
    return gsfr;
  }

  /**
   * Initialize this reader.  Read all the metadata
   *
   * @return true if successful
   * @throws IOException problem reading the data
   */
  protected boolean init() throws IOException {
    return init(true);
  }

  /**
   * Initialize this reader.  Get the Grid specific info
   *
   * @param fullCheck check to make sure there are grids we can handle
   * @return true if successful
   * @throws IOException problem reading the data
   */
  protected boolean init(boolean fullCheck) throws IOException {

    if (!super.init(fullCheck)) {
      return false;
    }

    // Modeled after SF_OFIL
    if (dmLabel.kftype != MFSF) {
      logError("not a surface data file ");
      return false;
    }

    String partType = ((dmLabel.kfsrce == 100) && (dmLabel.kprt == 1))
            ? SFTX
            : SFDT;

    DMPart part = getPart(partType);

    if (part == null) {
      logError("No part named " + partType + " found");
      return false;
    }

    if (!readStationsAndTimes(true)) {
      logError("Unable to read stations and times");
      return false;
    }
    // since the reads are ob by ob, set buffer size small
    if (subType.equals(STANDARD)) rf.setBufferSize(256);
    return true;

  }

  /**
   * Make the list of dates.  Override superclass to make the
   * value based on the subtype
   *
   * @param uniqueTimes true to make a unique list
   * @return the list of times
   */
  protected List<String> makeDateList(boolean uniqueTimes) {
    return super.makeDateList(!getFileSubType().equals(SHIP));
  }

  /**
   * Set the file subType.
   */
  protected void makeFileSubType() {
    // determine file type
    Key key = findKey(GempakStation.SLAT);
    if (key == null)
      throw new IllegalStateException("File does not have key="+GempakStation.SLAT);
    String latType = key.type;
    Key dateKey = findKey(DATE);
    if (dateKey != null && !dateKey.type.equals(latType)) {
      if (latType.equals(ROW)) {
        subType = CLIMATE;
      } else {
        subType = STANDARD;
      }
    } else {
      subType = SHIP;
    }
  }

  /**
   * Print the list of dates in the file
   *
   * @param row ob row
   * @param col ob column
   */
  public void printOb(int row, int col) {
    int stnIndex = (getFileSubType().equals(CLIMATE))
            ? row
            : col;
    List<GempakStation> stations = getStations();
    if (stations.isEmpty() || stnIndex > stations.size()) {
      System.out.println("\nNo data available");
      return;
    }
    GempakStation station = getStations().get(stnIndex - 1);
    StringBuilder builder = new StringBuilder();
    builder.append("\nStation:\n");
    builder.append(station.toString());
    builder.append("\nObs\n\t");
    List<GempakParameter> params = getParameters(SFDT);
    for (GempakParameter parm : params) {
      builder.append(StringUtil2.padLeft(parm.getName(), 7));
      builder.append("\t");
    }
    builder.append("\n");
    RData rd;
    try {
      rd = DM_RDTR(row, col, SFDT);
    } catch (IOException ioe) {
      ioe.printStackTrace();
      rd = null;
    }
    if (rd == null) {
      builder.append("No Data Available");
    } else {
      builder.append("\t");
      float[] data = rd.data;
      for (float datum : data) {
        builder.append(StringUtil2.padLeft(Format.formatDouble(datum, 7, 1), 7));
        builder.append("\t");
      }
      int[] header = rd.header;
      if (header.length > 0) {
        builder.append("\nOb Time = ");
        builder.append(header[0]);
      }
    }
    System.out.println(builder.toString());
  }


  /**
   * Get the type for this file
   *
   * @return file type (CLIMATE, STANDARD, SHIP)
   */
  public String getSurfaceFileType() {
    return getFileSubType();
  }

}

