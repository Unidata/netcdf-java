package ucar.nc2.ft.point;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.NetcdfFile;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Synthetic (Ncml) datasets for testing point feature variants.
 *
 * @author cwardgar
 * @since 2014/07/08
 */
@RunWith(Parameterized.class)
public class TestCfDocDsgExamples {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String cfDocDsgExamplesDir = TestDir.cdmLocalFromTestDataDir + "cfDocDsgExamples/";

  private static List<Object[]> getPointDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.1.1.ncml", FeatureType.POINT, 12});

    return result;
  }

  private static List<Object[]> getStationDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.2.1.1.ncml", FeatureType.STATION, 50});
    result.add(new Object[] {"H.2.2.1.ncml", FeatureType.STATION, 130});
    result.add(new Object[] {"H.2.3.1.ncml", FeatureType.STATION, 5});
    // @Ignore("Dont support multiple lat/lon coordinates for now")
    // result.add(new Object[] { "H.2.3.2.ncml", FeatureType.STATION, 15 });

    result.add(new Object[] {"H.2.4.1.ncml", FeatureType.STATION, 100});
    result.add(new Object[] {"H.2.5.1.ncml", FeatureType.STATION, 30});

    return result;
  }

  private static List<Object[]> getProfileDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.3.1.1.ncml", FeatureType.PROFILE, 56});
    result.add(new Object[] {"H.3.3.1.ncml", FeatureType.PROFILE, 42});
    result.add(new Object[] {"H.3.4.1.ncml", FeatureType.PROFILE, 37});
    result.add(new Object[] {"H.3.5.1.ncml", FeatureType.PROFILE, 37});

    return result;
  }

  private static List<Object[]> getTrajectoryDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.4.1.1.ncml", FeatureType.TRAJECTORY, 24});
    result.add(new Object[] {"H.4.2.1.ncml", FeatureType.TRAJECTORY, 42});
    result.add(new Object[] {"H.4.3.1.ncml", FeatureType.TRAJECTORY, 75});
    result.add(new Object[] {"H.4.4.1.ncml", FeatureType.TRAJECTORY, 75});

    return result;
  }

  private static List<Object[]> getStationProfileDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.5.1.1.ncml", FeatureType.STATION_PROFILE, 120});
    result.add(new Object[] {"H.5.1.2.ncml", FeatureType.STATION_PROFILE, 60});
    result.add(new Object[] {"H.5.2.1.ncml", FeatureType.STATION_PROFILE, 20});
    result.add(new Object[] {"H.5.3.1.ncml", FeatureType.STATION_PROFILE, 145});

    return result;
  }

  private static List<Object[]> getSectionDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {"H.6.1.1.ncml", FeatureType.TRAJECTORY_PROFILE, 120});
    result.add(new Object[] {"H.6.2.1.ncml", FeatureType.TRAJECTORY_PROFILE, 20});
    result.add(new Object[] {"H.6.3.1.ncml", FeatureType.TRAJECTORY_PROFILE, 145});

    return result;
  }

  @Parameterized.Parameters(name = "{0}") // Name the tests after the location.
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(getPointDatasets());
    result.addAll(getStationDatasets());
    result.addAll(getProfileDatasets());
    result.addAll(getTrajectoryDatasets());
    result.addAll(getStationProfileDatasets());
    result.addAll(getSectionDatasets());

    return result;
  }

  private final String location;
  private final FeatureType ftype;
  private final int countExpected;
  private final boolean show = true;

  public TestCfDocDsgExamples(String location, FeatureType ftype, int countExpected) {
    this.location = cfDocDsgExamplesDir + location;
    this.ftype = ftype;
    this.countExpected = countExpected;
    System.out.printf("Writing file %s%n", this.location);
  }

  @Test
  public void checkPointDataset() throws IOException {
    Assert.assertEquals("npoints", countExpected, TestPointDatasets.checkPointFeatureDataset(location, ftype, show));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Convert all NCML files in cfDocDsgExamplesDir to NetCDF-3 files.
  public static void main(String[] args) throws IOException {
    File examplesDir = new File("cdm/core/src/test/data/cfDocDsgExamples/");
    File convertedDir = new File(examplesDir, "converted");
    convertedDir.mkdirs();

    System.out.printf("Writing to dir %s%n", examplesDir.getAbsolutePath());
    File[] files = examplesDir.listFiles();

    for (File inputFile : examplesDir.listFiles(new NcmlFilenameFilter())) {
      String inputFilePath = inputFile.getCanonicalPath();
      String outputFileName = inputFile.getName().substring(0, inputFile.getName().length() - 2);
      String outputFilePath = new File(convertedDir, outputFileName).getCanonicalPath();
      System.out.printf("Writing %s to %s.%n", inputFilePath, outputFilePath);

      try (NetcdfFile ncfileIn = NetcdfDatasets.openFile(inputFilePath, null)) {
        NetcdfFormatWriter.Builder builder = NetcdfFormatWriter.createNewNetcdf3(outputFilePath);
        NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder);
        try (NetcdfFile ncout2 = copier.write(null)) {
          // empty
        }
      }
    }
  }

  private static class NcmlFilenameFilter implements FilenameFilter {
    @Override
    public boolean accept(File dir, String name) {
      return name.endsWith("ncml");
    }
  }
}
