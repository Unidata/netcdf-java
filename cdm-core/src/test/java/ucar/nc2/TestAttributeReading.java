/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import static com.google.common.truth.Truth.assertThat;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.MAMath;
import ucar.nc2.iosp.NetcdfFormatUtils;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test reading attributes */
public class TestAttributeReading {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

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

    Array aa = att.getValues();
    assert (att.getDataType() == DataType.INT);
    assert (aa.getElementType() == int.class);
    assert (aa.getSize() == 3);

    att = temp.findAttribute("versionD");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1.2 == att.getNumericValue().doubleValue());
    assert (DataType.DOUBLE == att.getDataType());

    aa = att.getValues();
    assert (att.getDataType() == DataType.DOUBLE);
    assert (aa.getElementType() == double.class);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionF");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1.2f == att.getNumericValue().floatValue());
    assert (Misc.nearlyEquals(1.2, att.getNumericValue().doubleValue(), 1.0e-5));
    assert (DataType.FLOAT == att.getDataType());

    aa = att.getValues();
    assert (att.getDataType() == DataType.FLOAT);
    assert (aa.getElementType() == float.class);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionI");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (1 == att.getNumericValue().intValue());
    assert (DataType.INT == att.getDataType());

    aa = att.getValues();
    assert (att.getDataType() == DataType.INT);
    assert (aa.getElementType() == int.class);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionS");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (2 == att.getNumericValue().shortValue());
    assert (DataType.SHORT == att.getDataType());

    aa = att.getValues();
    assert (att.getDataType() == DataType.SHORT);
    assert (aa.getElementType() == short.class);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionB");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (3 == att.getNumericValue().byteValue());
    assert (DataType.BYTE == att.getDataType());

    aa = att.getValues();
    assert (att.getDataType() == DataType.BYTE);
    assert (aa.getElementType() == byte.class);
    assert (aa.getSize() == 1);

    att = temp.findAttribute("versionString");
    assert (null != att);
    assert (!att.isArray());
    assert (1 == att.getLength());
    assert (DataType.STRING == att.getDataType());

    Number n = att.getNumericValue();
    assert (n != null);

    ncfile.close();
  }
}
