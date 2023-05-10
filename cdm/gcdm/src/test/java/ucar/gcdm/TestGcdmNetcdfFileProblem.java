/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;
import static ucar.gcdm.TestUtils.compareFiles;
import static ucar.gcdm.TestUtils.compareVariables;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.gcdm.client.GcdmNetcdfFile;
import ucar.gcdm.server.GcdmServer;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.MemberBuilder;
import ucar.nc2.Sequence;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.nio.file.Paths;

/** Test {@link GcdmNetcdfFile} problems */
public class TestGcdmNetcdfFileProblem {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final String gcdmPrefix = "gcdm://localhost:16111/";

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  static public void before() {
    // make sure to fetch data through gcdm every time
    Variable.permitCaching = false;
  }

  @AfterClass
  static public void after() {
    Variable.permitCaching = true;
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    compareVariables(localFilename, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRequestNotTooBig() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    compareVariables(localFilename, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB_Samples_Used");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testHdf4() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf4/MOD021KM.A2004328.1735.004.2004329164007.hdf";
    compareVariables(localFilename, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
  }

  @Ignore("This variable is too large and causes an out of memory exception unrelated to gcdm.")
  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    compareVariables(localFilename, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCombineStructureMember() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    compareVariables(localFilename, "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001.GCcsRadAnalStd-value");
  }

  @Test
  public void testReadSection() throws Exception {
    String localFilename = "../../dap4/d4tests/src/test/data/resources/testfiles/test_atomic_array.nc";
    String varName = "vs"; // Variable is non-numeric so values are not read when header is
    String[] expectedValues = {"hello\tworld", "\r\n", "Καλημέα", "abc"};
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      Array array = gcdmVar.read();
      assertThat(array.getSize()).isEqualTo(expectedValues.length);
      assertThat(array.getObject(0)).isEqualTo(expectedValues[0]);
      assertThat(array.getObject(1)).isEqualTo(expectedValues[1]);
      assertThat(array.getObject(2)).isEqualTo(expectedValues[2]);
      assertThat(array.getObject(3)).isEqualTo(expectedValues[3]);

      Array subset1 = gcdmVar.read("0:0, 0:1");
      assertThat(subset1.getSize()).isEqualTo(2);
      assertThat(subset1.getObject(0)).isEqualTo(expectedValues[0]);
      assertThat(subset1.getObject(1)).isEqualTo(expectedValues[1]);

      Array subset2 = gcdmVar.read("1:1, 0:1");
      assertThat(subset2.getSize()).isEqualTo(2);
      assertThat(subset2.getObject(0)).isEqualTo(expectedValues[2]);
      assertThat(subset2.getObject(1)).isEqualTo(expectedValues[3]);

      Array subset3 = gcdmVar.read("0:1, 1:1");
      assertThat(subset3.getSize()).isEqualTo(2);
      assertThat(subset3.getObject(0)).isEqualTo(expectedValues[1]);
      assertThat(subset3.getObject(1)).isEqualTo(expectedValues[3]);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testReadSectionOfArrayStructure() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/hdf5/IASI/IASI.h5";
    String varName = "U-MARF/EPS/IASI_xxx_1C/DATA/MDR_1C_IASI_L1_ARRAY_000001";
    String gcdmUrl = gcdmPrefix + Paths.get(localFilename).toAbsolutePath();
    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable gcdmVar = gcdmFile.findVariable(varName);
      assertThat((Object) gcdmVar).isNotNull();

      assertThat(gcdmVar.read().getSize()).isEqualTo(720);
      assertThat(gcdmVar.read("0:130").getSize()).isEqualTo(131);
      assertThat(gcdmVar.read("718:719").getSize()).isEqualTo(2);
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmVlenCast() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/files/tst_opaque_data.nc4";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGcdmProblemNeeds() throws Exception {
    String localFilename =
        TestDir.cdmUnitTestDir + "formats/netcdf4/e562p1_fp.inst3_3d_asm_Nv.20100907_00z+20100909_1200z.nc4";
    compareVariables(localFilename, "O3");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testDataTooLarge() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testAttributeStruct() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/attributeStruct.nc";
    compareVariables(localFilename, "observations");
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testEnumProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_enums.nc";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testCharProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    compareFiles(localFilename);
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testChunkProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/multiDimscale.nc4";
    compareFiles(localFilename);
  }

  @Test
  public void testOpaqueDataType() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "hdf5/test_atomic_types.nc";
    compareFiles(localFilename);
  }

  @Test
  public void testGcdmProblem2() throws Exception {
    String localFilename = TestDir.cdmLocalTestDataDir + "dataset/SimpleGeos/hru_soil_moist_vlen_3hru_5timestep.nc";
    compareFiles(localFilename);
  }

  @Test
  public void testDataChunking() throws IOException, InvalidRangeException {
    String variableName = "varName";
    DataType dataType = DataType.INT;
    Array expectedData = Array.makeArray(dataType, 13_000_000, 0, 1);

    String filename = tempFolder.newFile().getAbsolutePath();
    writeNetcdfFile(filename, variableName, dataType, expectedData);
    String gcdmUrl = gcdmPrefix + filename;

    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable variable = gcdmFile.findVariable(variableName);
      assertThat((Object) variable).isNotNull();

      long size = variable.getElementSize() * variable.getSize();
      assertThat(size).isGreaterThan(GcdmServer.MAX_MESSAGE); // Data should be chunked

      Array data = variable.read();
      assertThat(data.getSize()).isEqualTo(expectedData.getSize());
      for (int i = 0; i < data.getSize(); i++) {
        assertThat(data.getInt(i)).isEqualTo(expectedData.getInt(i));
      }
    }
  }

  @Ignore("Can lead to an out of memory exception because structure member data is not chunked")
  @Test
  public void testDataChunkingForStructures() throws IOException, InvalidRangeException {
    String structureName = "structureName";
    String memberName = "memberName";
    DataType dataType = DataType.INT;
    Array expectedData = Array.makeArray(dataType, 13_000_000, 0, 1);

    String filename = tempFolder.newFile().getAbsolutePath();
    writeNetcdfFileWithStructure(filename, structureName, memberName, dataType, expectedData);
    String gcdmUrl = gcdmPrefix + filename;

    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Variable variable = gcdmFile.findVariable(structureName);
      assertThat((Object) variable).isNotNull();

      long size = variable.getElementSize() * variable.getSize();
      assertThat(size).isGreaterThan(GcdmServer.MAX_MESSAGE); // Data should be chunked

      ArrayStructure arrayStructure = (ArrayStructure) variable.read();
      Array memberData = arrayStructure.extractMemberArray(arrayStructure.findMember(memberName));
      assertThat(memberData.getSize()).isEqualTo(expectedData.getSize());
      for (int i = 0; i < memberData.getSize(); i++) {
        assertThat(memberData.getInt(i)).isEqualTo(expectedData.getInt(i));
      }
    }
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testSequenceData() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/bufr/userExamples/test1.bufr";
    String gcdmUrl = gcdmPrefix + filename;

    try (GcdmNetcdfFile gcdmFile = GcdmNetcdfFile.builder().setRemoteURI(gcdmUrl).build()) {
      Sequence sequence = (Sequence) gcdmFile.findVariable("obs");
      assertThat((Object) sequence).isNotNull();
      assertThat(sequence.getNumberOfMemberVariables()).isEqualTo(78);

      Variable variable = sequence.findVariable("Station_or_site_name");
      assertThat((Object) variable).isNotNull();
      assertThat(variable.getSize()).isEqualTo(20);
      assertThat(variable.getDataType()).isEqualTo(DataType.CHAR);

      ArrayChar data = (ArrayChar) variable.read();
      assertThat(data.getString(0)).isEqualTo("WOLLOGORANG        ");
    }
  }

