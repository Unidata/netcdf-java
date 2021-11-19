/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.dataset;

import org.junit.Ignore;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.StructureDataArray;
import ucar.nc2.Sequence;
import ucar.nc2.NetcdfFile;
import ucar.nc2.bufr.BufrIosp;
import ucar.array.StructureData;
import ucar.unidata.util.test.TestDir;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test that nested structures get enhanced.
 */
public class TestNestedStructuresEnhancement {

  // TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr"; error on reading data

  @Test
  public void test1() throws IOException {
    String filename = TestDir.bufrLocalTestDataDir + "test1.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openFile(filename, null)) {
      testTable(ncfile, new MemberValue("Latitude_high_accuracy", 7278778),
          new MemberValue("Temperature-dry-bulb_temperature", 30116), new MemberValue("Elevation", 32767));

      testTableNested(ncfile, "struct3", new MemberValue("Vertical_significance_surface_observations-4", 7),
          new MemberValue("True_direction_from_which_a_phenomenon_or_clouds_are_moving", 511));
    }

    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      testTable(ncfile, new MemberValue("Latitude_high_accuracy", -17.212221f),
          new MemberValue("Temperature-dry-bulb_temperature", 301.16f), new MemberValue("Elevation", Float.NaN));

      testTableNested(ncfile, "struct3", new MemberValue("Vertical_significance_surface_observations-4", "Low cloud"),
          new MemberValue("True_direction_from_which_a_phenomenon_or_clouds_are_moving", 511));
    }
  }

  @Test
  public void testEmbedded() throws IOException {
    String filename = TestDir.bufrLocalTestDataDir + "embedded.bufr";
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openFile(filename, null)) {
      testTable(ncfile, new MemberValue("SELV", 3235), new MemberValue("CLON", 18000), new MemberValue("WMOS", 9));

      testTableNested(ncfile, "UASDG_RADIOSONDE_SOUNDING_SYSTEM_DATA", new MemberValue("QMST", 15),
          new MemberValue("UALNMN", 10));
    }

    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openDataset(filename)) {
      testTable(ncfile, new MemberValue("SELV", 2835.0f), new MemberValue("CLON", -4.0233135E-6f),
          new MemberValue("WMOS", 9));

      testTableNested(ncfile, "UASDG_RADIOSONDE_SOUNDING_SYSTEM_DATA", new MemberValue("QMST", 15),
          new MemberValue("UALNMN", 10));
    }
  }

  @Test
  @Ignore("error on reading data")
  public void testNested() throws IOException {
    String filename = TestDir.cdmLocalTestDataDir + "dataset/nestedTable.bufr"; // error on reading data
    try (NetcdfFile ncfile = ucar.nc2.dataset.NetcdfDatasets.openFile(filename, null)) {
      testTable(ncfile, new MemberValue("Latitude_coarse_accuracy", 32767), new MemberValue("Geopotential", 32767),
          new MemberValue("Wind_speed", 61));
    }
  }

  private class MemberValue {
    final String name;
    final Object val;

    public MemberValue(String name, Object val) {
      this.name = name;
      this.val = val;
    }
  }

  private void testTable(NetcdfFile ncfile, MemberValue... memvals) {
    System.out.printf("testTable %s%n", ncfile.getLocation());
    Sequence outer = (Sequence) ncfile.findVariable(BufrIosp.obsRecordName);
    assertThat((Object) outer).isNotNull();

    for (StructureData sdata : outer) {
      assertThat(sdata).isNotNull();
      for (MemberValue memval : memvals) {
        assertThat(sdata.getMemberData(memval.name).getScalar()).isEqualTo(memval.val);
      }
      break;
    }
  }

  private void testTableNested(NetcdfFile ncfile, String nestedMember, MemberValue... memvals) {
    System.out.printf("testTableNested %s%n", ncfile.getLocation());
    Sequence outer = (Sequence) ncfile.findVariable(BufrIosp.obsRecordName);
    assertThat((Object) outer).isNotNull();

    for (StructureData sdata : outer) {
      assertThat(sdata).isNotNull();

      Array<?> ndata = sdata.getMemberData(nestedMember);
      assertThat(ndata).isNotNull();
      assertThat(ndata).isInstanceOf(StructureDataArray.class);
      StructureDataArray nestedStruct = (StructureDataArray) ndata;

      for (StructureData nested : nestedStruct) {
        assertThat(nested).isNotNull();
        for (MemberValue memval : memvals) {
          assertThat(nested.getMemberData(memval.name).getScalar()).isEqualTo(memval.val);
        }
        break;
      }
      break;
    }
  }
}
