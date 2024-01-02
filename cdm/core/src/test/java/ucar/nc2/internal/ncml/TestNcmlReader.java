package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;
import static ucar.ma2.MAMath.nearlyEquals;

import java.io.IOException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ncml.TestEnhanceStandardizer;
import ucar.nc2.ncml.TestNcmlRead;
import ucar.unidata.util.test.TestDir;

public class TestNcmlReader {

  @Test
  public void shouldMergeNcml() throws IOException, JDOMException {
    final String filename = TestDir.cdmLocalTestDataDir + "example1.nc";

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null)) {
      final NetcdfDataset netcdfDataset = NcmlReader.mergeNcml(netcdfFile, getNcmlElement("modifyVars.xml")).build();

      final Variable ncmlVariable = netcdfDataset.findVariable("deltaLat");
      assertThat((Object) ncmlVariable).isInstanceOf(VariableDS.class);

      final Variable originalVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) originalVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldMergeNcmlWithEnhancements() throws IOException, JDOMException {
    final String filename = TestDir.cdmLocalTestDataDir + "example1.nc";

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null)) {
      final NetcdfDataset netcdfDataset =
          NcmlReader.mergeNcml(netcdfFile, getNcmlElement("enhance/testStandardizer.ncml")).build();

      final Variable ncmlVariable = netcdfDataset.findVariable("doublevar");
      assertThat((Object) ncmlVariable).isNotNull();
      Array dataDoubles = ncmlVariable.read();
      assertThat(nearlyEquals(dataDoubles, TestEnhanceStandardizer.DATA_DOUBLES)).isTrue();
    }
  }

  private static Element getNcmlElement(String filename) throws IOException, JDOMException {
    final String ncml = TestNcmlRead.topDir + filename;

    SAXBuilder saxBuilder = new SAXBuilder();
    saxBuilder.setExpandEntities(false);
    Document jdomDoc = saxBuilder.build(ncml);
    return jdomDoc.getRootElement();
  }
}
