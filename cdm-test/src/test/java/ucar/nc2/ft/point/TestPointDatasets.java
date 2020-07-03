/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.ft.point;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.unidata.util.test.CheckPointFeatureDataset;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;
import ucar.unidata.util.test.TestDir;

/** Test PointFeatureTypes. */
@RunWith(Parameterized.class)
@Category(NeedsCdmUnitTest.class)
public class TestPointDatasets {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static List<Object[]> getAllFilesInDirectory(String topdir, FileFilter filter) {
    List<Object[]> result = new ArrayList<>();

    File topDir = new File(topdir);
    File[] filea = topDir.listFiles();
    if (filea == null)
      return result;

    List<FileSort> files = new ArrayList<>();
    for (File f : filea) {
      if (filter != null && !filter.accept(f))
        continue;
      files.add(new FileSort(f));
    }
    Collections.sort(files);

    for (FileSort f : files) {
      result.add(new Object[] {f.path, FeatureType.ANY_POINT});
      System.out.printf("%s%n", f.path);
    }

    return result;
  }

  private static class FileSort implements Comparable<FileSort> {
    String path;
    int order = 10;

    FileSort(File f) {
      this.path = f.getPath();
      String name = f.getName().toLowerCase();
      if (name.contains("point"))
        order = 1;
      else if (name.contains("stationprofile"))
        order = 5;
      else if (name.contains("station"))
        order = 2;
      else if (name.contains("profile"))
        order = 3;
      else if (name.contains("traj"))
        order = 4;
      else if (name.contains("section"))
        order = 6;
    }

    @Override
    public int compareTo(FileSort o) {
      return order - o.order;
    }
  }

  //////////////////////////////////////////////////////////////////////

  static boolean showStructureData = false;
  static boolean showAll = false;

