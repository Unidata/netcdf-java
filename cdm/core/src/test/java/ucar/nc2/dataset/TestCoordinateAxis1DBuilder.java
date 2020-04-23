package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.constants.AxisType;
import ucar.nc2.constants.CDM;

public class TestCoordinateAxis1DBuilder {

  @Test
  public void testFromVariableDS() throws IOException {
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits("units")
        .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll())
        .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1");
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    assertThat(builder instanceof CoordinateAxis1D.Builder<?>).isTrue();
    CoordinateAxis axis = builder.build(parent.build());
    assertThat(axis instanceof CoordinateAxis1D).isTrue();
    CoordinateAxis1D axis1D = (CoordinateAxis1D) axis;

    assertThat(axis1D.getShortName()).isEqualTo("name");
    assertThat(axis1D.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(axis1D.getUnitsString()).isEqualTo("units");
    assertThat(axis1D.getDescription()).isEqualTo("desc");
    assertThat(axis1D.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis1D.findAttValueIgnoreCase(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis1D.findAttValueIgnoreCase(CDM.LONG_NAME, "")).isEqualTo("desc");

    Array data = axis1D.read();
    System.out.printf("data = %s%n", data);
    IndexIterator iter = data.getIndexIterator();
    while (iter.hasNext()) {
      assertThat(iter.getFloatNext()).isEqualTo(Float.NaN);
    }
  }

}
