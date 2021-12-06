/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.Section;
import ucar.nc2.*;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Miscellaneaous problems with hdf5 reader
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5problem {

  @Test
  public void problem() throws IOException {
    String filename =
        TestH5.testDir + "npoess/ExampleFiles/AVAFO_NPP_d2003125_t10109_e101038_b9_c2005829155458_devl_Tst.h5";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
    }
  }

  // original file name is AG_100nt_even10k.biom
  // problem is that theres a deflate filter on array of strings
  // whats screwy about that is that the heapids get compressed, not the strings (!) doubt thats very useful.
  @Test
  public void problemStringsWithFilter() throws IOException {
    String filename = TestH5.testDir + "StringsWFilter.h5";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable("/sample/ids");
      assertThat(v).isNotNull();
      int[] shape = v.getShape();
      assertThat(1).isEqualTo(shape.length);
      assertThat(3107).isEqualTo(shape[0]);
      Array data = v.readArray();
      assertThat(1).isEqualTo(data.getRank());
      assertThat(3107).isEqualTo(data.getShape()[0]);
    }
  }

  @Test
  public void sectionStringsWithFilter() throws Exception {
    String filename = TestH5.testDir + "StringsWFilter.h5";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable("/sample/ids");
      assertThat(v).isNotNull();
      int[] shape = v.getShape();
      assertThat(1).isEqualTo(shape.length);
      assertThat(3107).isEqualTo(shape[0]);

      Array dataSection = v.readArray(new Section("700:900:2")); // make sure to go acrross a chunk boundary
      assertThat(1).isEqualTo(dataSection.getRank());
      assertThat(101).isEqualTo(dataSection.getShape()[0]);
    }
  }

  // The HugeHeapId problem: java.io.IOException: java.lang.RuntimeException: Cant find DHeapId=0
  // fixes by rschmunk 4/30/2015
  @Test
  public void problemHugeHeapId() throws IOException {
    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = TestH5.testDir + "SMAP_L4_SM_aup_20140115T030000_V05007_001.h5";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Group g = ncfile.findGroup("Metadata");
      assertThat(g).isNotNull();
      Attribute att = g.findAttribute("iso_19139_dataset_xml");
      assertThat(att).isNotNull();
      assertThat(att.isString()).isTrue();
      String val = att.getStringValue();
      assertThat(val).isNotNull();
      System.out.printf(" len of %s is %d%n", att.getName(), val.length());
      assertThat(val.length()).isGreaterThan(200 * 1000); // silly rabbit
    }
  }



}
