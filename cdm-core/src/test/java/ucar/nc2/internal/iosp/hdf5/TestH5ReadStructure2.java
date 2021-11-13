/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.StructureData;
import ucar.array.StructureMembers;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Structure;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.unidata.util.test.TestDir;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test hdf5 structures.
 */
public class TestH5ReadStructure2 {

  String filename = TestDir.cdmLocalTestDataDir + "hdf5/compound_complex.h5";

  String[] b_name = new String[] {"A fight is a contract that takes two people to honor.",
      "A combative stance means that you've accepted the contract.", "In which case, you deserve what you get.",
      "  --  Professor Cheng Man-ch'ing"};
  String c_name = "Hello!";

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
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable dset = ncfile.findVariable("CompoundComplex");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
      assertThat(dset.getRank()).isEqualTo(1);
      assertThat(dset.getSize()).isEqualTo(6);

      Dimension d = dset.getDimension(0);
      assertThat(d.getLength()).isEqualTo(6);

      Structure s = (Structure) dset;

      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      int a_name = 0;
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assertThat(r).isEqualTo(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          sd.getMemberData(m);
        }
      }

    }
    System.out.println("*** testReadH5Structure ok");
  }

  @Test
  public void testH5StructureDS() throws java.io.IOException {
    try (NetcdfDataset ncfile = NetcdfDatasets.openDataset(filename)) {

      Variable dset = ncfile.findVariable("CompoundComplex");
      assertThat(dset).isNotNull();
      assertThat(dset.getArrayType()).isEqualTo(ArrayType.STRUCTURE);
      assertThat(dset.getRank()).isEqualTo(1);
      assertThat(dset.getSize()).isEqualTo(6);

      Structure s = (Structure) dset;

      // read all with the iterator
      Array<StructureData> iter = (Array<StructureData>) s.readArray();
      int a_name = 0;
      for (StructureData sd : iter) {
        assertThat((Integer) sd.getMemberData("a_name").getScalar()).isEqualTo(a_name);
        a_name++;
        Array<Byte> carr = (Array<Byte>) sd.getMemberData("c_name");
        assertThat(Arrays.makeStringFromChar(carr)).isEqualTo(c_name);
        Array<String> results = (Array<String>) sd.getMemberData("b_name");
        assertThat(results.length()).isEqualTo(b_name.length);
        int count = 0;
        for (String r : results) {
          assertThat(r).isEqualTo(b_name[count++]);
        }

        for (StructureMembers.Member m : sd.getStructureMembers()) {
          sd.getMemberData(m);
        }
      }
    }
    System.out.println("*** testH5StructureDS ok");
  }

}
