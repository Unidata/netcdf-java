/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.InvalidRangeException;
import ucar.array.StructureData;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test StructureIterator works when opened with IOSP_MESSAGE_ADD_RECORD_STRUCTURE. */
@Category(NeedsCdmUnitTest.class)
public class TestStructureIterator {

  @Test
  public void testStructureIterator() throws IOException, InvalidRangeException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "ft/station/Surface_METAR_20080205_0000.nc", -1,
        null, NetcdfFile.IOSP_MESSAGE_ADD_RECORD_STRUCTURE)) {

      Structure v = (Structure) ncfile.findVariable("record");
      assertThat(v).isNotNull();
      assertThat(v.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
      Array<StructureData> data = (Array<StructureData>) v.readArray();

      int count = 0;
      for (StructureData sd : data) {
        count++;
      }
      assertThat(count).isEqualTo(v.getSize());
    }
  }


}

