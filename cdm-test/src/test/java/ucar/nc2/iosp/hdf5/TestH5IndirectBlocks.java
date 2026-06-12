/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.iosp.hdf5;

import static com.google.common.truth.Truth.assertThat;
import static ucar.unidata.util.test.TestDir.cdmUnitTestDir;

import java.io.IOException;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

@Category(NeedsCdmUnitTest.class)
public class TestH5IndirectBlocks {

  private final String h5FileWithIndirectBlocks = cdmUnitTestDir + "formats/hdf5/bitplane_imaris/indirect_blocks.ims";

  // demonstrated https://github.com/Unidata/netcdf-java/issues/1546 using old api
  @Test
  public void testOpenFileWithIndirectBlocksOldApi() throws IOException {
    NetcdfFile ncf = NetcdfFile.open(h5FileWithIndirectBlocks);
    assertThat(ncf).isNotNull();
  }

  // demonstrated https://github.com/Unidata/netcdf-java/issues/1546 using new api
  @Test
  public void testOpenFileWithIndirectBlocksNewApi() throws IOException {
    NetcdfFile ncf = NetcdfFiles.open(h5FileWithIndirectBlocks);
    assertThat(ncf).isNotNull();
  }

  @Test
  public void testReadIndirectBlocksOldApi() throws IOException {
    NetcdfFile ncf = NetcdfFile.open(h5FileWithIndirectBlocks);
    assertThat(ncf).isNotNull();
    readVariableWithIndirectBlocks(ncf);
  }

  @Test
  public void testReadIndirectBlocksNewApi() throws IOException {
    NetcdfFile ncf = NetcdfFiles.open(h5FileWithIndirectBlocks);
    assertThat(ncf).isNotNull();
    readVariableWithIndirectBlocks(ncf);
  }

  private void readVariableWithIndirectBlocks(NetcdfFile ncf) throws IOException {
    Group groupWithIndirectBlocksVariables = ncf.findGroup("/DataSetInfo/ZeissAttrs");
    assertThat(groupWithIndirectBlocksVariables).isNotNull();
    List<Variable> variablesWithIndirectBlocks = groupWithIndirectBlocksVariables.getVariables();
    assertThat(variablesWithIndirectBlocks).isNotNull();
    assertThat(variablesWithIndirectBlocks.size()).isEqualTo(3);
    for (Variable variable : variablesWithIndirectBlocks) {
      String data = variable.readScalarString();
      assertThat(data).isNotNull();
      assertThat(data).isNotEmpty();
      assertThat(data).startsWith("<?xml");
      assertThat(data).contains("<ArrayOfRt");
    }
  }
}
