/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.InvalidRangeException;
import ucar.array.StructureData;
import ucar.array.StructureDataArray;
import ucar.array.StructureMembers;
import ucar.nc2.*;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test nc2 read JUnit framework.
 */
@Category(NeedsCdmUnitTest.class)
public class TestH5ReadStructure2 {

  /*
   * Structure {
   * int a_name;
   * String b_name(4);
   * char c_name(6);
   * short d_name(5, 4);
   * float e_name;
   * double f_name(10);
   * byte g_name;
   * } CompoundComplex(6);
   * type = Layout(8); type= 1 (contiguous) storageSize = (6,224) dataSize=0 dataAddress=2048
   */
  @Test
  public void testReadH5Structure() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[] {"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.", "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfFile ncfile = TestH5.openH5("complex/compound_complex.h5")) {

      Variable dset = ncfile.findVariable("CompoundComplex");
      assert (null != dset);
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 6);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 6);

      Structure s = (Structure) dset;

      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assert r.equals(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          Array data = sd.getMemberData(m);
        }
      }

    }
    System.out.println("*** testReadH5Structure ok");
  }

  @Test
  public void testH5StructureDS() throws java.io.IOException {
    int a_name = 0;
    String[] b_name = new String[] {"A fight is a contract that takes two people to honor.",
        "A combative stance means that you've accepted the contract.", "In which case, you deserve what you get.",
        "  --  Professor Cheng Man-ch'ing"};
    String c_name = "Hello!";

    // H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(TestH5.testDir + "complex/compound_complex.h5")) {

      Variable dset = ncfile.findVariable("CompoundComplex");
      assert (null != dset);
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 6);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 6);

      Structure s = (Structure) dset;

      // read all with the iterator
      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assert r.equals(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          Array data = sd.getMemberData(m);
        }
      }
    }
    System.out.println("*** testH5StructureDS ok");

  }

  /*
   * Structure {
   * int a_name;
   * byte b_name(3);
   * byte c_name(3);
   * short d_name(3);
   * int e_name(3);
   * long f_name(3);
   * int g_name(3);
   * short h_name(3);
   * int i_name(3);
   * long j_name(3);
   * float k_name(3);
   * double l_name(3);
   * } CompoundNative(15);
   * type = Layout(8); type= 1 (contiguous) storageSize = (15,144) dataSize=0 dataAddress=2048
   */
  @Test
  public void testReadH5StructureArrayMembers() throws java.io.IOException {
    try (NetcdfFile ncfile = TestH5.openH5("complex/compound_native.h5")) {

      Variable dset = ncfile.findVariable("CompoundNative");
      assert (null != dset);
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 15);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 15);

      Structure s = (Structure) dset;

      // read all with the iterator
      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      for (StructureData sd : iter) {
        for (StructureMembers.Member m : sd.getStructureMembers()) {
          Array data = sd.getMemberData(m);
        }
      }

    }
    System.out.println("*** testReadH5StructureArrayMembers ok");
  }

  /*
   * Structure {
   * int LAT[0];
   * ...
   * int LAT[149];
   * } IMAGE_LAT_ARRAY(3600);
   * type = Layout(8); type= 2 (chunked) storageSize = (1,600) dataSize=0 dataAddress=2548046
   */
  @Test
  public void testReadOneAtATime() throws Exception {
    try (NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5")) {

      Variable dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/IMAGE_LAT_ARRAY");
      assert (null != dset);
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 3600);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(3600);

      Structure s = (Structure) dset;
      StructureData sd = s.readRecord(3599);
      assertThat(sd.getMemberData("LAT[0]").getScalar()).isEqualTo(70862722);
      assertThat(sd.getMemberData("LAT[149]").getScalar()).isEqualTo(85302263);

      // read one at a time
      for (int i = 3590; i < d.getLength(); i++) {
        s.readRecord(i);
        System.out.println(" read structure " + i);
      }

    }
    System.out.println("*** testReadIASI ok");
  }

  /*
   * Structure {
   * char EntryName(64);
   * char Definition(1024);
   * char Unit(1024);
   * char Scale Factor(1024);
   * } TIME_DESCR(60);
   * type = Layout(8); type= 2 (chunked) storageSize = (1,3136) dataSize=0 dataAddress=684294
   */
  @Test
  public void testReadManyAtATime() throws Exception {
    try (NetcdfFile ncfile = TestH5.openH5("IASI/IASI.h5")) {

      Variable dset = ncfile.findVariable("U-MARF/EPS/IASI_xxx_1C/DATA/TIME_DESCR");
      assert (null != dset);
      assert (dset.getArrayType() == ArrayType.STRUCTURE);
      assert (dset.getRank() == 1);
      assert (dset.getSize() == 60);

      Dimension d = dset.getDimension(0);
      assert (d.getLength() == 60);

      StructureDataArray data = (StructureDataArray) dset.readArray();
      StructureMembers.Member m = data.getStructureMembers().findMember("EntryName");
      assert m != null;
      for (int i = 0; i < dset.getSize(); i++) {
        Array<Byte> carr = (Array<Byte>) data.get(i).getMemberData(m);
        String r = Arrays.makeStringFromChar(carr);
        if (i % 2 == 0)
          assert r.equals("TIME[" + i / 2 + "]-days") : r + " at " + i;
        else
          assert r.equals("TIME[" + i / 2 + "]-milliseconds") : r + " at " + i;
      }
    }
    System.out.println("*** testReadManyAtATime ok");
  }

}
