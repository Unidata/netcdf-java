/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;

import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;

public class TestUtils {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static final String gcdmPrefix = "gcdm://localhost:16111/";

  static void compareFiles(String filename) throws Exception {
    String gcdmUrl = gcdmPrefix + filename;
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Formatter formatter = new Formatter();
      boolean ok = CompareNetcdf2.compareFiles(ncfile, gcdmFile, formatter, true, true, true);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }

  static void compareVariables(String filename, String varName) throws Exception {
    String gcdmUrl = gcdmPrefix + filename;
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(filename, null);
        GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {

      Variable ncVar = ncfile.findVariable(varName);
      Variable gcdmVar = gcdmFile.findVariable(varName);

      Formatter formatter = new Formatter();
      CompareNetcdf2 compareNetcdf2 = new CompareNetcdf2(formatter, false, true, true);
      boolean ok = compareNetcdf2.compareVariable(ncVar, gcdmVar, CompareNetcdf2.IDENTITY_FILTER);
      logger.debug(formatter.toString());
      assertThat(ok).isTrue();
    }
  }
}
