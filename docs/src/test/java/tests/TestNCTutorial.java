package tests;

import static com.google.common.truth.Truth.assertThat;

import examples.NCTutorial;
import org.junit.Test;
import ucar.unidata.util.test.TestDir;

public class TestNCTutorial {

  // path to a netcdf file
  private static String testDataPathStr = TestDir.cdmTestDataDir +  "/thredds/public/testdata/testData.nc";
  // path to java file with tutorial code snippets
  private static String testClass = "src/test/java/examples/NCTutorial.java";

  @Test
  public void testOpenNCFileTutorial() {
    // test open success
    NCTutorial.logger.clearLog();
    NCTutorial.openNCFileTutorial(testDataPathStr);
    assertThat(NCTutorial.logger.getLogSize()).isEqualTo(0);

    // test open fail
    NCTutorial.openNCFileTutorial("");
    assertThat(NCTutorial.logger.getLastLogMsg()).isEqualTo(NCTutorial.yourOpenNetCdfFileErrorMsgTxt);
  }
}
