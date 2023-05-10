/*
 * Copyright (c) 1998-2023 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.gcdm;

import static com.google.common.truth.Truth.assertThat;
import static ucar.gcdm.TestUtils.*;
import static ucar.gcdm.TestUtils.gcdmPrefix;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import ucar.ma2.ArrayStructure;
import ucar.ma2.ArrayStructureW;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureDataW;
import ucar.ma2.StructureMembers;
import ucar.ma2.StructureMembers.MemberBuilder;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGcdmDataChunking {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testChunkProblem() throws Exception {
    String localFilename = TestDir.cdmUnitTestDir + "formats/netcdf4/multiDimscale.nc4";
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
