/*
 * Copyright (c) 2021 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

/** Test String Attributes */
@RunWith(Parameterized.class)
public class TestNullStringAttr {

  private static NetcdfFile ncfOld, ncfNew;
  private static final String testFile = TestDir.cdmLocalTestDataDir + "hdf5/string_attrs.nc4";

  private final NetcdfFile ncf;

  public TestNullStringAttr(String apiType, NetcdfFile ncf) {
    this.ncf = ncf;
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<?> netcdfFileObjects() throws IOException {
    ncfOld = NetcdfFile.open(testFile);
    ncfNew = NetcdfFiles.open(testFile);
    return Arrays.asList(new Object[][] {{"Old API", ncfOld}, {"New API", ncfNew},});
  }

  @Test
  public void testNullStringGlobalAttr() {
    Attribute attr = ncf.findGlobalAttribute("NULL_STR_GATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void testEmptyStringGlobalAttr() {
    Attribute attr = ncf.findGlobalAttribute("EMPTY_STR_GATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @Test
  public void testNullStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    assert null != testVar;
    Attribute attr = testVar.findAttribute("NULL_STR_ATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isNotEmpty();
    assertThat(value).isNotEqualTo("");
    assertThat(value).isEqualTo("NIL");
  }

  @Test
  public void testEmptyStringAttr() {
    Variable testVar = ncf.findVariable("/var");
    assert null != testVar;
    Attribute attr = testVar.findAttribute("EMPTY_STR_ATTR");
    assertThat(attr).isNotNull();
    String value = attr.getStringValue();
    assertThat(value).isNotNull();
    assertThat(value).isEmpty();
    assertThat(value).isEqualTo("");
  }

  @AfterClass
  public static void closeFile() throws IOException {
    ncfOld.close();
    ncfNew.close();
  }
}
