package examples.cdmdatasets;

import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.unidata.util.test.TestLogger;

import java.io.IOException;
import java.util.Set;

public class NetcdfDatasetTutorial {
  // logs error/info message in memory and can be accessed from test functions
  public static TestLogger logger = TestLogger.TestLoggerFactory.getLogger();

  ////////////////////////////
  // constant string messages for tests
  public static final String yourOpenNetCdfFileErrorMsgTxt = "exception while opening Netcdf file";

  ////////////////////////////
  // Netcdf tutorial functions
  // NOTE: these functions are used in the Netcdf tutorial docs, so formatting matters!

  /**
   * Tutorial code snippet to open a netcdf file and log exceptions
   * 
   * @param pathToYourFileAsStr: relative path to locally stored file
   */
  public static void openNCFile(String pathToYourFileAsStr) {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile(pathToYourFileAsStr, null)) {
      // Do cool stuff here
    } catch (IOException ioe) {
      // Handle less-cool exceptions here
      logger.log(yourOpenNetCdfFileErrorMsgTxt, ioe);
    }
  }

  /**
   * Tutorial code snippet to open an enhanced NetcdfDataset
   * 
   * @param pathToYourDatasetAsStr: relative path to locally stored file
   */
  public static void openEnhancedDataset(String pathToYourDatasetAsStr) {
    try (NetcdfDataset ncf = NetcdfDatasets.openDataset(pathToYourDatasetAsStr)) {
      // Do cool stuff here
    } catch (IOException ioe) {
      // Handle less-cool exceptions here
      logger.log(yourOpenNetCdfFileErrorMsgTxt, ioe);
    }
  }

  /**
   * Tutorial code snippet to unpack data from dataset opened in enhanced mode
   * 
   * @param packed_data_value
   * @param scale_factor
   * @param add_offset
   * @return
   */
  public static double unpackData(float packed_data_value, double scale_factor, double add_offset) {
    double unpacked_data_value = packed_data_value * scale_factor + add_offset;
    return unpacked_data_value; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet highlighting all options to open a file
   * full param list for NetcdfDatasets.openFile
   * 
   * @return opened file pointer
   * @throws IOException
   */
  public static NetcdfFile openNCFileOptions(DatasetUrl datasetUrl, int buffer_size,
      CancelTask cancelTask, Object serviceProviderInstance) throws IOException {
    // public static NetcdfFile openFile(DatasetUrl location, int buffer_size, CancelTask cancelTask, Object spiObject)
    NetcdfFile ncfile =
        NetcdfDatasets.openFile(datasetUrl, buffer_size, cancelTask, serviceProviderInstance);
    return ncfile; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet highlighting all options to open a file
   * full param list for NetcdfDatasets.openDataset
   * 
   * @return opened file pointer
   * @throws IOException
   */
  public static NetcdfDataset openEnhancedDatasetOptions(DatasetUrl datasetUrl,
      Set<NetcdfDataset.Enhance> enhanceMode, int buffer_size, CancelTask cancelTask,
      Object serviceProviderInstance) throws IOException {
    // public static NetcdfDataset openDataset(DatasetUrl location, Set<Enhance> enhanceMode, int buffer_size,
    // CancelTask cancelTask, Object spiObject)
    NetcdfDataset ncd = NetcdfDatasets.openDataset(datasetUrl, enhanceMode, buffer_size, cancelTask,
        serviceProviderInstance);
    return ncd; /* DOCS-IGNORE */
  }

  /**
   * Tutorial code snippet for caching opened files
   * 
   * @param datasetUrl
   * @param cancelTask
   * @throws IOException
   */
  public static void cacheFiles(DatasetUrl datasetUrl, CancelTask cancelTask) throws IOException {
    // on application startup
    NetcdfDatasets.initNetcdfFileCache(100, 200, 15 * 60);

    // instead of openFile
    NetcdfFile ncfile = NetcdfDatasets.acquireFile(datasetUrl, cancelTask);
    // do stuff here
    ncfile.close();

    // instead of openDataset
    NetcdfDataset ndc = NetcdfDatasets.acquireDataset(datasetUrl, cancelTask);
    // do stuff here
    ndc.close();

    // when terminating the application
    NetcdfDatasets.shutdown();
  }
}
