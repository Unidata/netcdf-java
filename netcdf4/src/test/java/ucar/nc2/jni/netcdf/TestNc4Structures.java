/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import static org.junit.Assert.assertNotNull;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.*;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Test writing structure data into netcdf4.
 *
 * @author caron
 * @since 5/12/14
 */
@Category(NeedsCdmUnitTest.class)
public class TestNc4Structures {
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
  public void writeStructure() throws IOException, InvalidRangeException {
    String datasetIn = TestDir.cdmUnitTestDir + "formats/netcdf4/compound/tst_compounds.nc4";
    String datasetOut = tempFolder.newFile().getAbsolutePath();
    writeStructure(datasetIn, datasetOut);
  }

  private void writeStructure(String datasetIn, String datasetOut) throws IOException {
    System.out.printf("NetcdfDatataset read from %s write to %s %n", datasetIn, datasetOut);

    CancelTask cancel = CancelTask.create();
    try (NetcdfFile ncfileIn = ucar.nc2.dataset.NetcdfDatasets.openFile(datasetIn, cancel)) {
      NetcdfFormatWriter.Builder builder =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, datasetOut, null);
      NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder);

      try (NetcdfFile ncfileOut = copier.write(cancel)) {
        // empty
      } finally {
        cancel.setDone(true);
        System.out.printf("%s%n", cancel);
      }

    } catch (Exception ex) {
      System.out.printf("%s = %s %n", ex.getClass().getName(), ex.getMessage());
    }

    cancel.setDone(true);
    System.out.printf("%s%n", cancel);
  }

  // Demonstrates GitHub issue #296.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeStringMember() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeStringMember", ".nc4");

    try {
      NetcdfFormatWriter.Builder writerb =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, outFile.getAbsolutePath(), null);
      Structure.Builder<?> structb = writerb.addStructure("struct", "");
      structb.addMemberVariable("foo", DataType.STRING, "");

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure struct = (Structure) writer.findVariable("struct");
        assertNotNull(struct);

        // Write data
        ArrayString.D2 fooArray = new ArrayString.D2(1, 1);
        fooArray.set(0, 0, "bar");
        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        writer.write("struct", arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.getRootGroup().findVariableLocal("struct");
        assertNotNull(struct);
        Assert.assertEquals("bar", struct.readScalarString());
      }

    } finally {
      outFile.delete();
    }
  }

  // Demonstrates GitHub issue #298.
  // I did my best to write test code here that SHOULD work when NetcdfFileWriter and Nc4Iosp are fixed, using
  // TestStructureArrayW as a reference. However, failure happens very early in the test, so it's hard to know if
  // the unreached code is 100% correct. It may need to be fixed slightly.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeNestedStructure() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeNestedStructure", ".nc4");
    try {
      NetcdfFormatWriter.Builder writerb =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, outFile.getAbsolutePath(), null);
      Structure.Builder<?> outerb = writerb.addStructure("outer", "");
      Structure.Builder<?> innerb = Structure.builder().setName("inner").addMemberVariable("foo", DataType.INT, "");
      outerb.addMemberVariable(innerb);

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure outer = (Structure) writer.findVariable("outer");
        assertNotNull(outer);
        Structure inner = (Structure) outer.findVariable("inner");
        assertNotNull(inner);

        ArrayInt.D0 fooArray = new ArrayInt.D0(false);
        fooArray.set(42);

        StructureDataW innerSdw = new StructureDataW(inner.makeStructureMembers());
        innerSdw.setMemberData("foo", fooArray);
        ArrayStructureW innerAsw = new ArrayStructureW(innerSdw);

        StructureDataW outerSdw = new StructureDataW(outer.makeStructureMembers());
        outerSdw.setMemberData("inner", innerAsw);
        ArrayStructureW outerAsw = new ArrayStructureW(outerSdw);

        writer.write(outer, outerAsw);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.getRootGroup().findVariableLocal("outer");
        StructureData outerStructureData = struct.readStructure();
        StructureData innerStructureData = outerStructureData.getScalarStructure("inner");
        int foo = innerStructureData.getScalarInt("foo");

        Assert.assertEquals(42, foo);
      }

    } finally {
      outFile.delete();
    }
  }

  // Demonstrates GitHub issue #299.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeUnlimitedLengthStructure() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeUnlimitedLengthStructure", ".nc4");
    try {
      NetcdfFormatWriter.Builder writerb =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, outFile.getAbsolutePath(), null);
      writerb.addDimension(Dimension.builder("dim", 5).setIsUnlimited(true).build());
      writerb.addStructure("struct", "dim").addMemberVariable("foo", DataType.INT, null);

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure struct = (Structure) writer.findVariable("struct");
        assertNotNull(struct);

        // Write data
        ArrayInt.D2 fooArray = new ArrayInt.D2(5, 1, false);
        fooArray.set(0, 0, 2);
        fooArray.set(1, 0, 3);
        fooArray.set(2, 0, 5);
        fooArray.set(3, 0, 7);
        fooArray.set(4, 0, 11);

        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        writer.write(struct, arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Array fooArray = ncFileIn.readSection("struct.foo");

        int[] expecteds = new int[] {2, 3, 5, 7, 11};
        int[] actuals = (int[]) fooArray.copyToNDJavaArray();
        Assert.assertArrayEquals(expecteds, actuals);
      }

    } finally {
      outFile.delete();
    }
  }
}
