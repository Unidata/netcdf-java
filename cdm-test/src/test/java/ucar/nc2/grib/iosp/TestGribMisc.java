/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.nc2.grib.iosp;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.array.Array;
import ucar.array.Arrays;
import ucar.array.Section;
import ucar.nc2.*;
import ucar.nc2.grib.collection.Grib;
import ucar.nc2.grib.grib1.Grib1RecordScanner;
import ucar.nc2.util.Misc;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test misc GRIB features
 */
@Category(NeedsCdmUnitTest.class)
public class TestGribMisc {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  public void pdsScaleOverflow() throws Exception {
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/pdsScale.grib2";
    logger.debug("{}", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.findVariable("isobaric");
      float val = (Float) v.readArray().getScalar();
      assertThat(Misc.nearlyEquals(val, 92500.0)).isTrue();
    }
  }

  @Test
  public void pdsGenType() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/08Aug08.12z.cras45_NA.grib2";
    logger.debug("{}", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L1");
      assert v != null : ncfile.getLocation();
    }

    // this one has a forecast and error = must be separate variables
    filename = TestDir.cdmUnitTestDir + "formats/grib2/RTMA_CONUS_2p5km_20111225_0000.grib2";
    logger.debug("{}", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      assert ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-3-0_L1") != null; // Pressure_Surface
      assert ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-0-0_error_L103") != null; // Temperature_error_height_above_ground
    }
  }

  @Test
  public void thinGrid() throws Exception {
    // this one has a analysis and forecast in same variable
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/thinGrid.grib2";
    logger.debug("{}", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L105");
      assert v != null : ncfile.getLocation();

      Array data = v.readArray();
      int[] shape = data.getShape();
      assert shape.length == 4;
      assert shape[shape.length - 2] == 1024;
      assert shape[shape.length - 1] == 2048;
    }
  }

  @Test
  public void testJPEG2K() throws Exception {
    // Tests specifically if the land-sea mask from GFS is decoded properly.
    // Not only does this test generally if the decoding works, but covers
    // a corner case with a single-bit field; this case was broken in the
    // original jj2000 code
    String filename = TestDir.cdmUnitTestDir + "tds/ncep/GFS_Global_onedeg_20100913_0000.grib2";
    logger.debug("testJPEG2K: {}", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_2-0-0_L1"); // Land_cover_0__sea_1__land_surface
      int[] origin = {0, 38, 281};
      int[] shape = {1, 1, 2};
      Array<Float> vals = (Array<Float>) v.readArray(new Section(origin, shape));
      Iterator<Float> iter = vals.iterator();
      assertThat(Misc.nearlyEquals(iter.next(), 0.0)).isTrue();
      assertThat(Misc.nearlyEquals(iter.next(), 1.0)).isTrue();
    }
  }

  @Test
  public void testNBits0() throws IOException {
    // Tests of GRIB2 nbits=0; should be reference value (0.0), not missing value
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/SingleRecordNbits0.grib2";
    logger.debug("testNBits0: {}", filename);

    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-1-194_L1");
      assert v != null : ncfile.getLocation();
      Array<Number> vals = (Array<Number>) v.readArray();
      for (Number val : vals) {
        assertThat(val.doubleValue()).isEqualTo(0.0);
      }
    }
  }

  @Ignore("NCEP may be miscoding. Withdraw unit test until we have more info")
  @Test
  public void testScanMode() throws Exception {
    // Robert.C.Lipschutz@noaa.gov
    // we are setting the value of scanMode to 64, which per GRIB2 Table 3.4 indicates "points scan in the +j
    // direction", and so filling
    // the data arrays from south to north.
    /*
     * Hi Bob:
     * 
     * You might think that if scanmode = 64, one should just invert the grids. As it turns out, on all projections
     * except for latlon (that i have sample of),
     * the right thing to do is to ignore the flipping, because the coordinate system (the assignment of lat,lon values
     * to each grid point) correctly adjusts for it. So its just on latlon grids that this issue arises.
     * 
     * So on your file:
     * 
     * C:/Users/caron/Downloads/grid174_scanmode_64_example.grb2
     * 
     * latlon scan mode=64 dLat=0.125000 lat=(89.938004,-89.938004)
     * 
     * Now, the only other example of a latlon Grid that I seem to have with scan mode 64 is
     * 
     * Q:/cdmUnitTest/tds/ncep/SREF_PacificNE_0p4_ensprod_20120213_2100.grib2
     * 
     * latlon scan 64 lat=(10.000000 , 50.000000)
     * 
     * its over the pacific and much harder to tell if its flipped, but im guessing not. Note that its lat range is
     * consistent with scan mode 64.
     * 
     * Im loath to generalize from a sample size of 2. Do you have a sample of GRIB2 files with various encodings?
     * Perhaps I could test them to see if we can guess when to flip or not.
     * 
     * thanks,
     * John
     */
    String filename = TestDir.cdmUnitTestDir + "formats/grib2/grid174_scanmode_64_example.grb2";
    System.out.printf("testScanMode %s%n", filename);
    try (NetcdfFile ncfile = NetcdfFiles.open(filename, null)) {
      Variable v = ncfile.getRootGroup().findVariableByAttribute(Grib.VARIABLE_ID_ATTNAME, "VAR_0-0-0_L1");
      assert v != null : ncfile.getLocation();
      Array<Number> vals = (Array<Number>) Arrays.reduce(v.readArray(new Section("0,:,0")));
      assertThat(Misc.nearlyEquals(vals.get(0).doubleValue(), 243.289993)).isTrue();
      assertThat(Misc.nearlyEquals(vals.get((int) vals.getSize() - 1).doubleValue(), 242.080002)).isTrue();
    }
  }

  // Tests reading a bad ecmwf encoded grib 1 file.
  // gaussian thin grid to boot.
  @Test
  public void testReadBadEcmwf() throws IOException {
    Grib1RecordScanner.setAllowBadDsLength(true);
    Grib1RecordScanner.setAllowBadIsLength(true);

    String filename = TestDir.cdmUnitTestDir + "formats/grib1/problem/badEcmwf.grib1";
    try (NetcdfFile nc = NetcdfFiles.open(filename)) {

      Variable var = nc.findVariable("2_metre_temperature_surface");
      Array<Float> data = (Array<Float>) var.readArray();
      int npts = 2560 * 5136;
      Assert.assertEquals(npts, data.getSize());

      float first = data.get(0, 0, 0);
      float last = data.get(0, 2559, 5135);

      Assert.assertEquals(273.260162, first, 1e-6);
      Assert.assertEquals(224.599670, last, 1e-6);
    }

    Grib1RecordScanner.setAllowBadDsLength(false);
    Grib1RecordScanner.setAllowBadIsLength(false);
  }

}
