package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;
import java.io.IOException;
import org.junit.Test;
import ucar.array.Array;
import ucar.ma2.DataType;
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

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits("units")
            .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll())
            .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1");
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.GeoX);
    CoordinateAxis axis = builder.build(parent.build());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(axis.getAxisType()).isEqualTo(AxisType.GeoX);
    assertThat(axis.getUnitsString()).isEqualTo("units");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("units");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");

    ucar.array.Array<?> data = axis.readArray();
    assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(data.length()).isEqualTo(7);
    ucar.array.Array<Float> fdata = (Array<Float>) data;
    System.out.printf("data = %s%n", data);
    for (float f : fdata) {
      assertThat(Float.isNaN(f)).isTrue();
    }
  }

  @Test
  public void testFromVariableDSwithData() throws IOException {
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
            .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT)
            .setUnits("days since 2001-01-01:00:00")
            .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll())
            .setAutoGen(1, 2)
            .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1");
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());

    assertThat(axis.getShortName()).isEqualTo("name");
    assertThat(axis.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(axis.getAxisType()).isEqualTo(AxisType.Time);
    assertThat(axis.getUnitsString()).isEqualTo("days since 2001-01-01:00:00");
    assertThat(axis.getDescription()).isEqualTo("desc");
    assertThat(axis.getEnhanceMode()).isEqualTo(NetcdfDataset.getEnhanceAll());
    assertThat(axis.findAttributeString(CDM.UNITS, "")).isEqualTo("days since 2001-01-01:00:00");
    assertThat(axis.findAttributeString(CDM.LONG_NAME, "")).isEqualTo("desc");

    Array<?> data = axis.readArray();
    assertThat(data.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(data.length()).isEqualTo(7);
    Array<Float> fdata = (Array<Float>) data;
    System.out.printf("data = %s%n", data);
    int count = 0;
    for (float f : fdata) {
      assertThat(f).isEqualTo(1 + 2 * count);
      count++;
    }
  }

}
