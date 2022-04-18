/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.write;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.constants.CDM;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test create a new netCDF file with a "record structure" */
public class TestWriteRecordStructure {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testWriteRecordStructure() throws IOException, InvalidRangeException {
    String fileName = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(fileName).setFill(false);
    writerb.addUnlimitedDimension("time");
    Dimension latDim = writerb.addDimension("lat", 3);
    Dimension lonDim = writerb.addDimension("lon", 4);

    // define Variables
    writerb.addVariable("lat", DataType.FLOAT, "lat").addAttribute(new Attribute("units", "degrees_north"));

    writerb.addVariable("lon", DataType.FLOAT, "lon").addAttribute(new Attribute("units", "degrees_east"));

    writerb.addVariable("rh", DataType.INT, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "relative humidity"))
        .addAttribute(new Attribute("units", "percent"));

    writerb.addVariable("T", DataType.DOUBLE, "time lat lon")
        .addAttribute(new Attribute(CDM.LONG_NAME, "surface temperature")).addAttribute(new Attribute("units", "degC"));

    writerb.addVariable("time", DataType.INT, "time").addAttribute(new Attribute("units", "hours since 1990-01-01"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // write out the non-record variables
      writer.write("lat", Array.makeFromJavaArray(new float[] {41, 40, 39}, false));
      writer.write("lon", Array.makeFromJavaArray(new float[] {-109, -107, -105, -103}, false));

      //// heres where we write the record variables

      // different ways to create the data arrays. Note the outer dimension has shape 1.
      ArrayInt rhData = new ArrayInt.D3(1, latDim.getLength(), lonDim.getLength(), false);
      ArrayDouble.D3 tempData = new ArrayDouble.D3(1, latDim.getLength(), lonDim.getLength());
      Array timeData = Array.factory(DataType.INT, new int[] {1});

      int[] origin = new int[] {0, 0, 0};
      int[] time_origin = new int[] {0};

      // loop over each record
      for (int time = 0; time < 10; time++) {
        // make up some data for this record, using different ways to fill the data arrays.
        timeData.setInt(timeData.getIndex(), time * 12);

        Index ima = rhData.getIndex();
        for (int lat = 0; lat < latDim.getLength(); lat++) {
          for (int lon = 0; lon < lonDim.getLength(); lon++) {
            rhData.setInt(ima.set(0, lat, lon), time * lat * lon);
            tempData.set(0, lat, lon, time * lat * lon / 3.14159);
          }
        }

        // write the data out for this record
        time_origin[0] = time;
        origin[0] = time;

        writer.write("rh", origin, rhData);
        writer.write("T", origin, tempData);
        writer.write("time", time_origin, timeData);
      }
    }
  }
}