  private static void writeNetcdfFile(String filename, String variableName, DataType dataType, Array values)
      throws IOException, InvalidRangeException {
    NetcdfFormatWriter.Builder writerBuilder = NetcdfFormatWriter.createNewNetcdf3(filename);

    writerBuilder.addDimension("dimension", (int) values.getSize());
    writerBuilder.addVariable(variableName, dataType, "dimension");

    try (NetcdfFormatWriter writer = writerBuilder.build()) {
      writer.write(variableName, values);
    }
  }

  private static void writeNetcdfFileWithStructure(String filename, String structureName, String memberName,
      DataType dataType, Array values) throws IOException, InvalidRangeException {
    String structureDimensionName = "structureDimension";
    String memberDimensionName = "memberDimension";
    NetcdfFormatWriter.Builder writerBuilder =
        NetcdfFormatWriter.createNewNetcdf4(NetcdfFileFormat.NETCDF4, filename, null);

    writerBuilder.addDimension(structureDimensionName, 1);
    writerBuilder.addDimension(memberDimensionName, (int) values.getSize());

    Structure.Builder<?> structureBuilder = writerBuilder.addStructure(structureName, structureDimensionName);
    Variable.Builder<?> variableBuilder =
        Variable.builder().setParentGroupBuilder(structureBuilder.getParentGroupBuilder())
            .setDimensionsByName(memberDimensionName).setName(memberName).setDataType(dataType);
    structureBuilder.addMemberVariable(variableBuilder);

    try (NetcdfFormatWriter writer = writerBuilder.build()) {
      Structure structure = (Structure) writer.findVariable(structureName);
      assertThat((Object) structure).isNotNull();

      int[] shape = new int[] {(int) values.getSize()};
      MemberBuilder memberBuilder = StructureMembers.builder().addMember(memberName, "desc", "units", dataType, shape);
      StructureMembers members = StructureMembers.builder().addMember(memberBuilder).build();
      StructureDataW structureData = new StructureDataW(members);
      structureData.setMemberData(memberName, values);
      ArrayStructureW arrayStructure = new ArrayStructureW(structureData);

      writer.write(structureName, arrayStructure);
    }
  }
}
