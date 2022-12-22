package ucar.nc2.internal.ncml;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.junit.Assert;
import org.junit.Test;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.ncml.TestNcmlRead;
import ucar.unidata.util.test.TestDir;

public class TestNcmlReader {

  @Test
  public void shouldMergeNcml() throws IOException {
    final String filename = TestDir.cdmLocalTestDataDir + "example1.nc";

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null)) {
      final NetcdfDataset netcdfDataset = NcmlReader.mergeNcml(netcdfFile, getNcmlElement()).build();

      final Variable ncmlVariable = netcdfDataset.findVariable("deltaLat");
      assertThat((Object) ncmlVariable).isInstanceOf(VariableDS.class);

      final Variable originalVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) originalVariable).isInstanceOf(VariableDS.class);
    }
  }

  private static Element getNcmlElement() {
    final String ncml = TestNcmlRead.topDir + "modifyVars.xml";

    try {
      SAXBuilder saxBuilder = new SAXBuilder();
      saxBuilder.setExpandEntities(false);
      Document jdomDoc = saxBuilder.build(ncml);
      return jdomDoc.getRootElement();
    } catch (JDOMException | IOException e) {
      Assert.fail();
      e.printStackTrace();
      return null;
    }
  }
}
