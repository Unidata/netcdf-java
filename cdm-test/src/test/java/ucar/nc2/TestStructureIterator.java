/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/** Test StructureIterator works when opened with IOSP_MESSAGE_ADD_RECORD_STRUCTURE. */
@Category(NeedsCdmUnitTest.class)
public class TestStructureIterator {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void testStructureIteratorOld() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFile.open(TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc")) {
      ncfile.sendIospMessage(NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE);

      Structure v = (Structure) ncfile.findVariable("record");
      assert v != null;
      assert (v.getDataType() == DataType.STRUCTURE);

      int count = 0;
      try (StructureDataIterator si = v.getStructureIterator()) {
        while (si.hasNext()) {
          StructureData sd = si.next();
          count++;
        }
      }
      assert count == v.getSize();
    }
  }

  @Test
  public void testStructureIterator() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc", -1,
        null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      Structure v = (Structure) ncfile.findVariable("record");
      assert v != null;
      assert (v.getDataType() == DataType.STRUCTURE);

      int count = 0;
      try (StructureDataIterator si = v.getStructureIterator()) {
        while (si.hasNext()) {
          StructureData sd = si.next();
          count++;
        }
      }
      assert count == v.getSize();
    }
  }


}

