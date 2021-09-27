package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.charset.Charset;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;
import ucar.nc2.iosp.NetcdfFormatUtils;

/** Test {@link ucar.nc2.Attribute} */
public class TestAttribute {

  @Test
  public void testBuilder() {
    Attribute att = Attribute.builder().setName("name").setDataType(DataType.FLOAT)
        .setValues(Array.makeArray(DataType.FLOAT, ImmutableList.of("3.14", ".0015"))).build();
    assertThat(att.getName()).isEqualTo("name");
    assertThat(att.getShortName()).isEqualTo("name");
    assertThat(att.getDataType()).isEqualTo(DataType.FLOAT);
    assertThat(att.getLength()).isEqualTo(2);
    assertThat(att.getValue(0)).isEqualTo(Float.parseFloat("3.14"));
    assertThat(att.getValue(1)).isEqualTo(Float.parseFloat(".0015"));

    assertThat(att.getNumericValue()).isEqualTo(Float.parseFloat("3.14"));
    assertThat(att.getNumericValue(0)).isEqualTo(Float.parseFloat("3.14"));
    assertThat(att.getNumericValue(1)).isEqualTo(Float.parseFloat(".0015"));

    assertThat(att.isArray()).isTrue();
    assertThat(att.isString()).isFalse();
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getEnumType()).isNull();
  }

  @Test
  public void testStringBuilder() {
    Attribute att = Attribute.builder().setName("name").setStringValue("666").build();
    assertThat(att.getShortName()).isEqualTo("name");
    assertThat(att.getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getLength()).isEqualTo(1);

    assertThat(att.getValue(0)).isEqualTo("666");
    assertThat(att.getStringValue()).isEqualTo("666");
    assertThat(att.getNumericValue()).isEqualTo(Double.parseDouble("666"));

    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isTrue();
    assertThat(att.getEnumType()).isNull();
  }

  @Test
  public void testNumericBuilder() {
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, false).build();
    assertThat(att.getShortName()).isEqualTo("name");
    assertThat(att.getDataType()).isEqualTo(DataType.INT);
    assertThat(att.getLength()).isEqualTo(1);

    assertThat(att.getValue(0)).isEqualTo(123);
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getNumericValue()).isEqualTo(123);

    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isFalse();
    assertThat(att.getEnumType()).isNull();
    assertThat(att).isEqualTo(att.toBuilder().build());
  }

  @Test
  public void testNumericUnsignedBuilder() {
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).build();
    assertThat(att.getShortName()).isEqualTo("name");
    assertThat(att.getDataType()).isEqualTo(DataType.UINT);
    assertThat(att.getLength()).isEqualTo(1);

    assertThat(att.getValue(0)).isEqualTo(123);
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getNumericValue()).isEqualTo(123);

    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isFalse();
    assertThat(att.getEnumType()).isNull();
  }

  @Test
  public void testStringConstructor() {
    Attribute att = Attribute.builder().setName("name").setStringValue("666").build();
    assertThat(att).isEqualTo(new Attribute("name", "666"));
    assertThat(att).isEqualTo(att.toBuilder().build());
  }

  @Test
  public void testStringWithNulls() {
    byte[] b = new byte[7]; // 6 trailing blanks
    b[0] = (byte) 'A';
    String svalue = new String(b, Charset.defaultCharset());
    Attribute att = Attribute.builder().setName("name").setStringValue(svalue).build();
    assertThat(att.getStringValue()).isEqualTo("A");

    Attribute att2 = new Attribute("name", svalue);
    assertThat(att2.getStringValue()).isEqualTo("A");
  }

  @Test
  public void testEmptyValues() {
    Attribute att = Attribute.emptyValued("name", DataType.STRING);
    Attribute att2 = Attribute.builder().setName("name").build();
    assertThat(att.equals(att2)).isTrue();
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getNumericValue()).isNull();
  }

  @Test
  public void testArrayFromString() {
    Attribute att = Attribute.builder().setName("name").setStringValue("3.14").build();
    assertThat(att.getValues().getShape()).isEqualTo(new int[] {1});
    assertThat(att.getValues().getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getValues().getObject(0)).isEqualTo("3.14");
    assertThat(att.getNumericValue()).isEqualTo(3.14);

    assertThat(att).isEqualTo(att.toBuilder().build());
  }

  @Test
  public void testNumericConstructor() {
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).build();
    assertThat(att.getDataType().isUnsigned()).isTrue();
    assertThat(att.getStringValue()).isNull();
    assertThat(att).isEqualTo(att.toBuilder().build());

    Attribute att2 = new Attribute("name", 123);
    assertThat(att2.getDataType().isUnsigned()).isFalse();
    assertThat(att2.getStringValue()).isNull();
  }

  @Test
  public void testBuilderWithEnum() {
    Map<Integer, String> map = ImmutableMap.of(123, "name");
    EnumTypedef typedef = new EnumTypedef("enum", map);
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).setEnumType(typedef).build();
    assertThat(att.getShortName()).isEqualTo("name");
    assertThat(att.getDataType()).isEqualTo(DataType.UINT);
    assertThat(att.getLength()).isEqualTo(1);

    assertThat(att.getValue(0)).isEqualTo(123);
    assertThat(att.getStringValue()).isNull();
    assertThat(att.getNumericValue()).isEqualTo(123);

    assertThat(att.isArray()).isFalse();
    assertThat(att.isString()).isFalse();
    assertThat(att.getEnumType()).isEqualTo(typedef);

    Attribute copy = att.toBuilder().build();
    assertThat(att).isEqualTo(copy);
  }

  @Test
  public void testEquals() {
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).build();
    Attribute attu = Attribute.builder().setName("name").setNumericValue(123, false).build();
    assertThat(att.equals(attu)).isFalse();
    assertThat(att.hashCode() == attu.hashCode()).isFalse();

    Attribute att2 = Attribute.builder().setName("name").setNumericValue(123, true).build();
    assertThat(att.equals(att2)).isTrue();
    assertThat(att.hashCode() == att2.hashCode()).isTrue();

    Attribute attNoValue = Attribute.builder().setName("name").build();
    assertThat(attNoValue.equals("name")).isFalse();
  }

  @Test
  public void testSetValues() {
    Array data = Array.makeArray(DataType.FLOAT, ImmutableList.of("3.14", ".0015"));
    Attribute att = Attribute.builder().setName("name").setValues(data).build();
    assertThat(att).isEqualTo(Attribute.fromArray("name", data));
    assertThat(att).isEqualTo(att.toBuilder().build());

    Array values = att.getValues();
    assertThat(values.equals(data)).isFalse(); // Array.equals !!!
    assertThat(MAMath.equals(values, data)).isTrue();

    Attribute attNullArray = Attribute.builder().setName("name").setValues(null).build();
    assertThat(attNullArray).isEqualTo(Attribute.emptyValued("name", DataType.STRING));
  }

  @Test
  public void testSetArrayValues() {
    ucar.array.Array<?> data = Arrays.factory(ArrayType.FLOAT, new int[] {2}, new float[] {3.14f, .001f});
    Attribute att = Attribute.builder().setName("name").setArrayValues(data).build();
    assertThat(att).isEqualTo(Attribute.fromArray("name", data));
    assertThat(att).isEqualTo(att.toBuilder().build());

    ucar.array.Array<?> values = att.getArrayValues();
    assertThat(values.equals(data)).isTrue();

    Attribute attNullArray = Attribute.builder().setName("name").setArrayValues(null).build();
    assertThat(attNullArray).isEqualTo(Attribute.emptyValued("name", DataType.STRING));
  }

  @Test
  public void testSetValuesChar() {
    char[] carray = new char[] {'a', 'b', 'd'};
    int[] shape = new int[] {3};
    Array data = Array.factory(DataType.CHAR, shape, carray);

    Attribute att = Attribute.builder().setName("name").setValues(data).build();
    assertThat(att).isEqualTo(new Attribute("name", "abd"));
  }

  @Test
  public void testSetValuesCharArray() {
    char[] carray = new char[] {'a', 'b', 'd', 'g'};
    int[] shape = new int[] {2, 2};
    Array data = Array.factory(DataType.CHAR, shape, carray);

    Attribute att = Attribute.builder().setName("name").setValues(data).build();
    assertThat(att.getDataType()).isEqualTo(DataType.STRING);
    assertThat(att.getLength()).isEqualTo(2);
    assertThat(att.getStringValue(0)).isEqualTo("ab");
    assertThat(att.getStringValue(1)).isEqualTo("dg");
  }

  /*
   * LOOK are we allowing opaque atttributes?
   * 
   * @Test
   * public void testSetValuesOpaque() {
   * ByteBuffer bb1 = ByteBuffer.allocate(11);
   * int[] shape = new int[] {1};
   * Object[] adata = new Object[] {bb1};
   * Array data = Array.factory(DataType.OPAQUE, shape, adata);
   * 
   * Attribute att = Attribute.builder().setName("name").setValues(data).build();
   * assertThat(att.getDataType()).isEqualTo(DataType.BYTE);
   * assertThat(att.getLength()).isEqualTo(11);
   * }
   * 
   * @Test
   * public void testSetValuesOpaqueArray() {
   * ByteBuffer bb1 = ByteBuffer.allocate(11);
   * ByteBuffer bb2 = ByteBuffer.allocate(1);
   * int[] shape = new int[] {2};
   * Object[] adata = new Object[] {bb1, bb2};
   * Array data = Array.factory(DataType.OPAQUE, shape, adata);
   * 
   * Attribute att = Attribute.builder().setName("name").setValues(data).build();
   * assertThat(att.getDataType()).isEqualTo(DataType.BYTE);
   * assertThat(att.getLength()).isEqualTo(12); // TODO Seems wrong
   * }
   */

  @Test
  public void testSetValuesFromByteArray() {
    ByteBuffer bb1 = ByteBuffer.allocate(22);
    ShortBuffer sb = bb1.asShortBuffer();
    for (int i = 0; i < 11; i++) {
      sb.put((short) i);
    }
    int[] shape = new int[] {11};
    Array data = Array.factory(DataType.SHORT, shape, bb1);

    Attribute att = Attribute.builder().setName("name").setValues(data).build();
    assertThat(att.getDataType()).isEqualTo(DataType.SHORT);
    assertThat(att.getLength()).isEqualTo(11);
    for (int i = 0; i < att.getLength(); i++) {
      assertThat(att.getNumericValue(i).equals((short) i));
    }
  }

  // Demonstrates GitHub issue #715: https://github.com/Unidata/thredds/issues/715
  @Test
  public void testLargeLongValue() {
    Attribute att = new Attribute("name", NetcdfFormatUtils.NC_FILL_INT64); // which is -9223372036854775806L
    long result = att.getNumericValue().longValue(); // returned -9223372036854775808L, before bug fix.

    Assert.assertEquals(NetcdfFormatUtils.NC_FILL_INT64, result);
  }

  @Test
  public void testStringBuilderEquals() {
    Attribute att = Attribute.builder().setName("name").setStringValue("svalue").build();
    assertThat(att).isEqualTo(new Attribute("name", "svalue"));
    Attribute att2 = att.toBuilder().setName("name2").build();
    assertThat(att2).isEqualTo(new Attribute("name2", "svalue"));
  }

  @Test
  public void testSetValuesList() {
    Attribute att = Attribute.builder().setName("name").setValues(ImmutableList.of(1, 2, 3), true).build();
    assertThat(att.getDataType()).isEqualTo(DataType.UINT);

    Attribute atts = Attribute.builder().setName("name").setValues(ImmutableList.of("1", "2", "3"), false).build();
    assertThat(atts.getDataType()).isEqualTo(DataType.STRING);

    List<String> vals2 = ImmutableList.of("1", "2", "3");
    // wont compile
    // Attribute atts2 = Attribute.builder().setName("name").setValues(vals2).build();
    // wont compile
    // Attribute atts21 = Attribute.builder().setName("name").setValues((List<Object>) vals2).build();
    Attribute atts22 = Attribute.builder().setName("name").setValues((List) vals2, false).build();
    assertThat(atts22.getDataType()).isEqualTo(DataType.STRING);

    Array array = Array.factory(DataType.SHORT, new int[] {4}, new short[] {1, 2, 3, 4});
    Attribute att2 = Attribute.builder().setName("name").setValues(array).build();
    assertThat(att2.getDataType()).isEqualTo(DataType.SHORT);
    assertThat(MAMath.equals(att2.getValues(), array)).isTrue();

    Attribute ad = Attribute.builder().setName("name").setValues(ImmutableList.of(1.0, 2.0, 3.0), false).build();
    assertThat(ad.getDataType()).isEqualTo(DataType.DOUBLE);

    Attribute af = Attribute.builder().setName("name").setValues(ImmutableList.of(1.0f, 2.0f, 3.0f), false).build();
    assertThat(af.getDataType()).isEqualTo(DataType.FLOAT);

    Attribute ab = Attribute.builder().setName("name").setValues(ImmutableList.of((byte) -1, (byte) -2), false).build();
    assertThat(ab.getDataType()).isEqualTo(DataType.BYTE);

    Attribute abu = Attribute.builder().setName("name").setValues(ImmutableList.of((byte) -1, (byte) -2), true).build();
    assertThat(abu.getDataType()).isEqualTo(DataType.UBYTE);

    Attribute as = Attribute.builder().setName("name").setValues(ImmutableList.of((short) 1, (short) 2), false).build();
    assertThat(as.getDataType()).isEqualTo(DataType.SHORT);

    Attribute asu = Attribute.builder().setName("name").setValues(ImmutableList.of((short) 1, (short) 2), true).build();
    assertThat(asu.getDataType()).isEqualTo(DataType.USHORT);

    Attribute al = Attribute.builder().setName("name").setValues(ImmutableList.of((long) 3), false).build();
    assertThat(al.getDataType()).isEqualTo(DataType.LONG);

    Attribute alu = Attribute.builder().setName("name").setValues(ImmutableList.of((long) 3), true).build();
    assertThat(alu.getDataType()).isEqualTo(DataType.ULONG);
  }

  @Test
  public void testWriteCDL() {
    Attribute att = Attribute.builder().setName("name").setValues(ImmutableList.of(1, 2, 3), true).build();
    Formatter f = new Formatter();
    att.writeCDL(f, false, null);
    assertThat(f.toString()).isEqualTo(":name = 1U, 2U, 3U");
  }

  @Test
  public void testWriteCDLString() {
    Attribute att = Attribute.builder().setName("name").setStringValue("svalue").build();
    Formatter f = new Formatter();
    att.writeCDL(f, false, null);
    assertThat(f.toString()).isEqualTo(":name = \"svalue\"");
  }

  @Test
  public void testWriteCDLEnum() {
    Map<Integer, String> map = ImmutableMap.of(123, "enum123");
    EnumTypedef typedef = new EnumTypedef("enum", map);
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).setEnumType(typedef).build();

    Formatter f = new Formatter();
    att.writeCDL(f, false, null);
    assertThat(f.toString()).isEqualTo(":name = \"enum123\"");
  }

  // TODO What is the correct behavior?
  @Test
  public void testWriteCDLBadEnum() {
    Map<Integer, String> map = ImmutableMap.of(123, "enum123");
    EnumTypedef typedef = new EnumTypedef("enum", map);
    Attribute att = Attribute.builder().setName("name").setNumericValue(678, true).setEnumType(typedef).build();

    Formatter f = new Formatter();
    att.writeCDL(f, false, null);
    assertThat(f.toString()).isEqualTo(":name = \"678\"");
  }

}
