/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.ArrayDouble;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.write.Nc4ChunkingStrategy;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test miscellaneous netcdf4 writing */
public class TestNc4Misc {

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

  // from Jeff Johnson jeff.m.johnson@noaa.gov 5/2/2014
  @Test
  public void testChunkStandard() throws IOException, InvalidRangeException {
    // define the file
    String location = tempFolder.newFile().getAbsolutePath();

    Nc4Chunking chunkingStrategy = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, false);
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, chunkingStrategy);

    Dimension timeDim = writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", DataType.DOUBLE, ImmutableList.of(timeDim))
        .addAttribute(new Attribute("units", "milliseconds since 1970-01-01T00:00:00Z"));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // create 1-D arrays to hold data values (time is the dimension)
      ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);

      int[] origin = new int[] {0};
      long startTime = 1398978611132L;

      // write the records to the file
      for (int i = 0; i < 10000; i++) {
        // load data into array variables
        double value = startTime++;
        timeArray.set(timeArray.getIndex(), value);

        origin[0] = i;

        // write a record
        writer.write("time", origin, timeArray);
      }

    }

    File resultFile = new File(location);
    logger.debug("Wrote data file {} size={}", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 : resultFile.length();

    try (NetcdfFile file = NetcdfFiles.open(location)) {
      Variable time = file.findVariable("time");
      Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
      assert chunk != null;
      assert chunk.getNumericValue().equals(1024) : "chunk failed= " + chunk;
    }
  }

  @Ignore("remove ability to set chunk with attribute 2/10/2016")
  @Test
  public void testChunkFromAttribute() throws IOException, InvalidRangeException {
    // define the file
    String location = tempFolder.newFile().getAbsolutePath();

    Nc4Chunking chunkingStrategy = Nc4ChunkingStrategy.factory(Nc4Chunking.Strategy.standard, 0, false);
    NetcdfFormatWriter.Builder writerb =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, chunkingStrategy);

    Dimension timeDim = writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", DataType.DOUBLE, ImmutableList.of(timeDim))
        .addAttribute(new Attribute("units", "milliseconds since 1970-01-01T00:00:00Z"))
        .addAttribute(new Attribute(CDM.CHUNK_SIZES, 2000));

    try (NetcdfFormatWriter writer = writerb.build()) {
      // create 1-D arrays to hold data values (time is the dimension)
      ArrayDouble.D1 timeArray = new ArrayDouble.D1(1);

      int[] origin = new int[] {0};
      long startTime = 1398978611132L;

      // write the records to the file
      for (int i = 0; i < 10000; i++) {
        // load data into array variables
        double value = startTime++;
        timeArray.set(timeArray.getIndex(), value);

        origin[0] = i;

        // write a record
        writer.write("time", origin, timeArray);
      }
    }

    File resultFile = new File(location);
    logger.debug("Wrote data file {} size={}", location, resultFile.length());
    assert resultFile.length() < 100 * 1000 : resultFile.length();

    try (NetcdfFile file = NetcdfFiles.open(location)) {
      Variable time = file.findVariable("time");
      Attribute chunk = time.findAttribute(CDM.CHUNK_SIZES);
      assert chunk != null;
      assert chunk.getNumericValue().equals(2000) : "chunk failed= " + chunk;
    }
  }


  /*
   * from peter@terrenus.ca
   * > > Writing version netcdf3
   * > > Jan 22, 2015 12:27:49 PM ncsa.hdf.hdf5lib.H5 loadH5Lib
   * > > INFO: HDF5 library: jhdf5
   * > > Jan 22, 2015 12:27:49 PM ncsa.hdf.hdf5lib.H5 loadH5Lib
   * > > INFO: successfully loaded from java.library.path
   * > > Found HDF 5 version = false
   * > > file.exists() = true
   * > > file.length() = 65612
   * > > file.delete() = true testGri
   * > > Writing version netcdf4
   * > > Netcdf nc_inq_libvers='4.3.2 of Oct 20 2014 09:49:08 $' isProtected=false
   * > > Exception in thread "main" java.io.IOException: -101: NetCDF: HDF error
   * > > at ucar.nc2.jni.netcdf.Nc4Iosp.create(Nc4Iosp.java:2253)
   * > > at ucar.nc2.NetcdfFileWriter.create(NetcdfFileWriter.java:794)
   * > > at NetCDFTest.main(NetCDFTest.java:39)
   */
  @Test
  public void testFileType3() throws Exception {
    testFileType(NetcdfFileFormat.NETCDF3);
  }

  @Test
  public void testFileType4() throws Exception {
    testFileType(NetcdfFileFormat.NETCDF4);
  }

  private void testFileType(NetcdfFileFormat format) throws Exception {
    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.builder().setFormat(format).setLocation(fileName);
    writerb.addVariable("coord_ref", DataType.INT, "");
    try (NetcdfFormatWriter writer = writerb.build()) {
    }

    /** open the file and see what format. */
    try (NetcdfFile ncfile = NetcdfFiles.open(fileName)) {
      assertThat(ncfile.getFileTypeId()).ignoringCase().isEqualTo(format.formatName());
      System.out.printf(" FileTypeId= '%s'%n", ncfile.getFileTypeId());
      System.out.printf(" FileTypeDescription= '%s'%n", ncfile.getFileTypeDescription());
      System.out.printf(" FileTypeVersion= '%s'%n%n", ncfile.getFileTypeVersion());
    }
  }

  // Demonstrates GitHub issue #718: https://github.com/Unidata/thredds/issues/718
  @Test
  public void testCloseNc4inDefineMode() throws IOException {
    String location = tempFolder.newFile().getAbsolutePath();
    Nc4Chunking chunking = Nc4ChunkingDefault.factory(Nc4Chunking.Strategy.standard, 5, true);

    // Should be able to open and close file without an exception. Would fail before the bug fix in this commit.
    try (NetcdfFormatWriter writer =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, chunking).build()) {
    }
  }
}
