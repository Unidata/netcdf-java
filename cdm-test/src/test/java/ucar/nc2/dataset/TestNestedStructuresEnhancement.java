/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.nc2.Sequence;
import ucar.nc2.NetcdfFile;
import ucar.nc2.bufr.BufrArrayIosp;
import ucar.array.StructureData;
import ucar.array.InvalidRangeException;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test that nested structures get enhanced.
 */
public class TestNestedStructuresEnhancement {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTable() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openFile(filename, null)) {
      System.out.printf("open %s%n", ncfile.getLocation());
      Sequence outer = (Sequence) ncfile.findVariable(BufrArrayIosp.obsRecordName);
      assertThat((Object) outer).isNotNull();

      for (StructureData sdata : outer) {
        assertThat(sdata).isNotNull();
        assertThat(sdata.getMemberData("Latitude_coarse_accuracy").getScalar()).isEqualTo(32767);

        Array<?> as = sdata.getMemberData("Geopotential");
        assertThat(as).isNotNull();
        assertThat((Short) sdata.getMemberData("Wind_speed").getScalar()).isEqualTo(61);
      }
    }
  }

  @Ignore("cant deal with BUFR at the moment")
  @Test
  public void testNestedTableEnhanced() throws IOException, InvalidRangeException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      System.out.printf("open %s%n", ncfile.getLocation());
      SequenceDS outer = (SequenceDS) ncfile.findVariable(BufrArrayIosp.obsRecordName);
      assertThat((Object) outer).isNotNull();

      for (StructureData sdata : outer) {
        assertThat(sdata).isNotNull();
        assertThat((Double) sdata.getMemberData("Latitude_coarse_accuracy").getScalar()).isNaN();

        Array<?> as = sdata.getMemberData("Geopotential");
        assertThat(as).isNotNull();
        assertThat((Float) sdata.getMemberData("Wind_speed").getScalar()).isEqualTo(6.1);
      }
    }
  }
}
