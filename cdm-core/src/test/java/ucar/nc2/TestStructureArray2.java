/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.StructureDataArray;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UtilsTestStructureArray;
import java.io.IOException;

/** Test reading record data */

public class TestStructureArray2 {
  private UtilsTestStructureArray test = new UtilsTestStructureArray();

  @Test
  public void testBB() throws IOException, InvalidRangeException {
    // testWriteRecord is 1 dimensional (nc2 record dimension)
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      Structure v = (Structure) ncfile.findVariable("record");
      assert v != null;

      assert (v.getArrayType() == ArrayType.STRUCTURE);

      Array<?> data = v.readArray();
      assert (data.getArrayType() == ArrayType.STRUCTURE);

      test.testArrayStructure((StructureDataArray) data);
    }
  }

}
