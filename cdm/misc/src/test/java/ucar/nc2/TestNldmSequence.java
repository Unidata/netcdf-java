/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArraySequence;
import ucar.ma2.ArrayStructure;
import ucar.ma2.MAMath;
import ucar.ma2.StructureData;
import ucar.ma2.StructureDataIterator;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Test Sequences constructed when reading NLDN datasets. */
public class TestNldmSequence {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testRead() throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(TestDir.cdmUnitTestDir + "ft/point/200929100.ingest")) {
      Sequence record = (Sequence) ncfile.findVariable("record");

      List<String> expectedMemberNames = Arrays.asList("tsec", "nsec", "lat", "lon", "sgnl", "mult", "fill",
          "majorAxis", "eccent", "ellipseAngle", "chisqr");
      Assert.assertEquals(Sets.newHashSet(expectedMemberNames), Sets.newHashSet(record.getVariableNames()));

      try (StructureDataIterator iter = record.getStructureIterator()) {
        int recordCount = 0;
        while (iter.hasNext()) {
          StructureData data = iter.next();

          // Assert that a single value from the first record equals an expected value.
          // Kinda lazy, but checking all values would be impractical.
          if (recordCount++ == 0) {
            Assert.assertEquals(-700, data.getScalarShort("sgnl"));
          }
        }

        Assert.assertEquals(1165, recordCount);
      }
    }
  }
}
