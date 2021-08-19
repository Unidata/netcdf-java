/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test String Attributes */
public class TestNullStringAttr {

  private static NetcdfFile ncf;
  private static final String testFile = TestDir.cdmLocalTestDataDir + "hdf5/string_attrs.nc4";

  @BeforeClass
  public static void openFile() throws IOException {
    ncf = NetcdfFiles.open(testFile);
  }

  @Test
  public void TestNullStringGlobalAttr() {
    Attribute attr = ncf.findGlobalAttribute("NULL_STR_GATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void TestEmptyStringGlobalAttr() {
    Attribute attr = ncf.findGlobalAttribute("EMPTY_STR_GATTR");
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @Test
  public void TestNullStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    Attribute attr = testVar.findAttribute("NULL_STR_ATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void TestEmptyStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    Attribute attr = testVar.findAttribute("EMPTY_STR_ATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @AfterClass
  public static void closeFile() throws IOException {
    ncf.close();
  }
}
