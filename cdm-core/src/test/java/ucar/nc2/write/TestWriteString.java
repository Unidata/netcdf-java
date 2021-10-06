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
import ucar.array.ArrayType;
import ucar.nc2.Attribute;
import ucar.nc2.constants.CDM;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestWriteString {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private static final String variableName = "dataVar";
  private static final String units = "units";

  private static final String latVar = "lat";
  private static final String lonVar = "lon";
  private static final String timeVar = "time";
  private static final String unitsAttName = "units";
  private static final String axisAttName = "axis";
  private static final String standardNameAttName = "standard_name";
  private static final String longNameAttName = CDM.LONG_NAME;
  private static final String missingValueAttName = CDM.MISSING_VALUE;
  private static final String fillValueAttName = "_FillValue";

  private void defineHeader(NetcdfFormatWriter.Builder writerb, String timeDim, String latDim, String lonDim,
      String dim3) {
    writerb.addVariable(latVar, ArrayType.FLOAT, latDim).addAttribute(new Attribute(unitsAttName, "degrees_north"))
        .addAttribute(new Attribute(axisAttName, "Y")).addAttribute(new Attribute(standardNameAttName, "latitude"));
    // could add bounds, but not familiar how it works

    writerb.addVariable(lonVar, ArrayType.FLOAT, lonDim).addAttribute(new Attribute(unitsAttName, "degrees_east"))
        .addAttribute(new Attribute(axisAttName, "X")).addAttribute(new Attribute(standardNameAttName, "longitude"));
    // could add bounds, but not familiar how it works

    writerb.addVariable(variableName, ArrayType.FLOAT, dim3).addAttribute(new Attribute(longNameAttName, variableName))
        .addAttribute(new Attribute(unitsAttName, units));

    writerb.addVariable("cellId", ArrayType.CHAR, "lat lon") // STRING illegal change to CHAR
        .addAttribute(new Attribute(longNameAttName, "Cell ID"));

    writerb.addVariable(timeVar, ArrayType.INT, timeDim).addAttribute(new Attribute(axisAttName, "T"))
        .addAttribute(new Attribute(standardNameAttName, timeVar))
        .addAttribute(new Attribute(longNameAttName, timeVar));
  }

  private void createTimeLatLonDataCube(String filename, double[] latitudes, double[] longitudes) throws IOException {
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf3(filename);

    // define dimensions, including unlimited
    writerb.addDimension(latVar, latitudes.length);
    writerb.addDimension(lonVar, longitudes.length);
    writerb.addUnlimitedDimension(timeVar);

    // define Variables
    defineHeader(writerb, timeVar, latVar, lonVar, timeVar + " " + latVar + " " + lonVar);

    // create and write the file
    try (NetcdfFormatWriter writer = writerb.build()) {
      // empty
    }
  }

  // this was succeeding, but it shoulnt - now fails in 4.0.26
  @Test
  public void testWrite() throws IOException {
    TestWriteString test = new TestWriteString();
    File tempFile = tempFolder.newFile();
    test.createTimeLatLonDataCube(tempFile.getPath(), new double[] {1, 2}, new double[] {10, 20, 30, 40});
  }
}
