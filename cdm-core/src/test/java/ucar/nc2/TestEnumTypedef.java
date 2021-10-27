/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import ucar.array.ArrayType;

/** Test {@link ucar.nc2.EnumTypedef} */
public class TestEnumTypedef {

  @Test
  public void testBasics() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef = new EnumTypedef("enumName", map);

    assertThat(typedef.getShortName()).isEqualTo("enumName");
    assertThat(typedef.getBaseArrayType()).isEqualTo(ArrayType.ENUM4);
    assertThat(typedef.getMap()).isEqualTo(map);

    assertThat(typedef.lookupEnumString(1)).isEqualTo("name1");
    assertThat(typedef.lookupEnumString(99)).isEqualTo(null);

    assertThat(typedef.lookupEnumInt("name3")).isEqualTo(3);
    assertThat(typedef.lookupEnumInt("name6")).isEqualTo(null);
  }

  @Test
  public void testWriteCDL() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef = new EnumTypedef("typeName", map);
    assertThat(typedef.toString()).isEqualTo("enum typeName { 'name1' = 1, 'name2' = 2, 'name3' = 3};");

    EnumTypedef typedef1 = new EnumTypedef("typeName", map, ArrayType.ENUM1);
    assertThat(typedef1.toString()).isEqualTo("byte enum typeName { 'name1' = 1, 'name2' = 2, 'name3' = 3};");

    EnumTypedef typedef2 = new EnumTypedef("typeName", map, ArrayType.ENUM2);
    assertThat(typedef2.toString()).isEqualTo("short enum typeName { 'name1' = 1, 'name2' = 2, 'name3' = 3};");
  }

  @Test
  public void testEquals() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 3, "name3");
    EnumTypedef typedef1 = new EnumTypedef("typeName", map);

    Map<Integer, String> map2 = ImmutableMap.of(2, "name2", 3, "name3", 1, "name1");
    EnumTypedef typedef2 = new EnumTypedef("typeName", map2);

    assertThat(map.equals(map2)).isTrue();
    assertThat(typedef1.equals(typedef2)).isTrue();
    assertThat(typedef1.hashCode() == typedef2.hashCode()).isTrue();
  }

  @Test
  public void testValidate() {
    Map<Integer, String> map = ImmutableMap.of(1, "name1", 2, "name2", 1000, "name3");
    try {
      new EnumTypedef("typeName1", map, ArrayType.ENUM1);
      fail();
    } catch (Exception e) {
      // expected
    }

    Map<Integer, String> map2 = ImmutableMap.of(1, "name1", 2, "name2", 100000, "name3");
    try {
      new EnumTypedef("typeName2", map2, ArrayType.ENUM2);
      fail();
    } catch (Exception e) {
      // expected
    }
  }


}
