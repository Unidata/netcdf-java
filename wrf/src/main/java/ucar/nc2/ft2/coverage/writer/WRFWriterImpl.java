/*
 * Copyright (c) 2019 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft2.coverage.writer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import ucar.nc2.ft2.coverage.Coverage;

public class WRFWriterImpl implements IWRFWriter {

  /**
   *
   * @param coverages Selected coverages (variables) from a GRIB dataset, written in the same format at that produced by the UNGRIB program.
   * @param mapSource Source of the originating or generating Center.
   * @param isWindEarthRel Set to false.
   * @return A WRF intermediate format file based on the coverages.
   *
   * This method is designed for use with TDS, where the returned file is packaged within an http response.
   */
  @Override
  public java.io.OutputStream writeStream(ArrayList<Coverage> coverages, String mapSource, boolean isWindEarthRel) {

    OutputStream out = new OutputStream() {
      @Override
      public void write(int b) throws IOException {

      }
    };
    return out;
  }
  /**
   *
   * @param coverages Selected coverages (variables) from a GRIB dataset, written in the same format at that produced by the UNGRIB program.
   * @param mapSource  Source of the originating or generating Center.
   * @param isWindEarthRel Should be set to false.
   * @param outputFileName  Name and full path for the WRF intermediate format output file.
   *
   * This method is designed for use with the NcWRFWriter command line tool.
   */
//  @Override
  public void writeFile(ArrayList<Coverage> coverages, String mapSource, boolean isWindEarthRel, File outputFileName) {

    StringBuilder testString = new StringBuilder("**** WRF WRITER COMMAND LINE TEST ****\n The map source info is:\n");
    testString.append(mapSource);
    testString.append("\n");
    testString.append(String.format("\nThere are %s coverages in the file\n",coverages.size()));

    String header = testString.toString();
    byte buf[] = header.getBytes();

    try (FileOutputStream outFile = new FileOutputStream(outputFileName)) {

      outFile.write(buf);

      for ( Coverage c : coverages) {
        outFile.write(c.toString().getBytes());
      }

    } catch (IOException e) {
        e.getMessage();
    }
  }
}
