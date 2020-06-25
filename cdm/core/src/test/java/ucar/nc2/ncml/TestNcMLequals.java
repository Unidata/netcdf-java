/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import java.util.Formatter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import ucar.nc2.util.CompareNetcdf2;

/** Test netcdf dataset in the JUnit framework. */
public class TestNcMLequals {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testEquals() throws IOException {
    testEquals("file:" + TestNcMLRead.topDir + "testEquals.xml");
    testEnhanceEquals("file:" + TestNcMLRead.topDir + "testEquals.xml");
  }

  public void problem() throws IOException {
    // testEnhanceEquals("file:G:/zg500_MM5I_1979010103.ncml");
    testEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
    testEnhanceEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
  }

  private void testEquals(String ncmlLocation) throws IOException {
    System.out.println("testEquals");
    try (NetcdfDataset ncd = NcMLReader.readNcML(ncmlLocation, null);
        NetcdfDataset ncdref = NetcdfDataset.openDataset(ncd.getReferencedFile().getLocation(), false, null)) {
      Assert.assertTrue(CompareNetcdf2.compareFiles(ncd, ncdref, new Formatter(), false, false, false));
    }
  }

  private void testEnhanceEquals(String ncmlLocation) throws IOException {
    System.out.println("testEnhanceEquals");
    try (NetcdfDataset ncml = NcMLReader.readNcML(ncmlLocation, null);
        NetcdfDataset ncmlEnhanced = new NetcdfDataset(ncml, true)) {

      String locref = ncml.getReferencedFile().getLocation();
      NetcdfDataset ncdrefEnhanced = NetcdfDataset.openDataset(locref, true, null);

      Assert
          .assertTrue(CompareNetcdf2.compareFiles(ncmlEnhanced, ncdrefEnhanced, new Formatter(), false, false, false));
    }
  }



}
