package tests.cdmdatasets;

import examples.cdmdatasets.NetcdfDatasetTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestNetcdfDatasetTutorial {

  private static String dataPathStr = TestDir.cdmLocalFromTestDataDir + "ncml/enhance/testStandaloneEnhance.ncml";
  private static DatasetUrl datasetUrl;

  @BeforeClass
  public static void setUpTests() throws IOException {
    datasetUrl = DatasetUrl.findDatasetUrl(dataPathStr);
  }

  @Test
  public void testOpenNCFileTutorial() {
    // test open success
    NetcdfDatasetTutorial.logger.clearLog();
    NetcdfDatasetTutorial.openNCFile(dataPathStr);
    assertThat(NetcdfDatasetTutorial.logger.getLogSize()).isEqualTo(0);

    // test open fail
    NetcdfDatasetTutorial.openNCFile("");
    assertThat(NetcdfDatasetTutorial.logger.getLastLogMsg())
        .isEqualTo(NetcdfDatasetTutorial.yourOpenNetCdfFileErrorMsgTxt);
  }

  @Test
  public void testOpenEnhancedDatasetTutorial() {
    // test open success
    NetcdfDatasetTutorial.logger.clearLog();
    NetcdfDatasetTutorial.openEnhancedDataset(dataPathStr);
    assertThat(NetcdfDatasetTutorial.logger.getLogSize()).isEqualTo(0);

    // test open fail
    NetcdfDatasetTutorial.openEnhancedDataset("");
    assertThat(NetcdfDatasetTutorial.logger.getLastLogMsg())
        .isEqualTo(NetcdfDatasetTutorial.yourOpenNetCdfFileErrorMsgTxt);
  }


  @Test
  public void testUnpackDataTutorial() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openDataset(dataPathStr, true, null)) {
      // get packed var
      Variable scaledvar = ncfile.findVariable("scaledvar");
      assertThat((Object) scaledvar).isNotNull();
      assertThat(scaledvar.getDataType()).isEqualTo(DataType.FLOAT);

      assertThat(scaledvar.attributes().hasAttribute("scale_factor")).isTrue();
      double scale_factor = scaledvar.attributes().findAttributeDouble("scale_factor", 1.0);
      assertThat(scaledvar.attributes().hasAttribute("add_offset")).isTrue();
      double add_offset = scaledvar.attributes().findAttributeDouble("add_offset", 1.0);

      double unpacked_data =
          NetcdfDatasetTutorial.unpackData(scaledvar.readScalarShort(), scale_factor, add_offset);
      assertThat(unpacked_data).isNotNaN();
    }
  }

  @Test
  public void testOpenNCFileOptionsTutorial() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasetTutorial.openNCFileOptions(datasetUrl, -1, null, null)) {
      assertThat(ncfile).isNotNull();
    }
  }

  @Test
  public void testOpenEnhancedDatasetOptionsTutorial() throws IOException {
    try (NetcdfDataset ncd =
        NetcdfDatasetTutorial.openEnhancedDatasetOptions(datasetUrl, null, -1, null, null)) {
      assertThat(ncd).isNotNull();
    }
  }

  @Test
  public void testCacheFilesTutorials() throws IOException {
    // just test it runs without errors or deprecation warnings
    NetcdfDatasetTutorial.cacheFiles(datasetUrl, null);
  }
}
