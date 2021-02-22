package ucar.nc2.write;

import org.jdom2.Element;
import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static ucar.nc2.TestUtils.makeDummyGroup;

public class TestNcmlWriter {

  @Test
  public void testMakeValuesElementFloat() throws IOException {
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsAnonymous(new int[] {3})
        .setAutoGen(3.2, 2).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected = "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3.2 5.2 7.2</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementDouble() throws IOException {
    Variable var = Variable.builder().setName("name").setDataType(DataType.DOUBLE).setDimensionsAnonymous(new int[] {3})
        .setAutoGen(3.2, 2).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected = "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3.2 5.2 7.2</values>\n";
    assertThat(ncml).endsWith(expected);
  }

  @Test
  public void testMakeValuesElementInteger() throws IOException {
    Variable var = Variable.builder().setName("name").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3})
        .setAutoGen(3, 22).build(makeDummyGroup());

    ucar.array.Array<?> data = var.readArray();
    System.out.printf("data = %s%n", data);

    NcmlWriter writer = new NcmlWriter();
    Element elem = writer.makeValuesElement(var, false);
    String ncml = writer.writeToString(elem);

    System.out.printf("ncml = %s%n", ncml);
    String expected = "<values xmlns=\"http://www.unidata.ucar.edu/namespaces/netcdf/ncml-2.2\">3 25 47</values>\n";
    assertThat(ncml).endsWith(expected);
  }
}
