package tests.writingiosp;

import examples.writingiosp.IospDetailsTutorial;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.unidata.io.RandomAccessFile;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

public class TestIospDetailsTutorial {
  private final static String testFilePath =
      "src/public/userguide/files/netcdfJava_tutorial/writingiosp/lightningData.txt";

  private static RandomAccessFile raf;

  private static int nRecords = 190;

  @BeforeClass
  public static void setUpTest() throws IOException {
    raf = RandomAccessFile.acquire(testFilePath);
  }

  @Test
  public void testIsValidExample1() throws IOException {
    IospDetailsTutorial.isValidExample1(raf);
  }

  @Test
  public void testIsValidExample2() {
    IospDetailsTutorial.isValidExample2(raf);
  }

  @Test
  public void testIsValidExample3() throws IOException {
    IospDetailsTutorial.isValidExample3(raf);
  }

  @Test
  public void testIsValidExample4() throws IOException {
    IospDetailsTutorial.isValidExample3(raf);
  }

  @Test
  public void testAddGlobalAttribute() {
    Group.Builder group = Group.builder();
    IospDetailsTutorial.addGlobalAttribute(group);
    Group rootGroup = group.build();
    assertThat(rootGroup.hasAttribute("version")).isTrue();
    assertThat(rootGroup.hasAttribute("Conventions")).isTrue();
  }

  @Test
  public void testAddVarAttribute() {
    IospDetailsTutorial.addVarAttribute();
  }

  @Test
  public void testAddDimension() {
    Group.Builder group = Group.builder();
    IospDetailsTutorial.addDimension(group);
    Group rootGroup = group.build();
    assertThat(rootGroup.findDimensionLocal("lat")).isNotNull();
    assertThat(rootGroup.findDimensionLocal("lon")).isNotNull();
  }

  @Test
  public void testUnsignedAttribute() {
    Variable.Builder var = Variable.builder().setName("variable").setDataType(DataType.INT);
    IospDetailsTutorial.unsignedAttribute(var);
    Variable v = var.build(Group.builder().build());
    assertThat(v.hasAttribute("_Unsigned")).isTrue();
    assertThat(v.findAttribute("_Unsigned").getStringValue()).isEqualTo("true");
  }

  @Test
  public void testCreateVariable() {
    Group.Builder group = Group.builder().addDimension(Dimension.builder("lat", 190).build())
        .addDimension(Dimension.builder("lon", 360).build());
    IospDetailsTutorial.createVariable(group);
    assertThat((Iterable<?>) group.build().findVariableLocal("elevation")).isNotNull();
  }

  @Test
  public void testCreateCoordinateVariable() {
    Group.Builder group = Group.builder().addDimension(Dimension.builder("lat", 190).build());
    IospDetailsTutorial.createCoordinateVariable(group);
    Variable var = group.build().findVariableLocal("lat");
    assertThat((Iterable<?>) var).isNotNull();
    assertThat(var.getShortName()).isEqualTo(var.getDimension(0).getShortName());
  }

  @Test
  public void testSetVariableData() {
    Group.Builder group = Group.builder().addDimension(Dimension.builder("lat", 190).build());
    Variable.Builder lat =
        Variable.builder().setParentGroupBuilder(group).setName("lat").setDataType(DataType.FLOAT)
            .setDimensionsByName("lat").addAttribute(new Attribute("units", "degrees_north"));
    IospDetailsTutorial.setVariableData(lat);
    assertThat(lat.build(group.build()).hasCachedData()).isTrue();
  }

  @Test
  public void testReadExample1() throws IOException, InvalidRangeException {
    Group.Builder group = Group.builder();
    Dimension d1 = Dimension.builder("i", 190).build();
    group.addDimension(d1);
    Dimension d2 = Dimension.builder("j", 5).build();
    group.addDimension(d2);
    Group parent = group.build();
    Variable var = Variable.builder().setName("var").setDataType(DataType.INT).addDimension(d1)
        .addDimension(d2).build(parent);
    Array data = IospDetailsTutorial.readExample1(raf, var,
        Section.builder().appendRange(0, 189).appendRange(0, 4).build());
    assertThat(data).isNotNull();
  }

  @Test
  public void testReadExample2() throws IOException, InvalidRangeException {
    Group.Builder group = Group.builder();
    Dimension d1 = Dimension.builder("i", 190).build();
    group.addDimension(d1);
    Dimension d2 = Dimension.builder("j", 5).build();
    group.addDimension(d2);
    Group parent = group.build();
    Variable var = Variable.builder().setName("var").setDataType(DataType.INT).addDimension(d1)
        .addDimension(d2).build(parent);
    Array data = IospDetailsTutorial.readExample2(raf, var, null);
    assertThat(data).isNotNull();
  }

  @Test
  public void testReadExample3() throws IOException, InvalidRangeException {
    Group.Builder group = Group.builder();
    IospDetailsTutorial.readExample3(raf, group);
    assertThat(group.findVariableLocal("elevation").isPresent()).isTrue();
    assertThat(group.findVariableLocal("elevation").get().spiObject).isNotNull();
  }
}
