/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test reading attributes */
public class TestAttributeReading {

  @Test
  public void testNC3ReadAttributes() throws IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    // global attributes
    assertThat(ncfile.getRootGroup().findAttributeString("yo", "barf")).isEqualTo("face");

    Variable temp = ncfile.findVariable("temperature");
    assertThat(temp).isNotNull();
    assertThat(temp.findAttributeString("units", "barf")).isEqualTo("K");

    Attribute att = temp.findAttribute("scale");
    assertThat(att).isNotNull();
    assertThat(att.isArray()).isTrue();
    assertThat(att.getLength()).isEqualTo(3);
    assertThat(att.getNumericValue(2).intValue()).isEqualTo(3);

    Array<?> aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.INT);
    assertThat(aa.getSize()).isEqualTo(3);

    att = temp.findAttribute("versionD");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(att.getLength()).isEqualTo(1);
    assertThat(att.getNumericValue().doubleValue()).isEqualTo(1.2);
    assertThat(ArrayType.DOUBLE).isEqualTo(att.getArrayType());

    aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.DOUBLE);
    assertThat(aa.getSize()).isEqualTo(1);

    att = temp.findAttribute("versionF");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(att.getLength()).isEqualTo(1);
    assertThat(att.getNumericValue().floatValue()).isEqualTo(1.2f);
    assertThat(Misc.nearlyEquals(1.2, att.getNumericValue().doubleValue(), 1.0e-5));
    assertThat(ArrayType.FLOAT).isEqualTo(att.getArrayType());

    aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.FLOAT);
    assertThat(aa.getSize()).isEqualTo(1);

    att = temp.findAttribute("versionI");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(att.getLength()).isEqualTo(1);
    assertThat(att.getNumericValue().intValue()).isEqualTo(1);
    assertThat(ArrayType.INT).isEqualTo(att.getArrayType());

    aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.INT);
    assertThat(aa.getSize()).isEqualTo(1);

    att = temp.findAttribute("versionS");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(1 == att.getLength());
    assertThat(2 == att.getNumericValue().shortValue());
    assertThat(ArrayType.SHORT).isEqualTo(att.getArrayType());

    aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.SHORT);
    assertThat(aa.getSize()).isEqualTo(1);

    att = temp.findAttribute("versionB");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(1).isEqualTo(att.getLength());
    assertThat(3).isEqualTo(att.getNumericValue().byteValue());
    assertThat(ArrayType.BYTE).isEqualTo(att.getArrayType());

    aa = att.getArrayValues();
    assertThat(att.getArrayType()).isEqualTo(ArrayType.BYTE);
    assertThat(aa.getSize()).isEqualTo(1);

    att = temp.findAttribute("versionString");
    assertThat(att).isNotNull();
    assertThat(!att.isArray()).isTrue();;
    assertThat(1).isEqualTo(att.getLength());
    assertThat(ArrayType.STRING).isEqualTo(att.getArrayType());

    Number n = att.getNumericValue();
    assertThat(n).isNotNull();

    ncfile.close();
  }
}
