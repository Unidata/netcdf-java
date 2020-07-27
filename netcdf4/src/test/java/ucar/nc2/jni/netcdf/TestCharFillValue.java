/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

/*
 * The code that demonstrates the problem can be found below. The issue is due
 * to the special checks we do for fill value in Nc4Iosp, specifically around
 * line 2822 and line 2841. Thanks for taking a look!
 */

package ucar.nc2.jni.netcdf;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.iosp.hdf5.H5headerNew;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.UnitTestCommon;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

public class TestCharFillValue extends UnitTestCommon {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String charVarName = "charVar";
  private final String charFillValue = "F";
  private final String charNullFillValue = "\0";

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  public void testCharFillValue() throws IOException {
    logger.info("*** Test Non-Null Character Fill Value");

    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filename, null);
    Dimension charDim = writerb.addDimension("charDim", 3);
    Variable.Builder charVar = writerb.addVariable(charVarName, DataType.CHAR, charDim.getShortName());
    // this works
    Array charArray = ArrayChar.makeFromString(charFillValue, 1);
    charVar.addAttribute(new Attribute("charAttrName", charArray));
    Array charArrayFillValue = ArrayChar.makeFromString(charFillValue, 1);
    Attribute charAttrFillValue;
    // Try to do _FillValue two ways
    if (true) {
      charAttrFillValue = new Attribute("_FillValue", charArrayFillValue);
    } else {
      charAttrFillValue =
          Attribute.builder().setName("_FillValue").setDataType(DataType.CHAR).setValues(charArrayFillValue).build();
    }
    charVar.addAttribute(charAttrFillValue);

    try (NetcdfFormatWriter writer = writerb.build()) {
      // empty
    }

    try (NetcdfFile ncf = NetcdfFiles.open(filename)) {
      Variable charVarFromFile = ncf.findVariable(charVarName);
      H5headerNew.Vinfo h5 = (H5headerNew.Vinfo) charVarFromFile.getSPobject();
      logger.debug("use fill value: {}", h5.useFillValue());
      // should be 3 charFillVal characters
      Array arr = charVarFromFile.read();
      char[] javaArr = (char[]) arr.get1DJavaArray(DataType.CHAR);
      String s = new String(javaArr);
      logger.debug("expected fill value: |{}|", charFillValue);
      logger.debug("actual filled value: |{}|", s);
      for (int i = 0; i < s.length(); i++) {
        Assert.assertTrue("position " + i, charFillValue.charAt(0) == s.charAt(i));
      }
    }
  }

  // Re: https://github.com/Unidata/thredds/pull/1262

  @Test
  public void testNullCharFillValue() throws IOException {
    logger.info("\n*** Test Null Character Fill Value");

    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filename, null);

    Dimension charDim = writerb.addDimension("charDim", 3);
    Variable.Builder charVar = writerb.addVariable(charVarName, DataType.CHAR, charDim.getShortName());
    Array charArrayFillValue = ArrayChar.makeFromString(charNullFillValue, 1);
    Attribute charAttrFillValue;
    charAttrFillValue = new Attribute("_FillValue", charArrayFillValue);
    charVar.addAttribute(charAttrFillValue);

    try (NetcdfFormatWriter writer = writerb.build()) {
      // empty
    }

    try (NetcdfFile ncf = NetcdfFiles.open(filename)) {
      Variable charVarFromFile = ncf.findVariable(charVarName);
      H5headerNew.Vinfo h5 = (H5headerNew.Vinfo) charVarFromFile.getSPobject();
      logger.debug("use fill value: {}", h5.useFillValue());
      // should be 3 charFillVal characters
      Array arr = charVarFromFile.read();
      char[] javaArr = (char[]) arr.get1DJavaArray(DataType.CHAR);
      String s = new String(javaArr);
      logger.debug("expected fill value: |{}|", s.charAt(0) == '\0' ? "" : charNullFillValue);
      logger.debug("actual fill value: |");
      for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) != '\0') {
          logger.debug("{}", s.charAt(i));
        }
      }
      logger.debug("|");
      for (int i = 0; i < s.length(); i++) {
        Assert.assertTrue("position " + i, charNullFillValue.charAt(0) == s.charAt(i));
      }
    }
  }
}
