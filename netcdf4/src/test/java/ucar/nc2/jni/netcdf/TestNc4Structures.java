/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.jni.netcdf;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Index;
import ucar.array.InvalidRangeException;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureDataStorageBB;
import ucar.array.StructureMembers;
import ucar.nc2.*;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.util.CancelTask;
import ucar.nc2.write.NetcdfCopier;
import ucar.nc2.iosp.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/** Test writing structure data into netcdf4. */
@Category(NeedsCdmUnitTest.class)
public class TestNc4Structures {
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

      try (NetcdfCopier copier = NetcdfCopier.create(ncfileIn, builder)) {
        copier.write(cancel);
      }

    } catch (Exception ex) {
      System.out.printf("%s = %s %n", ex.getClass().getName(), ex.getMessage());
    }

    cancel.cancel();
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
      structb.addMemberVariable("foo", ArrayType.STRING, "");

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure struct = (Structure) writer.findVariable("struct");
        assertThat(struct).isNotNull();

        StructureMembers members = struct.makeStructureMembersBuilder().build();
        ByteBuffer bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
        StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, 1);

        // Write data
        storage.putOnHeap("bar");
        writer.write(struct, Index.ofRank(0), new StructureDataArray(members, new int[] {1}, storage));
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.getRootGroup().findVariableLocal("struct");
        assertNotNull(struct);
        assertThat(struct.readScalarString()).isEqualTo("bar");
      }

    } finally {
      outFile.delete();
    }
  }

  // Demonstrates GitHub issue #298.
  // I did my best to write test code here that SHOULD work when NetcdfFileWriter and Nc4Iosp are fixed.
  // However, failure happens very early in the test, so it's hard to know if
  // the unreached code is 100% correct. It may need to be fixed slightly.
  @Ignore("Resolve issue before we enable this.")
  @Test
  public void writeNestedStructure() throws IOException, InvalidRangeException {
    File outFile = File.createTempFile("writeNestedStructure", ".nc4");
    try {
      NetcdfFormatWriter.Builder writerb =
          NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, outFile.getAbsolutePath(), null);
      Structure.Builder<?> outerb = writerb.addStructure("outer", "");
      Structure.Builder<?> innerb = Structure.builder().setName("inner").addMemberVariable("foo", ArrayType.INT, "");
      outerb.addMemberVariable(innerb);

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure outer = (Structure) writer.findVariable("outer");
        assertThat(outer).isNotNull();
        Structure inner = (Structure) outer.findVariable("inner");
        assertThat(inner).isNotNull();

        StructureMembers members = outer.makeStructureMembersBuilder().build();
        ByteBuffer bbuffer = ByteBuffer.allocate(members.getStorageSizeBytes());
        StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, 1);

        // Write data
        bbuffer.putInt(42);
        writer.write(outer, Index.ofRank(0), new StructureDataArray(members, new int[] {1}, storage));
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.getRootGroup().findVariableLocal("outer");
        StructureDataArray outerArray = (StructureDataArray) struct.readArray();
        StructureMembers members = outerArray.getStructureMembers();
        StructureMembers.Member innerMember = members.findMember("inner");

        StructureData outerStructureData = outerArray.get(0);
        StructureDataArray innerArray = (StructureDataArray) outerStructureData.getMemberData(innerMember);
        StructureMembers innerMembers = innerArray.getStructureMembers();
        StructureMembers.Member innerFoo = innerMembers.findMember("foo");

        StructureData innerStructureData = innerArray.get(0);
        Array<?> fooData = innerStructureData.getMemberData(innerFoo);
        int foo = (Integer) fooData.getScalar();
        assertThat(foo).isEqualTo(42);
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
      writerb.addStructure("struct", "dim").addMemberVariable("foo", ArrayType.INT, null);

      try (NetcdfFormatWriter writer = writerb.build()) {
        Structure struct = (Structure) writer.findVariable("struct");
        assertNotNull(struct);

        int nrecords = 5;
        StructureMembers members = struct.makeStructureMembersBuilder().build();
        ByteBuffer bbuffer = ByteBuffer.allocate(nrecords * members.getStorageSizeBytes());
        StructureDataStorageBB storage = new StructureDataStorageBB(members, bbuffer, nrecords);

        bbuffer.putInt(2);
        bbuffer.putInt(3);
        bbuffer.putInt(5);
        bbuffer.putInt(7);
        bbuffer.putInt(11);

        writer.write(struct, Index.ofRank(0), new StructureDataArray(members, new int[] {nrecords}, storage));
      }

      // Read the file back in and make sure that what we wrote is what we're getting back.
      try (NetcdfFile ncFileIn = NetcdfFiles.open(outFile.getAbsolutePath())) {
        Structure struct = (Structure) ncFileIn.findVariable("struct");
        assertThat(struct).isNotNull();
        StructureDataArray structArray = (StructureDataArray) struct.readArray();
        StructureMembers members = structArray.getStructureMembers();
        StructureMembers.Member innerMember = members.findMember("foo");
        assertThat(innerMember).isNotNull();

        Array<Integer> fooData = (Array<Integer>) structArray.extractMemberArray(innerMember);
        int[] expecteds = new int[] {2, 3, 5, 7, 11};
        int count = 0;
        for (int val : fooData) {
          assertThat(val).isEqualTo(expecteds[count++]);
        }
      }

    } finally {
      outFile.delete();
    }
  }
}
