/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ncml.TestNcMLRead;
import ucar.nc2.ncml.TestNcmlReadersCompare;
import ucar.nc2.util.CompareNetcdf2;

/** Test netcdf dataset in the JUnit framework. */
public class TestNcMLequals {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testEquals() throws IOException {
    testEquals("file:" + TestNcMLRead.topDir + "testEquals.xml");
    testEnhanceEquals("file:" + TestNcMLRead.topDir + "testEqualsEnhance.xml");
  }

  public void problem() throws IOException {
    // testEnhanceEquals("file:G:/zg500_MM5I_1979010103.ncml");
    testEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
    testEnhanceEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
  }

  private void testEquals(String ncmlLocation) throws IOException {
    try (NetcdfDataset ncd = NcmlReader.readNcML(ncmlLocation, null, null).build()) {
      String locref = ncd.getReferencedFile().getLocation();
      try (NetcdfDataset ncdref = NetcdfDatasets.openDataset(locref, false, null)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new TestNcmlReadersCompare.CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  private void testEnhanceEquals(String ncmlLocation) throws IOException {
    try (NetcdfDataset ncd = NcmlReader.readNcML(ncmlLocation, null, null).build()) {
      String locref = ncd.getReferencedFile().getLocation();
      try (NetcdfDataset ncdref = NetcdfDatasets.openDataset(locref, true, null)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new TestNcmlReadersCompare.CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }
}
