/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;

/** Test NetcdfDatasets.openDataset() of NcML files. */
public class TestNcmlEquals {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testEquals() throws IOException {
    testEquals("file:" + TestNcmlRead.topDir + "testEquals.xml");
    testEnhanceEquals("file:" + TestNcmlRead.topDir + "testEqualsEnhance.xml");
  }

  public void problem() throws IOException {
    // testEnhanceEquals("file:G:/zg500_MM5I_1979010103.ncml");
    testEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
    testEnhanceEquals(
        "file:R:/testdata/2008TrainingWorkshop/tds/knmi/RAD___TEST_R_C_NL25PCP_L___20080720T000000_200807201T015500_0001_resampledto256x256.ncml");
  }

  private void testEquals(String ncmlLocation) throws IOException {
    try (NetcdfDataset ncd = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
      String locref = ncd.getReferencedFile().getLocation();
      try (NetcdfDataset ncdref = NetcdfDatasets.openDataset(locref, false, null)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  private void testEnhanceEquals(String ncmlLocation) throws IOException {
    try (NetcdfDataset ncd = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
      String locref = ncd.getReferencedFile().getLocation();
      try (NetcdfDataset ncdref = NetcdfDatasets.openDataset(locref, true, null)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, false);
        boolean ok = compare.compare(ncd, ncdref, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  public static class NcmlFilter implements FileFilter {

    @Override
    public boolean accept(File pathname) {
      String name = pathname.getName();
      // Made to fail, so skip
      if (name.contains("aggExistingInequivalentCals.xml"))
        return false;
      // NcMLReader does not change variable to type int, so fails.
      if (name.contains("aggSynthetic.xml"))
        return false;
      // Bug in old reader
      if (name.contains("testStandaloneNoEnhance.ncml"))
        return false;
      if (name.contains("AggFmrc"))
        return false; // not implemented
      if (name.endsWith("ml"))
        return true; // .xml or .ncml
      return false;
    }
  }

  public static class CoordsObjFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder) && !att.getShortName().equals(CDM.NCPROPERTIES);
    }

    // override att comparision if needed
    public boolean attsAreEqual(Attribute att1, Attribute att2) {
      if (att1.getShortName().equalsIgnoreCase(CDM.UNITS) && att2.getShortName().equalsIgnoreCase(CDM.UNITS)) {
        return att1.getStringValue().equals(att2.getStringValue());
      }
      return att1.equals(att2);
    }

  }
}
