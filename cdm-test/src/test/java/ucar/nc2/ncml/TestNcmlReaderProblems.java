/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.ncml.NcmlReader;
import ucar.nc2.internal.util.CompareArrayToArray;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.nc2.internal.util.CompareNetcdf2.ObjFilter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare NetcdfDatasets.openDataset with NcmlReader.readNcml on specific problem datasets. */
@Category(NeedsCdmUnitTest.class)
public class TestNcmlReaderProblems {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void problem() throws IOException {
    // This has self contained data
    // compare("file:" + TestCFPointDatasets.CFpointObs_topdir + "stationData2Levels.ncml");
    // compareDS("file:" + TestCFPointDatasets.CFpointObs_topdir + "stationData2Levels.ncml");

    // compare("file:" + TestDir.cdmLocalTestDataDir + "cfDocDsgExamples/H.2.1.1.ncml");
    compare("file:" + TestDir.cdmLocalTestDataDir + "point//stationData2Levels.ncml");
    // compareVarData("file:" + TestDir.cdmLocalTestDataDir + "point//stationData2Levels.ncml", "trajectory");
  }

  private void compare(String ncmlLocation) throws IOException {
    System.out.printf("Compare NcMLReader.readNcML %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NetcdfDatasets.openDataset(ncmlLocation, false, null)) {
      System.out.printf("NcMLReader == %s%n", org);
      try (NetcdfDataset withBuilder = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
        System.out.printf("NcMLReaderNew == %s%n", withBuilder);
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  private void compareVarData(String ncmlLocation, String varName) throws IOException {
    System.out.printf("Compare NcMLReader.readNcML %s%n", ncmlLocation);
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    try (NetcdfDataset org = NetcdfDatasets.openDataset(ncmlLocation, false, null)) {
      Variable v = org.findVariable(varName);
      assert v != null;
      try (NetcdfDataset withBuilder = NcmlReader.readNcml(ncmlLocation, null, null).build()) {
        Variable vb = withBuilder.findVariable(varName);
        assert vb != null;
        boolean ok = CompareArrayToArray.compareData(varName, v.readArray(), vb.readArray());
        System.out.printf("%s%n", ok ? "OK" : "NOT OK");
        assertThat(ok).isTrue();
      }
    }
  }

  private void compareDS(String ncmlLocation) throws IOException {
    System.out.printf("Compare NetcdfDataset.openDataset %s%n", ncmlLocation);
    try (NetcdfDataset org = NetcdfDatasets.openDataset(ncmlLocation)) {
      try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(ncmlLocation)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        assertThat(ok).isTrue();
      }
    }
  }

  public static class CoordsObjFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Attribute att) {
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
