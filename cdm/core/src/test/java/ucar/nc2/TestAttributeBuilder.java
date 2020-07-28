package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.DataType;

public class TestAttributeBuilder {

  @Test
  public void testBuilder() {
    Attribute att = Attribute.builder().setName("name").setDataType(DataType.FLOAT)
        .setValues(Array.makeArray(DataType.FLOAT, ImmutableList.of("3.14", ".0015"))).build();
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
  }

  @Test
  public void testArrayConstructor() {
    Array data = Array.makeArray(DataType.FLOAT, ImmutableList.of("3.14", ".0015"));
    Attribute att = Attribute.builder().setName("name").setValues(data).build();
    assertThat(att).isEqualTo(Attribute.fromArray("name", data));
  }

  @Test
  public void testNumericConstructor() {
    Attribute att = Attribute.builder().setName("name").setNumericValue(123, true).build();
    assertThat(att.getDataType().isUnsigned()).isTrue();
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
  }

}
