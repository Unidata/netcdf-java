/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.ArrayType;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.EnumTypedef;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.write.Nc4ChunkingStrategyNone;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/** Test copying files with enums to netcdf4. */
public class TestNc4EnumWriting {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  /*
   * ncdump C:/temp/writeEnumType.nc4
   * netcdf C\:/temp/writeEnumType {
   * types:
   * short enum dessertType {pie = 18, cake = 3284, donut = 268} ;
   * dimensions:
   * time = UNLIMITED ; // (3 currently)
   * variables:
   * dessertType dessert(time) ;
   * data:
   *
   * dessert = pie, donut, cake ;
   * }
   *
   * netcdf writeEnumType {
   * types:
   * short enum dessertType { 'pie' = 18, 'donut' = 268, 'cake' = 3284};
   * enum dessert { 'pie' = 18, 'donut' = 268, 'cake' = 3284};
   *
   * dimensions:
   * time = UNLIMITED; // (3 currently)
   * variables:
   * enum dessert dessert(time=3);
   * :_ChunkSizes = 4096U; // uint
   *
   * // global attributes:
   *
   * data:
   * dessert =
   * {18, 268, 3284}
   * }
   *
   */


  @Test // "See issue #352")
  public void writeEnumType() throws IOException {
    String filenameOut = File.createTempFile("writeEnumType", ".nc").getAbsolutePath();
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filenameOut, new Nc4ChunkingStrategyNone());

    // Create shared, unlimited Dimension
    Dimension timeDim = new Dimension("time", 3, true, true, false);
    writerb.addDimension(timeDim);

    // Create a map from integers to strings.
    Map<Integer, String> enumMap = new HashMap<>();
    enumMap.put(18, "pie");
    enumMap.put(268, "donut");
    enumMap.put(3284, "cake");

    // Create EnumTypedef and add it to root group.
    EnumTypedef dessertType = new EnumTypedef("dessertType", enumMap, ArrayType.ENUM2);
    writerb.getRootGroup().addEnumTypedef(dessertType);

    // Create Variable of type dessertType.
    Variable.Builder<?> dessert = writerb.addVariable("dessert", ArrayType.ENUM2, "time");
    dessert.setEnumTypeName(dessertType.getShortName());

    try (NetcdfFormatWriter writer = writerb.build()) {
      short[] dessertStorage = new short[] {18, 268, 3284};
      Array<?> data = Arrays.factory(ArrayType.SHORT, new int[] {3}, dessertStorage);
      writer.write(writer.findVariable("dessert"), data.getIndex(), data);
    } catch (InvalidRangeException e) {
      e.printStackTrace();
      fail();
    }

    boolean ok = TestNc4reader.doCompare(filenameOut, false, false, true);
    assertThat(ok).isTrue();
  }
}
