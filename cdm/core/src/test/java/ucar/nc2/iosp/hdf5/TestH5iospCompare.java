/* Copyright Unidata */
package ucar.nc2.iosp.hdf5;

import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.CompareNetcdf2.ObjFilter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/**
 * Compare objects in original N3iosp vs N3iospNew using builders.
 */
@Category(NeedsCdmUnitTest.class)
@RunWith(Parameterized.class)
public class TestH5iospCompare {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static String testDir = TestDir.cdmUnitTestDir + "/formats/hdf5";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> getTestParameters() {
    Collection<Object[]> filenames = new ArrayList<>();
    try {
      TestDir.actOnAllParameterized(testDir, (file) -> !file.getPath().endsWith(".xml"), filenames, true);
    } catch (IOException e) {
      filenames.add(new Object[] {e.getMessage()});
    }
    return filenames;
  }

  private String filename;

  public TestH5iospCompare(String filename) {
    this.filename = filename;
  }

  @Test
  public void compareWithBuilder() throws IOException {
    System.out.printf("TestBuilders on %s%n", filename);
    try (NetcdfFile org = NetcdfFile.open(filename)) {
      try (NetcdfFile withBuilder = NetcdfFiles.open(filename)) {
        Formatter f = new Formatter();
        CompareNetcdf2 compare = new CompareNetcdf2(f, false, false, true);
        if (!compare.compare(org, withBuilder, new DimensionsFilter())) {
          System.out.printf("Compare %s%n%s%n", filename, f);
          fail();
        }
      }
    }
  }

  // These files the new iosp does it ci=orrectly, so we need to skip comparing dimensions.
  static List<String> skipNames = ImmutableList.of("PR1B0000-2000101203_010_001.hdf",
      "MISR_AM1_AGP_P040_F01_24.subset.eos", "MISR_AM1_GRP_TERR_GM_P040_AN.eos",
      "AMSR_E_L2A_BrightnessTemperatures_V08_200801012345_A.hdf", "AMSR_E_L3_DailyLand_B04_20080101.hdf");

  static class DimensionsFilter implements ObjFilter {
    @Override
    public boolean attCheckOk(Variable v, Attribute att) {
      String name = att.getShortName();

      if (name.equalsIgnoreCase(CDM.ADD_OFFSET))
        return false;
      if (name.equalsIgnoreCase("offset"))
        return false;
      if (name.equalsIgnoreCase(CDM.SCALE_FACTOR))
        return false;
      if (name.equalsIgnoreCase("factor"))
        return false;
      if (name.equalsIgnoreCase("unit"))
        return false;
      if (name.equalsIgnoreCase(CDM.UNITS))
        return false;

      return true;
    }

    @Override
    public boolean varDataTypeCheckOk(Variable v) {
      return true;
    }

    @Override
    public boolean checkDimensionsForFile(String filename) {
      for (String name : skipNames) {
        if (filename.endsWith(name))
          return false;
      }
      return true;
    }
  }
}
