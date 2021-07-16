/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

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

  @Test
  public void appendStructureData() throws IOException, InvalidRangeException {
    String ncFileOut = tempFolder.newFile().getAbsolutePath();
    NetcdfFormatWriter.Builder wb = NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, ncFileOut, null);

    Group.Builder gr_root = wb.getRootGroup();

    // create dimension
    Dimension dim = new Dimension("dim", 3);
    gr_root.addDimension(dim);

    // create structure
    Structure.Builder<?> sb = Structure.builder();
    sb.setName("struct");
    sb.addDimension(dim);
    sb.setParentGroupBuilder(gr_root);

    // add a member
    Variable.Builder<?> var = Variable.builder();
    var.setDataType(DataType.INT);
    var.setName("var");
    var.addDimension(dim);
    sb.addMemberVariable(var);

    // build
    wb.getRootGroup().addVariable(sb);
    NetcdfFormatWriter writer = wb.build();

    // add member data
    Structure s = (Structure) writer.findVariable("/struct");
    StructureMembers sm = s.makeStructureMembers();
    StructureDataW sw = new StructureDataW(sm);
    int[] data = new int[] {1, 2, 3};
    Array dataArray = Array.factory(DataType.INT, new int[] {3}, data);
    sw.setMemberData(sm.findMember(var.shortName), dataArray);

    // write to file
    writer.appendStructureData(s, sw);
    writer.close();

    // read and verify
    NetcdfFile ncfile = NetcdfFiles.open(ncFileOut);
    Structure struct = (Structure) ncfile.findVariable("/struct");
    Array out = struct.readStructure(0).getArray(var.shortName);
    Assert.assertArrayEquals(data, (int[]) out.get1DJavaArray(DataType.INT));
    ncfile.close();
  }

  // Demonstrates GitHub issue #296.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeStringMember() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeStringMember", ".nc4");
    try {
      try (NetcdfFileWriter ncFileWriter =
          NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileWriter.addVariable(null, "struct", DataType.STRUCTURE, "");
        ncFileWriter.addStructureMember(struct, "foo", DataType.STRING, null);

        ncFileWriter.create();

        // Write data
        ArrayString.D2 fooArray = new ArrayString.D2(1, 1);
        fooArray.set(0, 0, "bar");

        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        ncFileWriter.write(struct, arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.getRootGroup().findVariableLocal("struct");
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
      try (NetcdfFileWriter ncFileWriter =
          NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        Structure outer = (Structure) ncFileWriter.addVariable(null, "outer", DataType.STRUCTURE, "");
        Structure inner = (Structure) ncFileWriter.addStructureMember(outer, "inner", DataType.STRUCTURE, "");
        ncFileWriter.addStructureMember(inner, "foo", DataType.INT, null);

        ncFileWriter.create();

        // Write data
        ArrayInt.D0 fooArray = new ArrayInt.D0(false);
        fooArray.set(42);

        StructureDataW innerSdw = new StructureDataW(inner.makeStructureMembers());
        innerSdw.setMemberData("foo", fooArray);
        ArrayStructureW innerAsw = new ArrayStructureW(innerSdw);

        StructureDataW outerSdw = new StructureDataW(outer.makeStructureMembers());
        outerSdw.setMemberData("inner", innerAsw);
        ArrayStructureW outerAsw = new ArrayStructureW(outerSdw);

        ncFileWriter.write(outer, outerAsw);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
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
      try (NetcdfFileWriter ncFileWriter =
          NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, outFile.getAbsolutePath())) {
        // Test passes if we do "isUnlimited == false".
        Dimension dim = ncFileWriter.addDimension(null, "dim", 5, true /* false */, false);

        Structure struct = (Structure) ncFileWriter.addVariable(null, "struct", DataType.STRUCTURE, "dim");
        ncFileWriter.addStructureMember(struct, "foo", DataType.INT, null);

        ncFileWriter.create();

        // Write data
        ArrayInt.D2 fooArray = new ArrayInt.D2(5, 1, false);
        fooArray.set(0, 0, 2);
        fooArray.set(1, 0, 3);
        fooArray.set(2, 0, 5);
        fooArray.set(3, 0, 7);
        fooArray.set(4, 0, 11);

        ArrayStructureMA arrayStruct = new ArrayStructureMA(struct.makeStructureMembers(), struct.getShape());
        arrayStruct.setMemberArray("foo", fooArray);

        ncFileWriter.write(struct, arrayStruct);
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFile.open(outFile.getAbsolutePath())) {
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
