/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import static com.google.common.truth.Truth.assertThat;

/** Test miscellaneous netcdf4 writing */
public class TestNc4OpenExisting {

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

  @Test
  @Ignore("needs ucar.nc2.jni.netcdf.Nc4updater")
  public void testUnlimitedDimension() throws Exception {
    String location = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder<?> writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, null);
    System.out.printf("write to file = %s%n", new File(location).getAbsolutePath());

    Dimension timeDim = writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", ArrayType.DOUBLE, ImmutableList.of(timeDim));

    Array<?> data = Arrays.factory(ArrayType.DOUBLE, new int[] {4}, new double[] {0, 1, 2, 3});
    try (NetcdfFormatWriter writer = writerb.build()) {
      writer.write(writer.findVariable("time"), data.getIndex(), data);
    }

    Index origin = Index.ofRank(0);
    NetcdfFormatWriter.Builder<?> existingb = NetcdfFormatWriter.openExisting(location);
    try (NetcdfFormatWriter existing = existingb.build()) {
      Variable time = existing.findVariable("time");
      existing.write(time, origin.set((int) time.getSize()), data);
    }

    try (NetcdfFile file = NetcdfFiles.open(location)) {
      Variable time = file.findVariable("time");
      assertThat(time).isNotNull();
      assertThat(time.getSize()).isEqualTo(8);
    }
  }
}
