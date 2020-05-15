package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;

public class TestCoordinateAxisBuilder {

  @Test
  public void testFromVariableDS() {
    // NetcdfDataset ncd = NetcdfDataset.builder().build();
    VariableDS.Builder vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits("units")
        .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll());
    CoordinateAxis.Builder builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    CoordinateAxis axis = builder.build(makeDummyGroup());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(axis.getUnitsString()).isEqualTo("units");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");
  }

}
