/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.write.Nc4ChunkingStrategyNone;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

/** Test writing multiple unlimited dimensions to netcdf4. */
public class TestNc4ExpandUnlimitedDimension {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  @Test
  @Ignore("not ready")
  public void expandUnlimitedDimensions() throws IOException, InvalidRangeException {
    File outFile = tempFolder.newFile();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4,
        outFile.getAbsolutePath(), new Nc4ChunkingStrategyNone());

    writerb.addDimension(Dimension.builder().setName("row").setIsUnlimited(true).build());
    writerb.addDimension(Dimension.builder().setName("col").setIsUnlimited(true).build());
    writerb.addVariable("table", ArrayType.INT, "row col");

    try (NetcdfFormatWriter writer = writerb.build()) {
      // Start with a 1x1 block. Table will look like:
      // 1
      Index origin = Index.ofRank(2);
      int[] shape = new int[] {1, 1};
      int[] data = new int[] {1};
      Variable table = writer.findVariable("table");
      writer.write(table, Index.ofRank(2), Arrays.factory(ArrayType.INT, shape, data));

      // Add a row. Table will look like:
      // 1 _
      // 2 2
      origin.set(1, 0);
      shape = new int[] {1, 2};
      data = new int[] {2, 2};
      writer.write(table, origin, Arrays.factory(ArrayType.INT, shape, data));

      // Add a column. Table will look like:
      // 1 _ 3
      // 2 2 3
      // _ _ 3
      origin.set(0, 2);
      shape = new int[] {3, 1};
      data = new int[] {3, 3, 3};
      writer.write(table, origin, Arrays.factory(ArrayType.INT, shape, data));

      // Add a row. Table will look like:
      // 1 _ 3 _
      // 2 2 3 _
      // _ _ 3 _
      // 4 4 4 4
      origin.set(3, 0);
      shape = new int[] {1, 4};
      data = new int[] {4, 4, 4, 4};
      writer.write(table, origin, Arrays.factory(ArrayType.INT, shape, data));

      // Add a column. Table will look like:
      // 1 _ 3 _ 5
      // 2 2 3 _ 5
      // _ _ 3 _ 5
      // 4 4 4 4 5
      // _ _ _ _ 5
      origin.set(0, 4);
      shape = new int[] {5, 1};
      data = new int[] {5, 5, 5, 5, 5};
      writer.write(table, origin, Arrays.factory(ArrayType.INT, shape, data));
    } catch (IOException e) {
      if ("NetCDF: Start+count exceeds dimension bound".equals(e.getMessage())) {
        String m = String.format("This test requires netcdf-c 4.4.0+ Your version = %s", NetcdfClibrary.getVersion());
        throw new IOException(m, e);
      }
    }

    /*
     * File should look like:
     * netcdf expandUnlimitedDimensions {
     * dimensions:
     * row = UNLIMITED ; // (5 currently)
     * col = UNLIMITED ; // (5 currently)
     * variables:
     * int table(row, col) ;
     * data:
     * 
     * table =
     * {1, _, 3, _, 5},
     * {2, 2, 3, _, 5},
     * {_, _, 3, _, 5},
     * {4, 4, 4, 4, 5},
     * {_, _, _, _, 5} ;
     * }
     */

    try (NetcdfFile ncFile = NetcdfFiles.open(outFile.getAbsolutePath())) {
      Variable v = ncFile.getRootGroup().findVariableLocal("table");
      assertThat(v).isNotNull();
      Array<Integer> actualVals = (Array<Integer>) v.readArray();

      int fill = -2147483647; // See EnhanceScaleMissingImpl.NC_FILL_INT
      int[] expectedData = new int[] {1, fill, 3, fill, 5, 2, 2, 3, fill, 5, fill, fill, 3, fill, 5, 4, 4, 4, 4, 5,
          fill, fill, fill, fill, 5};
      Array expectedVals = Arrays.factory(ArrayType.INT, new int[] {5, 5}, expectedData);

      int count = 0;
      for (int val : actualVals) {
        assertThat(val).isEqualTo(expectedData[count++]);
      }
    }
  }
}
