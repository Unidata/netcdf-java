package ucar.nc2.internal.grid;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.calendar.CalendarDateUnit;
import ucar.nc2.constants.AxisType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link ExtractTimeCoordinateValues} */
public class TestExtractTimeCoordinateValues {

  @Test
  public void testNumericValue() {
    String units = "days since 2001-01-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    VariableDS.Builder<?> vdsBuilder = VariableDS.builder().setName("name").setDataType(DataType.FLOAT).setUnits(units)
        .setDesc("desc").setEnhanceMode(NetcdfDataset.getEnhanceAll()).setAutoGen(1, 2)
        .addAttribute(new Attribute("missing_value", 0.0f)).setParentGroupBuilder(parent).setDimensionsByName("dim1");
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).isNull();
  }

  @Test
  public void testStringValue() {
    int n = 11;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", n).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    String[] strings = new String[n];
    for (int i = 0; i < n; i++) {
      strings[i] = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
    }

    Array<String> values = Arrays.factory(ArrayType.STRING, new int[] {n}, strings);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setDataType(DataType.STRING).setUnits(units).setDesc("desc")
            .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).hasSize(n);
    System.out.printf("extract.cdates = %s%n", extract.cdates);

    CalendarDateUnit dateUnit = CalendarDateUnit.fromAttributes(axis.attributes(), null).orElseThrow();
    count = 0;
    for (CalendarDate cd : extract.cdates) {
      assertThat(cd).isEqualTo(dateUnit.makeCalendarDate(count++));
    }
  }

  @Test
  public void testStringValueBad() {
    int n = 11;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", n).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    String[] strings = new String[n];
    for (int i = 0; i < n; i++) {
      strings[i] = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
    }
    strings[10] = "badenoff";

    Array<String> values = Arrays.factory(ArrayType.STRING, new int[] {n}, strings);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setDataType(DataType.STRING).setUnits(units).setDesc("desc")
            .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1").setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    try {
      new ExtractTimeCoordinateValues(axis);
      fail();
    } catch (Exception e) {
      e.printStackTrace();
      assertThat(e).isInstanceOf(RuntimeException.class);
      assertThat(e.getMessage()).contains("badenoff");
    }
  }

  @Test
  public void testCharValue() {
    int ndates = 11;
    int nchars = 20;
    String units = "days since 2020-02-01 00:00";
    Group.Builder parent = Group.builder().addDimension(Dimension.builder("dim1", ndates).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));

    int count = 1;
    byte[] chars = new byte[ndates * nchars];
    for (int i = 0; i < ndates; i++) {
      String date = CalendarDate.of(2020, 2, count++, 0, 0, 0).toString();
      int pos = i * nchars;
      for (int c = 0; c < date.length(); c++) {
        chars[pos++] = (byte) date.charAt(c);
      }
    }

    Array<String> values = Arrays.factory(ArrayType.CHAR, new int[] {ndates, nchars}, chars);

    VariableDS.Builder<?> vdsBuilder =
        VariableDS.builder().setName("name").setArrayType(ArrayType.CHAR).setUnits(units).setDesc("desc")
            .setEnhanceMode(NetcdfDataset.getEnhanceAll()).addAttribute(new Attribute("missing_value", 0.0f))
            .setParentGroupBuilder(parent).setDimensionsByName("dim1 " + nchars).setSourceData(values);
    parent.addVariable(vdsBuilder);

    CoordinateAxis.Builder<?> builder = CoordinateAxis.fromVariableDS(vdsBuilder).setAxisType(AxisType.Time);
    CoordinateAxis axis = builder.build(parent.build());
    ExtractTimeCoordinateValues extract = new ExtractTimeCoordinateValues(axis);

    assertThat(extract.cdates).hasSize(ndates);
    System.out.printf("extract.cdates = %s%n", extract.cdates);

    CalendarDateUnit dateUnit = CalendarDateUnit.fromAttributes(axis.attributes(), null).orElseThrow();
    count = 0;
    for (CalendarDate cd : extract.cdates) {
      assertThat(cd).isEqualTo(dateUnit.makeCalendarDate(count++));
    }
  }


}
