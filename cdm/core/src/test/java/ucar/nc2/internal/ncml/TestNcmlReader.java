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
import ucar.nc2.Attribute;
import ucar.nc2.Group;
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

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null);
        final NetcdfDataset netcdfDataset =
            NcmlReader.mergeNcml(netcdfFile, getNcmlElement("modifyVars.xml")).build();) {

      final Variable ncmlVariable = netcdfDataset.findVariable("deltaLat");
      assertThat((Object) ncmlVariable).isInstanceOf(VariableDS.class);

      final Variable originalVariable = netcdfDataset.findVariable("lat");
      assertThat((Object) originalVariable).isInstanceOf(VariableDS.class);
    }
  }

  @Test
  public void shouldMergeNcmlWithEnhancements() throws IOException, JDOMException {
    final String filename = TestDir.cdmLocalTestDataDir + "example1.nc";

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null);
        final NetcdfDataset netcdfDataset =
            NcmlReader.mergeNcml(netcdfFile, getNcmlElement("enhance/testStandardizer.ncml")).build();) {

      final Variable ncmlVariable = netcdfDataset.findVariable("doublevar");
      assertThat((Object) ncmlVariable).isNotNull();
      Array dataDoubles = ncmlVariable.read();
      assertThat(nearlyEquals(dataDoubles, TestEnhanceStandardizer.DATA_DOUBLES)).isTrue();
    }
  }

  @Test
  public void mergeWithExistingGroups() throws IOException, JDOMException {
    final String filename = TestDir.cdmLocalTestDataDir + "testModifyNestedGroups.nc4";

    try (NetcdfFile netcdfFile = NetcdfDatasets.openFile(filename, null);
        final NetcdfDataset netcdfDataset =
            NcmlReader.mergeNcml(netcdfFile, getNcmlElement("modifyNestedGroups.xml")).build()) {

      final Group ncmlOuterGroup = netcdfDataset.findGroup("/outer_group");
      assertThat(ncmlOuterGroup).isNotNull();

      final Attribute ncmlOuterAttribute = ncmlOuterGroup.findAttribute("new_outer_attr");
      assertThat(ncmlOuterAttribute).isNotNull();
      assertThat(ncmlOuterAttribute.getStringValue()).isNotNull();
      assertThat(ncmlOuterAttribute.getStringValue()).isEqualTo("new_outer_attr_value");

      final Variable ncmlOuterVar = ncmlOuterGroup.findVariableLocal("lat");
      assertThat(ncmlOuterVar != null).isTrue();
      assertThat(ncmlOuterVar.attributes().hasAttribute("new_lat_attr")).isTrue();
      assertThat(ncmlOuterVar.attributes().findAttribute("new_lat_attr")).isNotNull();
      assertThat(ncmlOuterVar.attributes().findAttribute("new_lat_attr").getStringValue()).isNotNull();
      assertThat(ncmlOuterVar.attributes().findAttribute("new_lat_attr").getStringValue())
          .isEqualTo("new_lat_attr_value");
      // <attribute name="new_lat_attr" value="new_lat_attr_value" />

      final Group ncmlInnerGroup = netcdfDataset.findGroup("/outer_group/inner_group");
      assertThat(ncmlInnerGroup).isNotNull();

      final Attribute ncmlInnerAttribute = ncmlInnerGroup.findAttribute("new_inner_attr");
      assertThat(ncmlInnerAttribute).isNotNull();
      assertThat(ncmlInnerAttribute.getStringValue()).isNotNull();
      assertThat(ncmlInnerAttribute.getStringValue()).isEqualTo("new_inner_attr_value");

      final Variable ncmlInnerVar = ncmlInnerGroup.findVariableLocal("lon");
      assertThat(ncmlInnerVar != null).isTrue();
      assertThat(ncmlInnerVar.attributes().hasAttribute("new_lon_attr")).isTrue();
      assertThat(ncmlInnerVar.attributes().findAttribute("new_lon_attr")).isNotNull();
      assertThat(ncmlInnerVar.attributes().findAttribute("new_lon_attr").getStringValue()).isNotNull();
      assertThat(ncmlInnerVar.attributes().findAttribute("new_lon_attr").getStringValue())
          .isEqualTo("new_lon_attr_value");
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
