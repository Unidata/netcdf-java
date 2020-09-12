/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import com.google.common.collect.ImmutableList;
import java.util.Optional;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
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
import ucar.unidata.util.test.Assert2;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.*;

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

  @Test
  @Ignore("openExisting not working yet")
  public void testUnlimitedDimension() throws IOException, InvalidRangeException {
    String location = tempFolder.newFile().getAbsolutePath();

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, location, null);
    System.out.printf("write to file = %s%n", new File(location).getAbsolutePath());

    Dimension timeDim = writerb.addUnlimitedDimension("time");
    writerb.addVariable("time", DataType.DOUBLE, ImmutableList.of(timeDim));

    Array data = Array.makeFromJavaArray(new double[] {0, 1, 2, 3});
    try (NetcdfFormatWriter writer = writerb.build()) {
      writer.write("time", data);
    }

    NetcdfFormatWriter.Builder existingb = NetcdfFormatWriter.openExisting(location);
    try (NetcdfFormatWriter existing = existingb.build()) {
      Variable time = existing.findVariable("time");
      int[] origin = new int[1];
      origin[0] = (int) time.getSize();
      existing.write("time", origin, data);
    }

    try (NetcdfFile file = NetcdfFiles.open(location)) {
      Variable time = file.findVariable("time");
      assert time.getSize() == 8 : "failed to append to unlimited dimension";
    }
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
  public void testInvalidCfdid() throws Exception {
    testInvalidCfdid(NetcdfFileFormat.NETCDF3);
    testInvalidCfdid(NetcdfFileFormat.NETCDF4);
  }

  private void testInvalidCfdid(NetcdfFileFormat format) throws Exception {

    logger.debug("Writing version {}", format);

    /**
     * Create the file writer and reserve some extra space in the header
     * so that we can switch between define mode and write mode without
     * copying the file (this may work or may not).
     */
    String fileName = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.builder().setFormat(format).setLocation(fileName);
    writerb.setExtraHeader(64 * 1024); // LOOK: why?

    /**
     * Create a variable in the root group.
     */
    writerb.addVariable("coord_ref", DataType.INT, "");

    /**
     * Now create the file and close it.
     */
    try (NetcdfFormatWriter writer = writerb.build()) {
      //
    }

    logger.debug("File written {}", fileName);

    /**
     * Now we're going to detect the file format using the HDF 5 library.
     */
    // LOOK wheres the beef?

    /**
     * Now delete the file, getting ready for the next format.
     */
    File file = new File(fileName);
    logger.debug("file.exists() = {}", file.exists());
    logger.debug("file.length() = {}", file.length());
    logger.debug("file.delete() = {}", file.delete());

  } // for

  @Test
  @Ignore("Netcdf-4 rename not working yet")
  public void testAttributeChangeNc4() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalFromTestDataDir + "dataset/testRename.nc4");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }

  @Test
  @Ignore("Netcdf-3 rename not working yet")
  public void testAttributeChangeNc3() throws IOException {
    Path source = Paths.get(TestDir.cdmLocalFromTestDataDir + "dataset/testRename.nc3");
    Path target = tempFolder.newFile().toPath();
    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    doRename(target.toString());
  }

  private void doRename(String filename) throws IOException {
    logger.debug("Rename {}", filename);
    // old and new name of variable
    String oldVarName = "Pressure_reduced_to_MSL_msl";
    String newVarName = "Pressure_MSL";
    // name and value of attribute to change
    String attrToChange = "long_name";
    String newAttrValue = "Long name changed!";
    Array orgData;

    NetcdfFormatWriter.Builder writerb = NetcdfFormatWriter.openExisting(filename).setFill(false);
    Optional<Variable.Builder<?>> newVar = writerb.renameVariable(oldVarName, newVarName);
    newVar.ifPresent(vb -> vb.addAttribute(new Attribute(attrToChange, newAttrValue)));

    // write the above changes to the file
    try (NetcdfFormatWriter writer = writerb.build()) {
      Variable var = writer.findVariable(newVarName);
      orgData = var.read();
    }

    // check that it worked
    try (NetcdfFile ncd = NetcdfFiles.open(filename)) {
      Variable var = ncd.findVariable(newVarName);
      Assert.assertNotNull(var);
      String attValue = var.findAttributeString(attrToChange, "");
      Assert.assertEquals(attValue, newAttrValue);

      Array data = var.read();
      logger.debug("{}", data);
      orgData.resetLocalIterator();
      data.resetLocalIterator();
      while (data.hasNext() && orgData.hasNext()) {
        float val = data.nextFloat();
        float orgval = orgData.nextFloat();
        Assert2.assertNearlyEquals(orgval, val);
      }
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
