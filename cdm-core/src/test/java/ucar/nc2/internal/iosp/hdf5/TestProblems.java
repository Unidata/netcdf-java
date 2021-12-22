/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.internal.iosp.hdf5;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.array.ArrayType;
import ucar.nc2.EnumTypedef;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import static com.google.common.truth.Truth.assertThat;

/** Test problems in hdf5 / netcdf 4 files. */
@Category(NeedsCdmUnitTest.class)
public class TestProblems {

  @Test
  public void problem() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/netcdf4/attributeStruct.nc";
    System.out.printf("TestProblems %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      System.out.printf("result = %s%n", ncfile);
    }
  }

  @Test
  public void problemAnon() throws Exception {
    String filename = "/home/snake/Downloads/GC1SG1_202105251107C25504_L2SG_SSTDK_2000.h5";
    System.out.printf("problemAnon %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      System.out.printf("result = %s%n", ncfile);
    }
  }
}
