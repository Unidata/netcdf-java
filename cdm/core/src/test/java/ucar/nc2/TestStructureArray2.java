/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.internal.dataset.StructurePseudoDS;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.UtilsTestStructureArray;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test reading record data */

public class TestStructureArray2 {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  private UtilsTestStructureArray test = new UtilsTestStructureArray();

  @Test
  public void testBB() throws IOException, InvalidRangeException {
    // testWriteRecord is 1 dimensional (nc2 record dimension)
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmLocalTestDataDir + "testWriteRecord.nc", -1, null,
        NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      Structure v = (Structure) ncfile.findVariable("record");
      assert v != null;

      assert (v.getDataType() == DataType.STRUCTURE);

      Array data = v.read();
      assert (data instanceof ArrayStructure);
      assert (data instanceof ArrayStructureBB);
      assert (data.getElementType() == StructureData.class);

      test.testArrayStructure((ArrayStructure) data);
    }
  }

  @Test
  public void testMA() throws IOException, InvalidRangeException {
    // jan.nc is 1 dimensional (nc2 record dimension)
    try (NetcdfFile ncfile = TestDir.openFileLocal("jan.nc");
        NetcdfDataset ncd = NetcdfDatasets.enhance(ncfile, NetcdfDataset.getDefaultEnhanceMode(), null)) {
      Dimension dim = ncd.findDimension("time");
      assert dim != null;

      Structure p = StructurePseudoDS.fromVars(ncd.getRootGroup(), "Psuedo", null, dim);

      assert (p.getDataType() == DataType.STRUCTURE);

      Array data = p.read();
      assert (data instanceof ArrayStructure);
      assert (data instanceof ArrayStructureMA);
      assert (data.getElementType() == StructureData.class);

      test.testArrayStructure((ArrayStructure) data);
    }
  }

}
