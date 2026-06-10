/*
 * Copyright (c) 2026 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.dataset;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;

import org.junit.Test;
import ucar.ma2.DataType;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;

public class TestCoordinateAxisDataType {

  String testFile = TestDir.cdmLocalTestDataDir + "/ncml/coords/string_coord.ncml";

  @Test
  public void testDataTypeChangeOldApi() throws IOException {
    try (NetcdfDataset ncdOld = NetcdfDataset.openDataset(testFile)) {
      checkDataType(ncdOld);
    }
  }

  @Test
  public void testDataTypeChangeNewApi() throws IOException {
    String testFile = TestDir.cdmLocalTestDataDir + "/ncml/coords/string_coord.ncml";
    try (NetcdfDataset ncdNew = NetcdfDatasets.openDataset(testFile)) {
      checkDataType(ncdNew);
    }
  }

  private void checkDataType(NetcdfDataset ncd) {
    Variable var = ncd.findVariable("var");
    assertThat(var != null).isTrue();
    CoordinateAxis1D axis = (CoordinateAxis1D) var;
    assertThat(axis.getOriginalDataType()).isEqualTo(DataType.INT);
    assertThat(axis.getDataType()).isEqualTo(DataType.STRING);
    assertThat(axis.isNumeric()).isFalse();
  }

}
