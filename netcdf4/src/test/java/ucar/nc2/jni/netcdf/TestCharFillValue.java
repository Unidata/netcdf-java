/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.iosp.hdf5.H5header;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;


/*
 * The code that demonstrates the problem can be found below. The issue is due
 * to the special checks we do for fill value in Nc4Iosp, specifically around
 * line 2822 and line 2841. Thanks for taking a look!
 */
public class TestCharFillValue {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private final String charVarName = "charVar";
  private final char charFillValue = 'F';
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
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filename, null);
    Dimension charDim = writerb.addDimension("charDim", 3);
    Variable.Builder<?> charVar = writerb.addVariable(charVarName, ArrayType.CHAR, charDim.getShortName());

    Array<?> charArray = makeFromString(charFillValue, 3);
    charVar.addAttribute(Attribute.fromArray("charAttrName", charArray));
    Array<?> charArrayFillValue = makeFromString(charFillValue, 1);
    Attribute charAttrFillValue = Attribute.fromArray("_FillValue", charArrayFillValue);
    charVar.addAttribute(charAttrFillValue);

    try (NetcdfFormatWriter writer = writerb.build()) {
      // empty
    }
    try (NetcdfFile ncf = NetcdfFiles.open(filename)) {
      Variable charVarFromFile = ncf.findVariable(charVarName);
      H5header.Vinfo h5 = (H5header.Vinfo) charVarFromFile.getSPobject();
      System.out.printf("use fill value: %s%n", h5.useFillValue());
    }


    try (NetcdfDataset ncf = NetcdfDatasets.openDataset(filename)) {
      Variable charVarFromFile = ncf.findVariable(charVarName);
      // should be 3 charFillVal characters
      Array<Byte> arr = (Array<Byte>) charVarFromFile.readArray();
      System.out.printf("expected fill value: |%s|%n", charFillValue);
      System.out.printf("actual filled value: |%s|%n", Arrays.makeStringFromChar(arr));
      for (Byte c : arr) {
        assertThat(c).isEqualTo(charFillValue);
      }
    }
  }

  // Re: https://github.com/Unidata/thredds/pull/1262

  @Test
  public void testNullCharFillValue() throws IOException {
    logger.info("\n*** Test Null Character Fill Value");

    String filename = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filename, null);

    Dimension charDim = writerb.addDimension("charDim", 3);
    Variable.Builder<?> charVar = writerb.addVariable(charVarName, ArrayType.CHAR, charDim.getShortName());
    Array<?> charArrayFillValue = makeFromString(charNullFillValue, 1);
    Attribute charAttrFillValue = Attribute.fromArray("_FillValue", charArrayFillValue);
    charVar.addAttribute(charAttrFillValue);

    try (NetcdfFormatWriter writer = writerb.build()) {
      // empty
    }

    try (NetcdfFile ncf = NetcdfFiles.open(filename)) {
      Variable charVarFromFile = ncf.findVariable(charVarName);
      H5header.Vinfo h5 = (H5header.Vinfo) charVarFromFile.getSPobject();
      logger.debug("use fill value: {}", h5.useFillValue());
      // should be 3 charFillVal characters
      Array<Byte> arr = (Array<Byte>) charVarFromFile.readArray();
      logger.debug("expected fill value: |{}|", arr.get(0) == '\0' ? "" : charNullFillValue);
      logger.debug("actual fill value: |");
      for (Byte c : arr) {
        if (c != '\0') {
          logger.debug("{}", c);
        }
      }
      for (Byte c : arr) {
        assertThat(charNullFillValue.charAt(0)).isEqualTo(c);
      }
    }
  }

  public static Array<?> makeFromString(String s, int max) {
    char[] cvalue = new char[max];
    for (int i = 0; i < max && i < s.length(); i++) {
      cvalue[i] = s.charAt(i);
    }
    return Arrays.factory(ArrayType.CHAR, new int[] {max}, cvalue);
  }

  public static Array<Character> makeFromString(char c, int max) {
    char[] bval = new char[max];
    for (int i = 0; i < max; i++) {
      bval[i] = c;
    }
    return Arrays.factory(ArrayType.CHAR, new int[] {max}, bval);
  }

}
