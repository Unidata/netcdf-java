/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.jni.netcdf;

import java.io.IOException;
import java.util.Formatter;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;
import ucar.nc2.internal.util.CompareNetcdf2;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

/** Compare reading netcdf through jni with native java reading */
@Category(NeedsCdmUnitTest.class)
public class TestNc4JniReadProblem {

  @Before
  public void setLibrary() {
    // Ignore this class's tests if NetCDF-4 isn't present.
    // We're using @Before because it shows these tests as being ignored.
    // @BeforeClass shows them as *non-existent*, which is not what we want.
    Assume.assumeTrue("NetCDF-4 C library not present.", NetcdfClibrary.isLibraryPresent());
  }

  /*
   * netcdf D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/vlen/IntTimSciSamp.nc {
   * dimensions:
   * time = UNLIMITED; // (29 currently)
   * variables:
   * long time(time=29);
   * :_ChunkSizes = 1U; // uint
   * 
   * 
   * Structure {
   * int shutterPositionA;
   * int shutterPositionD;
   * int shutterPositionB;
   * int shutterPositionC;
   * int dspGainMode;
   * int coneActiveStateA;
   * int coneActiveStateD;
   * int coneActiveStateB;
   * int coneActiveStateC;
   * int loopDataA(1, *);
   * int loopDataB(1, *);
   * long sampleVtcw;
   * } tim_records(time=29);
   * :_ChunkSizes = 1U; // uint
   * }
   */
  @Test
  @Ignore("dont have a fix yet")
  public void problemWithVlen() throws IOException {
    // Not sure if either java or jni is correct.
    // When fixed, should test if reading enhanced works.
    // Note that "showData" in UI isnt right
    compareDatasets(TestDir.cdmUnitTestDir + "formats/netcdf4/vlen/IntTimSciSamp.nc");
  }

  /*
   * netcdf
   * D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc {
   * dimensions:
   * time = 8395;
   * UTM_Meters_East = 385;
   * UTM_Meters_North = 781;
   * variables:
   * double lon(UTM_Meters_North=781, UTM_Meters_East=385);
   * :long_name = "Longitude";
   * :standard_name = "longitude";
   * :units = "degrees_east";
   * :_ChunkSizes = 30U, 30U; // uint
   * :_CoordinateAxisType = "Lon";
   * 
   * double UpperDeschutes_t4p10_swemelt(time=8395, UTM_Meters_North=781, UTM_Meters_East=385);
   * :units = "kg m-2";
   * :long_name = "Daily Total Snowmelt (m)";
   * :standard_name = "surface_snow_melt_amount";
   * :description =
   * "technically, liquid water equivalent of snowmelt at the surface. There exists no Unidata convention standard_name for liquid water equivalent of snow melt thus we use \'amount\' which is kg m-2 but is equivalent to a depth in meters when converted via the constant density of water"
   * ;
   * :coordinates = "lon lat";
   * :grid_mapping = "UTM_PROJECTION";
   * :_ChunkSizes = 1U, 30U, 30U; // uint
   * ...
   */
  @Test
  public void problemWithLargeData() throws IOException {
    // UpperDeschutes_t4p10_swemelt(time=8395, UTM_Meters_North=781, UTM_Meters_East=385) has size > 2Gb
    compareDatasets(TestDir.cdmUnitTestDir + "formats/netcdf4/UpperDeschutes_t4p10_swemelt.nc");
  }

  /*
   * netcdf D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/tst/tst_opaques.nc {
   * dimensions:
   * dim = 3;
   * variables:
   * opaque var(dim=3);
   * :_ChunkSizes = 3U; // uint
   * }
   * 
   * $ ncdump D:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/tst/tst_opaques.nc
   * netcdf D\:/testData/thredds-test-data/local/thredds-test-data/cdmUnitTest/formats/netcdf4/tst/tst_opaques {
   * types:
   * opaque(20) type ;
   * dimensions:
   * dim = 3 ;
   * variables:
   * type var(dim) ;
   * data:
   * 
   * var = 0X0000000000000000000000000000000000000000,
   * 0X0000000000000000000000000000000000000000,
   * 0X0000000000000000000000000000000000000000 ;
   * }
   * 
   */
  @Test
  public void problemWithOpaque() throws IOException {
    compareDatasets(TestDir.cdmUnitTestDir + "formats/netcdf4/tst/tst_opaques.nc");
  }

  private void compareDatasets(String filename) throws IOException {
    try (NetcdfFile ncfile = NetcdfFiles.open(filename); NetcdfFile jni = TestNc4reader.openJni(filename)) {
      System.err.println("Test input: " + ncfile.getLocation());
      System.err.println("Baseline: " + jni.getLocation());
      System.err.flush();

      Formatter f = new Formatter();
      CompareNetcdf2 mind = new CompareNetcdf2(f, true, true, true);
      boolean ok = mind.compare(ncfile, jni, new CompareNetcdf2.Netcdf4ObjectFilter());
      if (!ok) {
        System.out.printf("--Compare %s%n", filename);
        System.out.printf("  %s%n", f);
      } else {
        System.out.printf("--Compare %s is OK%n", filename);
      }
      Assert.assertTrue(filename, ok);
    }
  }

}

