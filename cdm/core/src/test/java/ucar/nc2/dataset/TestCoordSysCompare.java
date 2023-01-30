/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.Variable;
import ucar.nc2.constants._Coordinate;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import ucar.unidata.util.test.TestDir;

/** Compare CoordSysBuilder (new) and CoordSystemBuilderImpl (old) in cdmLocalTestDataDir */
@RunWith(Parameterized.class)
public class TestCoordSysCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(TestDir.cdmLocalTestDataDir,
          pathname -> pathname.getName().endsWith("nc") && !pathname.getName().startsWith("cfrad."), filenames, true);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private String fileLocation;

  public TestCoordSysCompare(String filename) {
    this.fileLocation = "file:" + filename;
  }

  @Test
  public void compareCoordSysBuilders() throws IOException {
    System.out.printf("Compare %s%n", fileLocation);
    logger.info("TestCoordSysCompare on {}%n", fileLocation);
    try (NetcdfDataset org = NetcdfDataset.openDataset(fileLocation)) {
      try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(fileLocation)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        boolean ok = compare.compare(org, withBuilder, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        System.out.printf("org = %s%n", org.getRootGroup().findAttributeString(_Coordinate._CoordSysBuilder, ""));
        System.out.printf("new = %s%n",
            withBuilder.getRootGroup().findAttributeString(_Coordinate._CoordSysBuilder, ""));
        assertThat(ok).isTrue();
      }
    }
  }

  public static class CoordsObjFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder);
    }
  }

}


