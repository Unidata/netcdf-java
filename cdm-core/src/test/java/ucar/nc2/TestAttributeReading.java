/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

/** Test reading attributes */
public class TestAttributeReading {

  @Test
  public void testNC3ReadAttributes() throws IOException {
    NetcdfFile ncfile = TestDir.openFileLocal("testWrite.nc");

    // global attributes
    assert ("face".equals(ncfile.getRootGroup().findAttributeString("yo", "barf")));

    Variable temp = null;
    assert (null != (temp = ncfile.findVariable("temperature")));
    assert ("K".equals(temp.findAttributeString("units", "barf")));

    Attribute att = temp.findAttribute("scale");
    assert (null != att);
    assert (att.isArray());
    assert (3 == att.getLength());
    assert (3 == att.getNumericValue(2).intValue());

    Array<?> aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.INT);
    assert (aa.getSize() == 3);

    att = temp.findAttribute("versionD");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1.2 == att.getNumericValue().doubleValue());
    assert (ArrayType.DOUBLE == att.getArrayType());

    aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.DOUBLE);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionF");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1.2f == att.getNumericValue().floatValue());
    assert (Misc.nearlyEquals(1.2, att.getNumericValue().doubleValue(), 1.0e-5));
    assert (ArrayType.FLOAT == att.getArrayType());

    aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.FLOAT);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionI");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1 == att.getNumericValue().intValue());
    assert (ArrayType.INT == att.getArrayType());

    aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.INT);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionS");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (2 == att.getNumericValue().shortValue());
    assert (ArrayType.SHORT == att.getArrayType());

    aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.SHORT);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionB");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (3 == att.getNumericValue().byteValue());
    assert (ArrayType.BYTE == att.getArrayType());

    aa = att.getArrayValues();
    assert (att.getArrayType() == ArrayType.BYTE);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionString");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (ArrayType.STRING == att.getArrayType());

    Number n = att.getNumericValue();
    assert (n != null);

    ncfile.close();
  }
}
