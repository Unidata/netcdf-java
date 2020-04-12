/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.ncml;

import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileFilter;
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
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.internal.ncml.NcMLReaderNew;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import ucar.unidata.util.test.TestDir;

/**
 * Compare NcmlReader and NcmlReaderNew
 */
@RunWith(Parameterized.class)
public class TestNcmlReadersCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(TestNcMLRead.topDir, new NcmlFilter(), filenames, true);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private String ncmlLocation;

  public TestNcmlReadersCompare(String filename) {
    this.ncmlLocation = "file:" + filename;
  }

  @Test
  public void compareReaders() {
    logger.info("TestNcmlReaders on {}%n", ncmlLocation);
    System.out.printf("Compare %s%n", ncmlLocation);
    try (NetcdfDataset org = NcMLReader.readNcML(ncmlLocation, null)) {
      try (NetcdfDataset withBuilder = NcMLReaderNew.readNcML(ncmlLocation, null, null).build()) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder, new CoordsObjFilter())) {
          System.out.printf("Compare %s%n%s%n", ncmlLocation, f);
          fail();
        }
      }
    } catch (IOException e) {
      fail();
    }
  }

  private static class NcmlFilter implements FileFilter {

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
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder);
    }
  }

}

