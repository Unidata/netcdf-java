/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare CoordSysBuilder (new) and CoordSystemBuilderImpl (old) in cdmUnitTestDir */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestCoordSysCompareMore {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String convDir = TestDir.cdmUnitTestDir + "/conventions";
  private static List<String> otherDirs =
      ImmutableList.of(TestDir.cdmUnitTestDir + "/ft", TestDir.cdmUnitTestDir + "/cfPoint");
  private static List<String> hdfDirs =
      ImmutableList.of(TestDir.cdmUnitTestDir + "/formats/hdf4/", TestDir.cdmUnitTestDir + "/formats/hdf5/");

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(convDir,
          (file) -> !file.getPath().endsWith(".pdf") && !file.getName().startsWith("cfrad."), filenames, true);
      for (String dir : otherDirs) {
        TestDir.actOnAllParameterized(dir, (file) -> file.getName().endsWith(".nc"), filenames, true);
      }
      for (String dir : hdfDirs) {
        TestDir.actOnAllParameterized(dir, TestCoordSysCompareMore::skipHdf, filenames, true);
      }
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private static List<String> skippers = ImmutableList.of(
      // Skip these because old code doesnt push Dims up properly.
      "PR1B0000-2000101203_010_001.hdf", "misr", "MISR_AM1_AGP_P040_F01_24.subset.eos",
      "AMSR_E_L3_DailyLand_B04_20080101.hdf",
      // transforms
      "MOD13Q1.A2012321.h00v08.005.2012339011757.hdf", "MOD10A1.A2008001.h23v15.005.2008003161138.hdf",
      // dont have group names right
      "AMSR_E_L2_Land_T06_200801012345_A.hdf", "AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf",
      // anon dimensions
      "MOD02OBC.A2007001.0005.005.2007307210540.hdf",
      // attributes changed, see DimensionsFilter in H4 and 5 iosp compare
      "2006166131201_00702_CS_2B-GEOPROF_GRANULE_P_R03_E00.hdf",
      // problem with filename
      "Europe_MSG1_8bit_HRV_OF_21-NOV-2003_06%3A00%3A04.171.H5");

  private static boolean skipHdf(File file) {
    for (String skip : skippers) {
      if (file.getPath().contains(skip)) {
        return false;
      }
    }
    return !file.getName().endsWith(".xml");
  }

  private String fileLocation;

  public TestCoordSysCompareMore(String filename) {
    this.fileLocation = "file:" + filename;
  }

  @Test
  public void compareCoordSysBuilders() throws IOException {
    System.out.printf("Compare %s%n", fileLocation);
    logger.info("TestCoordSysCompare on {}%n", fileLocation);
    try (NetcdfDataset org = NetcdfDataset.openDataset(fileLocation)) {
      try (NetcdfDataset withBuilder = NetcdfDatasets.openDataset(fileLocation)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f);
        boolean ok = compare.compare(org, withBuilder, new CoordsObjFilter());
        System.out.printf("%s %s%n", ok ? "OK" : "NOT OK", f);
        System.out.printf("org = %s%n", org.getRootGroup().findAttValueIgnoreCase(_Coordinate._CoordSysBuilder, ""));
        System.out.printf("new = %s%n",
            withBuilder.getRootGroup().findAttValueIgnoreCase(_Coordinate._CoordSysBuilder, ""));
        assertThat(ok).isTrue();
      }
    }
  }

  public static class CoordsObjFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      return !att.getShortName().equals(_Coordinate._CoordSysBuilder);
    }

    @Override
    public boolean compareCoordinateTransform(CoordinateTransform ct1, CoordinateTransform ct2) {
      return ct2.getName().startsWith(ct1.getName());
    }
  }

}