  public static List<Object[]> getCFDatasets() {
    List<Object[]> result = new ArrayList<>();

    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/point/filtered_apriori_super_calibrated_binned1.nc",
        FeatureType.POINT, 1001});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/point/nmcbob.shp.nc", FeatureType.POINT, 1196});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/station/rig_tower.2009-02-01.ncml", FeatureType.STATION,
        17280});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/station/billNewDicast.nc", FeatureType.STATION, 78912});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/station/billOldDicast.nc", FeatureType.STATION, 19728});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, 1728});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/trajectory/rt_20090512_willy2.ncml",
        FeatureType.TRAJECTORY, 53176});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/trajectory/p1140004.ncml", FeatureType.TRAJECTORY, 245});
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "cfPoint/stationProfile/timeSeriesProfile-Ragged-SingleStation-H.5.3.nc",
            FeatureType.STATION_PROFILE, 40});

    // CF 1.0 multidim with dimensions reversed
    // testPointDataset(TestDir.cdmUnitTestDir+"cfPoint/station/solrad_point_pearson.ncml", FeatureType.STATION, true);

    return result;
  }

  public static List<Object[]> getPlugDatasets() {
    List<Object[]> result = new ArrayList<>();

    // cosmic
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "ft/trajectory/cosmic/wetPrf_C005.2007.294.16.22.G17_0001.0002_nc",
            FeatureType.TRAJECTORY, 383});
    // ndbc
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/ndbc/41001h1976.nc", FeatureType.STATION, 1405});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/suomi/suoHWV_2006.105.00.00.0060_nc",
        FeatureType.STATION, 124});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/suomi/gsuPWV_2006.105.00.00.1440_nc",
        FeatureType.STATION, 4848});
    // fsl wind profilers
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_RASS_01hr_20091027_1500.nc",
        FeatureType.STATION_PROFILE, 198});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_RASS_06min_20091028_2318.nc",
        FeatureType.STATION_PROFILE, 198});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_wind_01hr_20091024_1200.nc",
        FeatureType.STATION_PROFILE, 1728});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/stationProfile/PROFILER_wind_06min_20091030_2330.nc",
        FeatureType.STATION_PROFILE, 2088});
    // netcdf buoy / synoptic / metars ( robb's perl decoder output)
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_METAR_latest.nc", FeatureType.POINT, 7});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc",
        FeatureType.POINT, 32452});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Synoptic_20090921_0000.nc",
        FeatureType.POINT, 1516});
    // RAF-Nimbus
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "ft/trajectory/aircraft/135_ordrd.nc", FeatureType.TRAJECTORY, 7741});
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "ft/trajectory/aircraft/raftrack.nc", FeatureType.TRAJECTORY, 8157});
    // Madis
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/trajectory/acars/acars_20091109_0800.nc",
        FeatureType.TRAJECTORY, 5063});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/19981110_1200", FeatureType.POINT, 2499});
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "ft/station/madis2/hydro/20050729_1200", FeatureType.STATION, 1374});
    result.add(
        new Object[] {TestDir.cdmUnitTestDir + "ft/sounding/netcdf/20070612_1200", FeatureType.STATION_PROFILE, 1788});
    // unidata point obs
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, 5023});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/20080814_LMA.ncml", FeatureType.POINT, 277477});

    // FslRaob
    // assert 63 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/raob_soundings20216.cdf",
    // FeatureType.STATION_PROFILE, false);
    // assert 4638 == checkPointDataset(TestDir.testdataDir + "sounding/netcdf/Upperair_20060621_0000.nc",
    // FeatureType.STATION_PROFILE, false);

    return result;
  }

  // lots of trouble - remove for now
  public static List<Object[]> getGempakDatasets() {
    List<Object[]> result = new ArrayList<>();

    // gempack sounding
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml",
        FeatureType.STATION_PROFILE, 8769});

    // gempak surface
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/gempak/2009103008_sb.gem", FeatureType.POINT, 3337});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/gempak/2009110100_ship.gem", FeatureType.POINT, 938});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.POINT, 55856});
    result
        .add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/gempak/20091030_syn.gem", FeatureType.STATION, 28328});

    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml",
        FeatureType.STATION_PROFILE, 8769});

    // (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    // checkPointDataset(TestDir.cdmUnitTestDir + "formats/gempak/surface/19580807_sao.gem", FeatureType.STATION, true);

    // stationAsPoint (GEMPAK IOSP) stn = psuedoStruct, obs = multidim Structure, time(time) as extraJoin
    // testPointDataset(TestDir.cdmUnitTestDir + "formats/gempak/surface/20090521_sao.gem", FeatureType.POINT, true);

    // testGempakAll(TestDir.cdmUnitTestDir + "formats/gempak/surface/20090524_sao.gem");
    // testGempakAll(TestDir.cdmUnitTestDir+"C:/data/ft/station/09052812.sf");

    // testPointDataset("collection:C:/data/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);
    // checkPointDataset("collection:D:/formats/gempak/surface/#yyyyMMdd#_sao\\.gem", FeatureType.STATION, true);

    return result;
  }


  public static List<Object[]> getMiscDatasets() {
    List<Object[]> result = new ArrayList<>();
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/ldm/04061912_buoy.nc", FeatureType.POINT, 218});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/point/netcdf/Surface_Buoy_20090921_0000.nc",
        FeatureType.POINT, 32452});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/multiStationMultiVar.ncml", FeatureType.STATION, 15});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "cfPoint/station/sampleDataset.nc", FeatureType.STATION, 1728});
    result.add(new Object[] {TestDir.cdmUnitTestDir + "ft/station/200501q3h-gr.nc", FeatureType.STATION, 5023}); // */
    return result;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> getTestParameters() {
    List<Object[]> result = new ArrayList<>();

    result.addAll(getCFDatasets());
    result.addAll(getPlugDatasets());
    result.addAll(getMiscDatasets());

    return result;
  }

  String location;
  FeatureType ftype;
  int countExpected;
  boolean show = false;

  public TestPointDatasets(String location, FeatureType ftype, int countExpected) {
    this.location = location;
    this.ftype = ftype;
    this.countExpected = countExpected;
  }

  @Test
  public void checkPointFeatureDataset() throws IOException {
    CheckPointFeatureDataset checker = new CheckPointFeatureDataset(location, ftype, show);
    Assert.assertEquals("npoints", countExpected, checker.check());
  }
}
