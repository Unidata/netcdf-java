package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static ucar.nc2.TestUtils.makeDummyGroup;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.MAMath;
import ucar.ma2.Range;
import ucar.ma2.Section;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;

/** Test {@link ucar.nc2.Variable} */
public class TestVariable {

  @Test
  public void testBuilder() {
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).build(makeDummyGroup());
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isTrue();
    assertThat(var.getElementSize()).isEqualTo(4);
    assertThat(var.getSize()).isEqualTo(1);
    assertThat(var.getDatasetLocation()).isEqualTo("N/A");
    assertThat(var.isMetadata()).isEqualTo(false);
    assertThat(var.isUnlimited()).isEqualTo(false);
    assertThat(var.isVariableLength()).isEqualTo(false);
  }

  @Test
  public void testWithDims() {
    try {
      // Must set dimension first
      Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsByName("dim1 dim2")
          .build(makeDummyGroup());
      fail();
    } catch (Exception e) {
      // ok
    }

    Dimension dim1 = Dimension.builder("dim1", 7).setIsUnlimited(true).build();
    Dimension dim2 = new Dimension("dim2", 27);
    Group.Builder gb = Group.builder().addDimensions(ImmutableList.of(dim1, dim2));

    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setParentGroupBuilder(gb)
        .setDimensionsByName("dim1 dim2").build(gb.build());
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isTrue();
    assertThat(var.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {7, 27}));

    assertThat(var.getDimensions()).isEqualTo(ImmutableList.of(dim1, dim2));
    assertThat(var.getDimension(0)).isEqualTo(dim1);
    assertThat(var.getDimension(1)).isEqualTo(dim2);
    assertThat(var.getDimensionsString()).isEqualTo("dim1 dim2");

    assertThat(var.findDimensionIndex("dim2")).isEqualTo(1);
    assertThat(var.findDimensionIndex("fake")).isEqualTo(-1);
  }

  @Test
  public void testWithAnonymousDims() {
    int[] shape = new int[] {3, 6, -1};
    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setDimensionsAnonymous(shape)
        .build(makeDummyGroup());
    assertThat(var.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(var.getShortName()).isEqualTo("name");
    assertThat(var.isScalar()).isFalse();
    assertThat(var.isUnlimited()).isFalse();
    assertThat(var.getShape()).isEqualTo(new int[] {3, 6, -1});
    assertThat(var.getShapeAsSection()).isEqualTo(new Section(new int[] {3, 6, -1}));
    assertThat(var.getDimensionsString()).isEqualTo("3 6 *");
  }

  @Test
  public void testCopy() {
    Group.Builder gb = Group.builder().addDimension(Dimension.builder("dim1", 7).setIsUnlimited(true).build())
        .addDimension(new Dimension("dim2", 27));
    Group group = gb.build();

    Variable var = Variable.builder().setName("name").setDataType(DataType.FLOAT).setParentGroupBuilder(gb)
        .setDimensionsByName("dim1 dim2").build(group);

    Variable copy = var.toBuilder().build(group);

    assertThat(copy.getParentGroup()).isEqualTo(group);
    assertThat(copy.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(copy.getShortName()).isEqualTo("name");
    assertThat(copy.isScalar()).isFalse();
    assertThat(copy.isUnlimited()).isTrue();
    assertThat(copy.getShape()).isEqualTo(new int[] {7, 27});
    assertThat(copy.getShapeAsSection()).isEqualTo(new Section(new int[] {7, 27}));

    assertThat((Object) copy).isEqualTo(var);
  }

  @Test
  public void testNestedGroups() {
    Dimension low = new Dimension("low", 1);
    Dimension mid = new Dimension("mid", 1);
    Dimension high = new Dimension("high", 1);
    Group.Builder parentg = Group.builder().setName("parent").addDimension(low);
    Group.Builder grampsb = Group.builder().setName("gramps").addGroup(parentg).addDimension(mid);
    Group.Builder uncleb = Group.builder().setName("uncle");

    Variable.Builder<?> vattb = Variable.builder().setName("vatt").setDataType(DataType.STRING)
        .setParentGroupBuilder(parentg).setDimensionsByName("mid").addAttribute(new Attribute("findme", "findmevalue"));
    grampsb.addVariable(vattb);
    assertThat(vattb.toString()).isEqualTo("String vatt");

    Group root = Group.builder().addGroup(grampsb).addGroup(uncleb).addDimension(high).build();
    Variable vatt = root.findVariableByAttribute("findme", "findmevalue");
    assertThat(vatt).isNotNull();

    assertThat(vatt.getFullName()).isEqualTo("gramps/vatt");
  }

  @Test
  public void testNetcdf() {
    Variable.Builder<?> vb =
        Variable.builder().setName("v").setDataType(DataType.UBYTE).setDimensionsAnonymous(new int[] {3, 6});

    Group.Builder root = Group.builder().addVariable(vb);
    NetcdfFile ncfile = NetcdfFile.builder().setRootGroup(root).setLocation("loca").setId("notFileType").build();

    Variable var = ncfile.findVariable("v");
    assertThat(var).isNotNull();
    assertThat(var.getDatasetLocation()).isEqualTo("loca");
    assertThat(var.getFileTypeId()).isEqualTo("N/A");
    assertThat(var.getNetcdfFile()).isEqualTo(ncfile);
  }

  @Test
  public void testEnum() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("typename", map);

    Variable.Builder<?> vb = Variable.builder().setName("v").setDataType(DataType.ENUM4)
        .setDimensionsAnonymous(new int[] {3, 6}).setEnumTypeName("typename");

    Group.Builder root = Group.builder().addEnumTypedef(typedef1).addVariable(vb);
    NetcdfFile ncfile = NetcdfFile.builder().setRootGroup(root).build();

    Variable var = ncfile.findVariable("v");
    assertThat(var).isNotNull();

    assertThat(var.getEnumTypedef()).isEqualTo(typedef1);
    assertThat(var.lookupEnumString(3)).isEqualTo("name3");

    assertThat(var.toString()).startsWith("enum typename v(3, 6);");

    try {
      Variable.builder().setName("v").setDataType(DataType.ENUM4).setDimensionsAnonymous(new int[] {3, 6})
          .build(makeDummyGroup());
      fail();
    } catch (Exception e) {
      // expected
    }

    try {
      Variable.builder().setName("v").setDataType(DataType.ENUM4).setDimensionsAnonymous(new int[] {3, 6})
          .setEnumTypeName("enum").build(makeDummyGroup());
      fail();
    } catch (Exception e) {
      // expected
    }

  }

  @Test
  public void testUnits() {
    Variable v = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .addAttribute(new Attribute(CDM.UNITS, " wuw ")).build(makeDummyGroup());
    assertThat(v.getUnitsString()).isEqualTo("wuw");

    Variable v2 = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .build(makeDummyGroup());
    assertThat(v2.getUnitsString()).isNull();
  }

  @Test
  public void testDesc() {
    Variable v = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .addAttribute(new Attribute(CDM.LONG_NAME, "what")).build(makeDummyGroup());
    assertThat(v.getDescription()).isEqualTo("what");

    Variable v2 = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .addAttribute(new Attribute("description", "desc")).build(makeDummyGroup());
    assertThat(v2.getDescription()).isEqualTo("desc");

    Variable v3 = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .addAttribute(new Attribute(CDM.TITLE, "title")).build(makeDummyGroup());
    assertThat(v3.getDescription()).isEqualTo("title");

    Variable v4 = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .addAttribute(new Attribute(CF.STANDARD_NAME, "standar")).build(makeDummyGroup());
    assertThat(v4.getDescription()).isEqualTo("standar");

    Variable vnone = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .build(makeDummyGroup());
    assertThat(vnone.getDescription()).isNull();

    Variable vnotString =
        Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
            .addAttribute(new Attribute(CDM.LONG_NAME, 123)).build(makeDummyGroup());
    assertThat(vnotString.getDescription()).isNull();
  }

  @Test
  public void testIsCoordinateVariable() {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var =
        Variable.builder().setName("x").setDataType(DataType.FLOAT).setDimensions(ImmutableList.of(x));
    Group g = Group.builder().addDimension(x).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();
    assertThat(xvar.isCoordinateVariable()).isTrue();

    Variable vnone = Variable.builder().setName("v").setDataType(DataType.INT).setDimensionsAnonymous(new int[] {3, 6})
        .build(makeDummyGroup());
    assertThat(vnone.isCoordinateVariable()).isFalse();
  }

  @Test
  public void testIsCoordinateVariableChar() {
    Dimension x = new Dimension("x", 27);
    Dimension xlen = new Dimension("xlen", 27);
    Variable.Builder<?> var =
        Variable.builder().setName("x").setDataType(DataType.CHAR).setDimensions(ImmutableList.of(x, xlen));
    Group g = Group.builder().addDimensions(ImmutableList.of(x, xlen)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();
    assertThat(xvar.isCoordinateVariable()).isTrue();
  }

  @Test
  public void testCDL() {
    Dimension x = new Dimension("x", 27);
    Dimension xlen = new Dimension("xlen", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.CHAR)
        .setDimensions(ImmutableList.of(x, xlen)).addAttribute(new Attribute("name", "value"));
    Group g = Group.builder().addDimensions(ImmutableList.of(x, xlen)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    assertThat(xvar.getNameAndDimensions()).isEqualTo("x(x=27, xlen=27)");
    assertThat(xvar.toString()).startsWith(String.format("char x(x=27, xlen=27);%n" + "  :name = \"value\";"));
  }

  @Test
  public void testEquals() {
    Dimension x = new Dimension("x", 27);
    Dimension xlen = new Dimension("xlen", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.CHAR)
        .setDimensions(ImmutableList.of(x, xlen)).addAttribute(new Attribute("name", "value"));
    Group g = Group.builder().addDimensions(ImmutableList.of(x, xlen)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Variable.Builder<?> var2 = Variable.builder().setName("x").setDataType(DataType.CHAR)
        .setDimensions(ImmutableList.of(x, xlen)).addAttribute(new Attribute("name", "value"));
    Group g2 = Group.builder().addDimensions(ImmutableList.of(x, xlen)).addVariable(var2).build();
    Variable xvar2 = g2.findVariableLocal("x");
    assertThat(xvar2).isNotNull();

    assertThat(xvar).isEqualTo(xvar2);
    assertThat(xvar.hashCode()).isEqualTo(xvar2.hashCode());
    assertThat(xvar.compareTo(xvar2)).isEqualTo(0);
  }

  @Test
  public void testAutoGen() throws IOException {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(100, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Array data = xvar.read();
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, x.getLength(), 100, 10))).isTrue();
  }

  @Test
  public void testReadByIndex() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(0, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Array data = xvar.read(new int[] {3}, new int[] {3});
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 3, 30, 10))).isTrue();
  }

  @Test
  public void testReadByRanges() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(100, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Range r = new Range(10, 20);
    Array data = xvar.read(ImmutableList.of(r));
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 11, 200, 10))).isTrue();
  }

  @Test
  public void testReadBySection() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 99);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(0, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Array data = xvar.read(Section.builder().appendRange(20, 66).build());
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 47, 200, 10))).isTrue();
  }

  @Test
  public void testReadBySectionSpec() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(100, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Array data = xvar.read("10:20");
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 11, 200, 10))).isTrue();
  }

  @Test
  public void testSection() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 27);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x)).addAttribute(new Attribute("name", "value")).setAutoGen(100, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Range r = new Range(10, 20);
    Variable section = xvar.section(ImmutableList.of(r));

    Array data = section.read();
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 11, 200, 10))).isTrue();
  }

  @Test
  public void testSliceRowMajor() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 20);
    Dimension y = new Dimension("y", 2);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(x, y)).addAttribute(new Attribute("name", "value")).setAutoGen(0, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x, y)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();
    Array alldata = xvar.read();

    Variable section = xvar.slice(1, 1);
    Array data = section.read();
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 20, 10, 20))).isTrue();
  }

  @Test
  public void testSliceColMajor() throws IOException, InvalidRangeException {
    Dimension x = new Dimension("x", 20);
    Dimension y = new Dimension("y", 2);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(y, x)).addAttribute(new Attribute("name", "value")).setAutoGen(0, 10);
    Group g = Group.builder().addDimensions(ImmutableList.of(x, y)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Variable slice = xvar.slice(1, 1);
    Array data = slice.read();
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 2, 10, 200))).isTrue();
  }

  @Test
  public void testReduce() throws IOException {
    Dimension x = new Dimension("x", 20);
    Dimension y = new Dimension("y", 1);
    Variable.Builder<?> var = Variable.builder().setName("x").setDataType(DataType.INT)
        .setDimensions(ImmutableList.of(y, x)).addAttribute(new Attribute("name", "value")).setAutoGen(10, 2);
    Group g = Group.builder().addDimensions(ImmutableList.of(x, y)).addVariable(var).build();
    Variable xvar = g.findVariableLocal("x");
    assertThat(xvar).isNotNull();

    Variable reduce = xvar.reduce(ImmutableList.of(y));
    assertThat(reduce.getShape()).isEqualTo(new int[] {20});
    Array data = reduce.read();
    assertThat(MAMath.equals(data, Array.makeArray(DataType.INT, 20, 10, 2))).isTrue();
  }

  @Test
  public void testReadScalarByte() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.BYTE).setAutoGen(10, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 10);
    assertThat(varb.readScalarShort()).isEqualTo((short) 10);
    assertThat(varb.readScalarInt()).isEqualTo(10);
    assertThat(varb.readScalarLong()).isEqualTo((long) 10);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 10);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 10);
  }

  @Test
  public void testReadScalarShort() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.SHORT).setAutoGen(11, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 11);
    assertThat(varb.readScalarShort()).isEqualTo((short) 11);
    assertThat(varb.readScalarInt()).isEqualTo(11);
    assertThat(varb.readScalarLong()).isEqualTo((long) 11);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 11);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 11);
  }


  @Test
  public void testReadScalarInt() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.INT).setAutoGen(11, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 11);
    assertThat(varb.readScalarShort()).isEqualTo((short) 11);
    assertThat(varb.readScalarInt()).isEqualTo(11);
    assertThat(varb.readScalarLong()).isEqualTo((long) 11);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 11);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 11);
  }


  @Test
  public void testReadScalarLong() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.LONG).setAutoGen(11, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 11);
    assertThat(varb.readScalarShort()).isEqualTo((short) 11);
    assertThat(varb.readScalarInt()).isEqualTo(11);
    assertThat(varb.readScalarLong()).isEqualTo((long) 11);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 11);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 11);
  }


  @Test
  public void testReadScalarFloat() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.FLOAT).setAutoGen(11, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 11);
    assertThat(varb.readScalarShort()).isEqualTo((short) 11);
    assertThat(varb.readScalarInt()).isEqualTo(11);
    assertThat(varb.readScalarLong()).isEqualTo((long) 11);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 11);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 11);
  }

  @Test
  public void testReadScalarDouble() throws IOException {
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.DOUBLE).setAutoGen(11, 1).build(makeDummyGroup());
    assertThat(varb.readScalarByte()).isEqualTo((byte) 11);
    assertThat(varb.readScalarShort()).isEqualTo((short) 11);
    assertThat(varb.readScalarInt()).isEqualTo(11);
    assertThat(varb.readScalarLong()).isEqualTo((long) 11);
    assertThat(varb.readScalarFloat()).isEqualTo((float) 11);
    assertThat(varb.readScalarDouble()).isEqualTo((double) 11);
  }

  @Test
  public void testReadScalarString() throws IOException {
    Array data = Array.makeArray(DataType.STRING, new String[] {"one"});
    Variable varb = Variable.builder().setName("varb").setDataType(DataType.STRING).setCachedData(data, true)
        .build(makeDummyGroup());
    assertThat(varb.readScalarString()).isEqualTo("one");
  }

  @Test
  public void testReadScalarChar() throws IOException {
    Array data = Array.factory(DataType.CHAR, new int[] {3}, new char[] {'1', '2', '3'});
    Variable varb =
        Variable.builder().setName("varb").setDataType(DataType.CHAR).setCachedData(data, true).build(makeDummyGroup());
    assertThat(varb.readScalarString()).isEqualTo("123");
  }

}
