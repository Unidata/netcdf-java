/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grib.iosp;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;

/** Test Grib Coordinates. */
@Category(NeedsCdmUnitTest.class)
public class TestGribMiscCoordinates {

  // 4.2 used to add the vert coord transform, for GRIB 1 when the getVerticalPressureLevels() was set.
  // But how do we associate it with a surface pressure variable ???
  /*
   * Q:/cdmUnitTest/formats/grib1/HIRLAMhybrid.grib
   * Level Type : (109) hybrid
   * verticalPressureLevels (2) = 1003.0288 0.0000
   * has hybrid levels 1-40
   * maybe incorrect parameter name (taken from WMO) for 99 (center 99/0) .
   * looks better with level > 3
   */
  @Test
  public void testHybrid1() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/HIRLAMhybrid.grib";
    System.out.println("\n\nReading File " + filename);
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    Variable hybrid = ncfile.findVariable("hybrid");
    assertThat(hybrid).isNotNull();
    assertThat(hybrid.getNameAndDimensions()).isEqualTo("hybrid(hybrid=40)");

    int idx = hybrid.findDimensionIndex("hybrid");
    Dimension dim = hybrid.getDimension(idx);
    assertThat(dim.getShortName()).isEqualTo("hybrid");

    ncfile.close();
  }

  @Test
  public void testHybridCoordinates() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/07010418_arw_d01.GrbF01500";
    System.out.println("\n\nReading File " + filename);
    NetcdfFile ncfile = NetcdfFiles.open(filename);
    Variable hybrid = ncfile.findVariable("hybrid1");
    assertThat(hybrid).isNotNull();
    assertThat(hybrid.getDimensions()).hasSize(1);
    Dimension d = hybrid.getDimension(0);
    assertThat(d.getLength()).isEqualTo(2);

    ncfile.close();
  }

  @Test
  public void testGaussianLats() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "formats/grib1/CCCma_SRES_A2_HGT500_1-10.grb";
    System.out.println("\n\nReading File " + filename);

    NetcdfFile ncfile = NetcdfFiles.open(filename);
    Variable lat = ncfile.findVariable("lat");
    assertThat(lat).isNotNull();
    assertThat(lat.getSize()).isEqualTo(48);
    ncfile.close();
  }
}
