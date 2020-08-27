package examples.tutorials;

import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import java.io.IOException;

public class NCTutorial {
  // logs error/info message in memory and can be accessed from test functions
  static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

  ////////////////////////////
  // constant string messages for tests
  static final String yourOpenNetCdfFileErrorMsgTxt = "exception while opening Netcdf file";

  ////////////////////////////
  // Netcdf tutorial functions
  // NOTE: these functions are used in the NetCdf tutorial docs, so formatting matters!

  /**
   * Code snippet to open a netcdf file and log exceptions
   *
   * @param pathToYourFileAsStr: relative path to locally stored file
   */
  public static void openNCFileTutorial(String pathToYourFileAsStr) {
    try (NetcdfFile ncfile = NetcdfFiles.open(pathToYourFileAsStr)) {
      // Do cool stuff here
    } catch (IOException ioe) {
      // Handle less-cool exceptions here
      logger.log(yourOpenNetCdfFileErrorMsgTxt, ioe);
    }
  }
}
