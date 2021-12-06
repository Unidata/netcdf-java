/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.internal.iosp.hdf5;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.grid.Grid;
import ucar.nc2.grid.GridDataset;
import ucar.nc2.grid.GridDatasetFactory;
import ucar.nc2.grid.GridReferencedArray;
import ucar.nc2.util.DebugFlags;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

/** Miscellaneous test on HDF5 iosp */
@Category(NeedsCdmUnitTest.class)
public class TestN4problems {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @AfterClass
  static public void after() {
    H5header.setDebugFlags(DebugFlags.create("")); // make sure debug flags are off
  }

  @Test
  public void testTilingNetcdfFile() throws Exception {
    // java.lang.AssertionError: shape[2] (385) >= pt[2] (390)
    String filename = TestN4reading.testDir + "UpperDeschutes_t4p10_swemelt.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.getRootGroup().findVariableLocal("UpperDeschutes_t4p10_swemelt");
      Array<?> data = v.readArray(new Section("8087, 150:155, 150:155"));
      assertThat(data).isNotNull();
    }
  }

  // margolis@ucar.edu
  // I really don't think this is a problem with your code
  // may be bug in HDF5 1.8.4-patch1
  @Test
  public void testTilingGridDataset() throws Exception {
    // Global Heap 1t 13059 runs out with no heap id = 0
    String filename = TestN4reading.testDir + "tiling.nc4";
    String vname = "Turbulence_SIGMET_AIRMET";
    Formatter errlog = new Formatter();
    try (GridDataset gds = GridDatasetFactory.openGridDataset(filename, errlog)) {
      Grid grid = gds.findGrid(vname).orElseThrow();
      System.out.printf("grid=%s%n", grid);
      GridReferencedArray data = grid.getReader().read();
      assertThat(data.data()).isNotNull();
    }
  }


  @Test
  @Ignore("user types not correct")
  public void utestEnum() throws IOException {
    H5header.setDebugFlags(DebugFlags.create("H5header/header"));
    String filename = TestN4reading.testDir + "nc4/tst_enum_data.nc";
    try (NetcdfFile ncfile = NetcdfFiles.open(filename)) {
      Variable v = ncfile.findVariable("primary_cloud");
      assertThat(v).isNotNull();

      Array<?> data = v.readArray();
      System.out.println("\n**** testReadNetcdf4 done\n\n" + ncfile);
      logger.debug(NcdumpArray.printArray(data, "primary_cloud", null));
    }
    H5header.setDebugFlags(DebugFlags.create(""));
  }

  @Test
  @Ignore("user types not correct")
  public void utestEnum2() throws IOException {
    try (NetcdfFile ncfile = NetcdfDatasets.openFile("D:/netcdf4/tst_enum_data.nc", null)) {
      Variable v = ncfile.findVariable("primary_cloud");
      assertThat(v).isNotNull();

      Array<?> data = v.readArray();
      assertThat(data.getArrayType()).isEqualTo(ArrayType.BYTE);
    }

    try (NetcdfDataset ncd = NetcdfDatasets.openDataset("D:/netcdf4/tst_enum_data.nc")) {
      Variable v2 = ncd.findVariable("primary_cloud");
      assertThat(v2).isNotNull();

      Array<?> data2 = v2.readArray();
      assertThat(data2.getArrayType()).isEqualTo(ArrayType.STRING);
    }
  }

}
