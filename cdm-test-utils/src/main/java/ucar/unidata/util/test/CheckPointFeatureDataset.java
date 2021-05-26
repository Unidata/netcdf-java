/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.util.test;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.DataType;
import ucar.ma2.StructureData;
import ucar.ma2.StructureMembers;
import ucar.nc2.VariableSimpleIF;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCC;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.ProfileFeature;
import ucar.nc2.ft.ProfileFeatureCollection;
import ucar.nc2.ft.StationProfileFeature;
import ucar.nc2.ft.StationProfileFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeature;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.ft.TrajectoryProfileFeature;
import ucar.nc2.ft.TrajectoryProfileFeatureCollection;
import ucar.nc2.ft.point.CollectionInfo;
import ucar.nc2.ft.point.DsgCollectionHelper;
import ucar.nc2.time2.CalendarDate;
import ucar.nc2.time2.CalendarDateRange;
import ucar.nc2.time2.CalendarDateUnit;
import ucar.nc2.ft.IOIterator;
import ucar.nc2.write.Ncdump;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.StringUtil2;

/** Read and check a point feature dataset, return number of point features. */
public class CheckPointFeatureDataset {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static boolean showStructureData = false;
  private static boolean showAll = false;

  private final String location;
  private final FeatureType ftype;
  private final boolean show;

  public CheckPointFeatureDataset(String location, FeatureType ftype, boolean show) {
    this.location = location;
    this.ftype = ftype;
    this.show = show;
  }

  // return number of PointFeatures
  public int check() throws IOException {
    return checkPointFeatureDataset(location, ftype, show);
  }

  // return number of PointFeatures
  private int checkPointFeatureDataset(String location, FeatureType type, boolean show) throws IOException {
    File fileIn = new File(location);
    String absIn = fileIn.getCanonicalPath();
    absIn = StringUtil2.replace(absIn, "\\", "/");

    System.out.printf("================ TestPointFeatureCollection read %s %n", absIn);
    File cwd = new File(".");
    System.out.printf("**** CWD = %s%n", cwd.getAbsolutePath());

    File f = new File(absIn);
    System.out.printf("**** %s = %s%n", f.getAbsolutePath(), f.exists());

    Formatter out = new Formatter();
    try (FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out)) {
      if (fdataset == null) {
        System.out.printf("**failed on %s %n --> %s %n", location, out);
        assert false;
      }

      // FeatureDataset
      if (showAll) {
        System.out.printf("----------- testPointDataset getDetailInfo -----------------%n");
        fdataset.getDetailInfo(out);
        System.out.printf("%s %n", out);
      } else {
        System.out.printf("  Feature Type %s %n", fdataset.getFeatureType());
      }

      return checkPointFeatureDataset(fdataset, show);
    }

  }

  public static int checkPointFeatureDataset(FeatureDataset fdataset, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    CalendarDate d1 = fdataset.getCalendarDateStart();
    CalendarDate d2 = fdataset.getCalendarDateEnd();
    if ((d1 != null) && (d2 != null)) {
      Assert.assertTrue("calendar date min <= max", d1.isBefore(d2) || d1.equals(d2));
    }

    List<VariableSimpleIF> dataVars = fdataset.getDataVariables();
    Assert.assertNotNull("fdataset.getDataVariables()", dataVars);
    for (VariableSimpleIF v : dataVars) {
      Assert.assertNotNull(v.getShortName(), fdataset.getDataVariable(v.getShortName()));
    }

    // FeatureDatasetPoint
    Assert.assertTrue("fdataset instanceof FeatureDatasetPoint", fdataset instanceof FeatureDatasetPoint);
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    for (DsgFeatureCollection fc : fdpoint.getPointFeatureCollectionList()) {
      checkDsgFeatureCollection(fc);

      if (fc instanceof PointFeatureCollection) {
        PointFeatureCollection pfc = (PointFeatureCollection) fc;
        count = checkPointFeatureCollection(pfc, show);
        Assert.assertEquals("PointFeatureCollection getData count = size", count, pfc.size());

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        count = checkStationFeatureCollection((StationTimeSeriesFeatureCollection) fc);
        // testNestedPointFeatureCollection((StationTimeSeriesFeatureCollection) fc, show);

      } else if (fc instanceof StationProfileFeatureCollection) {
        count = checkStationProfileFeatureCollection((StationProfileFeatureCollection) fc, show);
        if (showStructureData) {
          showStructureData((StationProfileFeatureCollection) fc);
        }

      } else if (fc instanceof TrajectoryProfileFeatureCollection) {
        count = checkSectionFeatureCollection((TrajectoryProfileFeatureCollection) fc, show);

      } else if (fc instanceof ProfileFeatureCollection) {
        count = checkProfileFeatureCollection((ProfileFeatureCollection) fc, show);

      } else {
        count = checkOther(fc, show);
      }
      checkInfo(fc);
    }

    long took = System.currentTimeMillis() - start;
    System.out.printf(" nobs=%d took= %d msec%n", count, took);
    return count;
  }

  static void checkDsgFeatureCollection(DsgFeatureCollection dsg) throws IOException {
    String what = dsg.getClass().getName();
    Assert.assertNotNull(what + " name", dsg.getName());
    Assert.assertNotNull(what + " featureTYpe", dsg.getCollectionFeatureType());
    Assert.assertNotNull(what + " timeUnit", dsg.getTimeUnit());
    // Assert.assertNotNull(what + " altUnits", dsg.getAltUnits());
    // Assert.assertNotNull(what + " extraVars", dsg.getExtraVariables());
  }

  static void checkInfo(DsgFeatureCollection dsg) throws IOException {
    Assert.assertNotNull(dsg.getBoundingBox());
    Assert.assertNotNull(dsg.getCalendarDateRange());
    Assert.assertNotNull(dsg.size() > 0);
  }

  static int checkPointFeatureCollection(PointFeatureCollection pfc, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int counts = 0;
    for (PointFeature pf : pfc) {
      checkPointFeature(pf, pfc.getTimeUnit());
      counts++;
    }
    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");
    }

    checkPointFeatureCollectionBB(pfc, show);
    return counts;
  }

  static int checkProfileFeatureCollection(ProfileFeatureCollection profileFeatureCollection, boolean show)
      throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    Set<String> profileNames = new HashSet<>();
    for (ProfileFeature profile : profileFeatureCollection) {
      checkDsgFeatureCollection(profile);
      Assert.assertNotNull("ProfileFeature time", profile.getTime());
      Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
      Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
      Assert.assertTrue(!profileNames.contains(profile.getName()));
      profileNames.add(profile.getName());

      // assert pf.getTime() != null;
      count += checkPointFeatureCollection(profile, show);
    }
    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(
          " testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    }
    return count;
  }

  static int checkStationFeatureCollection(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf("--------------------------\nComplete Iteration for %s %n", sfc.getName());
    int countStns = countLocations(sfc);

    // try a subset
    LatLonRect bb = sfc.getBoundingBox();
    Assert.assertNotNull(bb);
    LatLonRect bb2 = new LatLonRect.Builder(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2).build();
    System.out.println(" BB Subset= " + bb2.toString2());
    StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
    int countSub = countLocations(sfcSub);
    Assert.assertTrue(countSub <= countStns);
    System.out.println("  nobs= " + sfcSub.size());

    /*
     * test info
     * CollectionInfo info = new DsgCollectionHelper(sfc).calcBounds(); // sets internal values
     * Assert.assertNotNull(info);
     * CalendarDateRange dr = sfc.getCalendarDateRange();
     * Assert.assertNotNull(dr);
     * Assert.assertEquals(info.getCalendarDateRange(null), dr);
     *
     * // subset(bb, dr);
     * long diff = dr.getEnd().getDifferenceInMsecs(dr.getStart());
     * CalendarDate mid = dr.getStart().add(diff/2, CalendarPeriod.Field.Millisec);
     * CalendarDateRange drsubset = CalendarDateRange.of(dr.getStart(), mid);
     * System.out.println(" CalendarDateRange Subset= " + drsubset);
     * StationTimeSeriesFeatureCollection sfcSub2 = sfc.subset(bb2, drsubset);
     * for (StationTimeSeriesFeature sf : sfcSub2) {
     * Assert.assertTrue( bb2.contains(sf.getLatLon()));
     * for (PointFeature pf : sf) {
     * Assert.assertEquals(sf.getLatLon(), pf.getLocation().getLatLon());
     * Assert.assertTrue(drsubset.includes(pf.getObservationTimeAsCalendarDate()));
     * // Assert.assertTrue( pf.getClass().getName(), pf instanceof StationFeature);
     * }
     * }
     * System.out.println("  nobs= " + sfcSub2.size());
     * Assert.assertTrue(sfcSub2.size() <= sfcSub.size()); //
     */

    System.out.println("Flatten= " + bb2.toString2());
    PointFeatureCollection flatten = sfc.flatten(bb2, null);
    int countFlat = countLocations(flatten);
    assert countFlat <= countStns;

    flatten = sfc.flatten(null, null, null);
    return countObs(flatten);
  }

  static int countLocations(StationTimeSeriesFeatureCollection sfc) throws IOException {
    System.out.printf(" Station List Size = %d %n", sfc.getStationFeatures().size());

    // check uniqueness
    Map<String, StationTimeSeriesFeature> stns = new HashMap<>(5000);
    Map<MyLocation, StationTimeSeriesFeature> locs = new HashMap<>(5000);

    int dups = 0;
    for (StationTimeSeriesFeature sf : sfc) {
      StationTimeSeriesFeature other = stns.get(sf.getName());
      if (other != null && dups < 10) {
        System.out.printf("  duplicate name = %s %n", sf);
        System.out.printf("   of = %s %n", other);
        dups++;
      } else {
        stns.put(sf.getName(), sf);
      }

      MyLocation loc = new MyLocation(sf.getStation());
      StationTimeSeriesFeature already = locs.get(loc);
      if (already != null) {
        System.out.printf("  duplicate location %s(%s) of %s(%s) %n", sf.getName(), sf.getStation().getDescription(),
            already.getName(), already.getStation().getDescription());
      } else {
        locs.put(loc, sf);
      }
    }

    System.out.printf(" duplicate names = %d %n", dups);
    System.out.printf(" unique locs = %d %n", locs.size());
    System.out.printf(" unique stns = %d %n", stns.size());

    return stns.size();
  }

  static int checkStationProfileFeatureCollection(StationProfileFeatureCollection stationProfileFeatureCollection,
      boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    for (StationProfileFeature spf : stationProfileFeatureCollection) {
      checkDsgFeatureCollection(spf);
      Assert.assertNotNull("StationProfileFeature latlon", spf.getStation().getLatLon());
      Assert.assertNotNull("StationProfileFeature featureData", spf.getFeatureData());

      // iterates through the profile but not the profile data
      List<CalendarDate> times = spf.getTimes();
      if (showAll) {
        System.out.printf("  times= ");
        for (CalendarDate t : times) {
          System.out.printf("%s, ", t);
        }
        System.out.printf("%n");
      }

      Set<String> profileNames = new HashSet<>();
      for (ProfileFeature profile : spf) {
        checkDsgFeatureCollection(profile);
        Assert.assertNotNull("ProfileFeature time", profile.getTime());
        Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
        Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
        Assert.assertTrue(!profileNames.contains(profile.getName()));
        profileNames.add(profile.getName());

        if (show) {
          System.out.printf(" ProfileFeature=%s %n", profile.getName());
        }
        count += checkPointFeatureCollection(profile, show);
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(
          " testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    }
    return count;
  }

  static int checkSectionFeatureCollection(TrajectoryProfileFeatureCollection sectionFeatureCollection, boolean show)
      throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;
    for (TrajectoryProfileFeature section : sectionFeatureCollection) {
      checkDsgFeatureCollection(section);
      Assert.assertNotNull("SectionFeature featureData", section.getFeatureData());

      Set<String> profileNames = new HashSet<>();
      for (ProfileFeature profile : section) {
        checkDsgFeatureCollection(profile);
        Assert.assertNotNull("ProfileFeature time", profile.getTime());
        Assert.assertNotNull("ProfileFeature latlon", profile.getLatLon());
        Assert.assertNotNull("ProfileFeature featureData", profile.getFeatureData());
        Assert.assertTrue(!profileNames.contains(profile.getName()));
        profileNames.add(profile.getName());

        if (show) {
          System.out.printf(" ProfileFeature=%s %n", profile.getName());
        }
        count += checkPointFeatureCollection(profile, show);
      }
    }
    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(
          " testStationProfileFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    }
    return count;
  }

  // stuff we havent got around to coding specific tests for
  static int checkOther(DsgFeatureCollection dsg, boolean show) throws IOException {
    long start = System.currentTimeMillis();
    int count = 0;

    // we will just run through everything
    try {
      CollectionInfo info = new DsgCollectionHelper(dsg).calcBounds();
      if (show) {
        System.out.printf(" info=%s%n", info);
      }
      count = info.nobs;

    } catch (IOException e) {
      e.printStackTrace();
      return 0;
    }

    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out
          .println(" testNestedPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    }

    return count;
  }

  /////////////////////////////////////////////


  // check that the location and times are filled out
  // read and test the data
  static private void checkPointFeature(PointFeature pobs, CalendarDateUnit timeUnit) throws java.io.IOException {

    Assert.assertNotNull("PointFeature location", pobs.getLocation());
    Assert.assertNotNull("PointFeature time", pobs.getNominalTimeAsCalendarDate());
    Assert.assertNotNull("PointFeature dataAll", pobs.getDataAll());
    Assert.assertNotNull("PointFeature featureData", pobs.getFeatureData());

    // LOOK
    Assert.assertEquals("PointFeature makeCalendarDate", timeUnit.makeCalendarDate((long) pobs.getObservationTime()),
        pobs.getObservationTimeAsCalendarDate());

    assert timeUnit.makeCalendarDate((long) pobs.getObservationTime()).equals(pobs.getObservationTimeAsCalendarDate());
    checkData(pobs.getDataAll());
  }

  // read each field, check datatype
  static private void checkData(StructureData sdata) {

    for (StructureMembers.Member member : sdata.getMembers()) {
      DataType dt = member.getDataType();
      if (dt == DataType.FLOAT) {
        sdata.getScalarFloat(member);
        sdata.getJavaArrayFloat(member);
      } else if (dt == DataType.DOUBLE) {
        sdata.getScalarDouble(member);
        sdata.getJavaArrayDouble(member);
      } else if (dt == DataType.BYTE) {
        sdata.getScalarByte(member);
        sdata.getJavaArrayByte(member);
      } else if (dt == DataType.SHORT) {
        sdata.getScalarShort(member);
        sdata.getJavaArrayShort(member);
      } else if (dt == DataType.INT) {
        sdata.getScalarInt(member);
        sdata.getJavaArrayInt(member);
      } else if (dt == DataType.LONG) {
        sdata.getScalarLong(member);
        sdata.getJavaArrayLong(member);
      } else if (dt == DataType.CHAR) {
        sdata.getScalarChar(member);
        sdata.getJavaArrayChar(member);
        sdata.getScalarString(member);
      } else if (dt == DataType.STRING) {
        sdata.getScalarString(member);
      }

      if ((dt != DataType.STRING) && (dt != DataType.CHAR) && (dt != DataType.STRUCTURE) && (dt != DataType.SEQUENCE)) {
        sdata.convertScalarFloat(member.getName());
      }

    }
  }

  static void showStructureData(PointFeatureCCC ccc) throws IOException {
    PrintWriter pw = new PrintWriter(System.out);

    IOIterator<PointFeatureCC> iter = ccc.getCollectionIterator();
    while (iter.hasNext()) {
      PointFeatureCC cc = iter.next();
      System.out.printf(" 1.hashCode=%d %n", cc.hashCode());
      IOIterator<PointFeatureCollection> iter2 = cc.getCollectionIterator();
      while (iter2.hasNext()) {
        PointFeatureCollection pfc = iter2.next();
        System.out.printf("  2.hashcode%d %n", pfc.hashCode());

        for (ucar.nc2.ft.PointFeature pointFeature : pfc) {
          System.out.printf("   3.hashcode=%d %n", pointFeature.hashCode());
          StructureData sdata = pointFeature.getDataAll();
          logger.debug(Ncdump.printStructureData(sdata));
        }
      }
    }
  }

  /////////////////////////////////////////////////////

  static int checkPointFeatureCollectionBB(PointFeatureCollection pfc, boolean show) throws IOException {
    if (show) {
      System.out.printf("----------- testPointFeatureCollection -----------------%n");
      System.out.println(" test PointFeatureCollection " + pfc.getName());
      System.out.println(" calcBounds");
    }
    if (show) {
      System.out.println("  bb= " + pfc.getBoundingBox());
      System.out.println("  dateRange= " + pfc.getCalendarDateRange());
      System.out.println("  npts= " + pfc.size());
    }

    int n = pfc.size();
    if (n == 0) {
      System.out.println("  empty " + pfc.getName());
      return 0; // empty
    }

    LatLonRect bb = pfc.getBoundingBox();
    assert bb != null;
    CalendarDateRange dr = pfc.getCalendarDateRange();
    assert dr != null;

    // read all the data - check that it is contained in the bbox, dateRange
    if (show) {
      System.out.println(" complete iteration");
    }
    long start = System.currentTimeMillis();
    int count = 0;
    for (PointFeature pf : pfc) {
      checkPointFeature(pf, pfc.getTimeUnit());
      if (!bb.contains(pf.getLocation().getLatLon())) {
        System.out.printf("  point not in BB = %s on %s %n", pf.getLocation().getLatLon(), pfc.getName());
      }

      if (!dr.includes(pf.getObservationTimeAsCalendarDate())) {
        System.out.printf("  date out of Range= %s on %s %n", pf.getObservationTimeAsCalendarDate(), pfc.getName());
      }
      count++;
    }
    long took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(" testPointFeatureCollection complete count= " + count + " full iter took= " + took + " msec");
    }

    // subset with a bounding box, test result is in the bounding box
    LatLonRect bb2 = new LatLonRect.Builder(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2).build();
    PointFeatureCollection subset = pfc.subset(bb2, null);
    if (show) {
      System.out.println(" subset bb= " + bb2.toString2());
    }
    assert subset != null;

    start = System.currentTimeMillis();
    int counts = 0;
    for (PointFeature pf : subset) {
      LatLonPoint llpt = pf.getLocation().getLatLon();
      if (!bb2.contains(llpt)) {
        System.out.printf("  point not in BB = %s on %s %n", llpt, pfc.getName());
        bb2.contains(llpt);
      }

      checkPointFeature(pf, pfc.getTimeUnit());
      counts++;
    }
    took = System.currentTimeMillis() - start;
    if (show) {
      System.out.println(" testPointFeatureCollection subset count= " + counts + " full iter took= " + took + " msec");
    }

    return count;
  }

  ////////////////////////////////////////////////////////////

  /////////////////////////////////////////////////////////


  public static void checkLocation(String location, FeatureType type, boolean show) throws IOException {
    Formatter out = new Formatter();
    FeatureDataset fdataset = FeatureDatasetFactoryManager.open(type, location, null, out);
    if (fdataset == null) {
      System.out.printf("**failed on %s %n --> %s %n", location, out);
      assert false;
    }
    assert fdataset instanceof FeatureDatasetPoint;
    FeatureDatasetPoint fdpoint = (FeatureDatasetPoint) fdataset;

    List<DsgFeatureCollection> collectionList = fdpoint.getPointFeatureCollectionList();

    DsgFeatureCollection fc = collectionList.get(0);

    if (fc instanceof PointFeatureCollection) {
      PointFeatureCollection pfc = (PointFeatureCollection) fc;
      countLocations(pfc);

      LatLonRect bb = pfc.getBoundingBox();
      LatLonRect bb2 = new LatLonRect.Builder(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2).build();
      PointFeatureCollection subset = pfc.subset(bb2, (CalendarDateRange) null);
      countLocations(subset);

    } else if (fc instanceof StationTimeSeriesFeatureCollection) {
      StationTimeSeriesFeatureCollection sfc = (StationTimeSeriesFeatureCollection) fc;
      PointFeatureCollection pfcAll = sfc.flatten(null, (CalendarDateRange) null);
      System.out.printf("Unique Locations all = %d %n", countLocations(pfcAll));

      LatLonRect bb = sfc.getBoundingBox();
      assert bb != null;
      LatLonRect bb2 = new LatLonRect.Builder(bb.getLowerLeftPoint(), bb.getHeight() / 2, bb.getWidth() / 2).build();
      PointFeatureCollection pfcSub = sfc.flatten(bb2, (CalendarDateRange) null);
      System.out.printf("Unique Locations sub1 = %d %n", countLocations(pfcSub));

      StationTimeSeriesFeatureCollection sfcSub = sfc.subset(bb2);
      PointFeatureCollection pfcSub2 = sfcSub.flatten(null, (CalendarDateRange) null);
      System.out.printf("Unique Locations sub2 = %d %n", countLocations(pfcSub2));

      // Dons
      sfc = sfc.subset(bb2);
      PointFeatureCollection subDon = sfc.flatten(bb2, (CalendarDateRange) null);
      System.out.printf("Unique Locations subDon = %d %n", countLocations(subDon));
    }

  }

  static int countLocations(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    Set<MyLocation> locs = new HashSet<>(80000);
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      MyLocation loc = new MyLocation(pf.getLocation());
      if (!locs.contains(loc)) {
        locs.add(loc);
      }
      count++;
      // if (count % 1000 == 0) System.out.printf("Count %d%n", count);
    }

    System.out.printf("Count Points  = %d Unique points = %d %n", count, locs.size());
    return locs.size();

    // The problem is that all the locations are coming up with the same value. This:
    // always returns the same lat/lon/alt (of the first observation).
    // (pos was populated going through the PointFeatureIterator).

  }

  static int countObs(PointFeatureCollection pfc) throws IOException {
    int count = 0;
    pfc.resetIteration();
    while (pfc.hasNext()) {
      PointFeature pf = pfc.next();
      StructureData sd = pf.getDataAll();
      count++;
    }
    return count;
  }

  private static class MyLocation {

    double lat, lon, alt;

    public MyLocation(EarthLocation from) {
      this.lat = from.getLatitude();
      this.lon = from.getLongitude();
      this.alt = Double.isNaN(from.getAltitude()) ? 0.0 : from.getAltitude();
    }

    @Override
    public boolean equals(Object oo) {
      if (this == oo) {
        return true;
      }
      if (!(oo instanceof MyLocation)) {
        return false;
      }
      MyLocation other = (MyLocation) oo;
      return (lat == other.lat) && (lon == other.lon) && (alt == other.alt);
    }

    @Override
    public int hashCode() {
      if (hashCode == 0) {
        int result = 17;
        result += 37 * result + lat * 10000;
        result += 37 * result + lon * 10000;
        result += 37 * result + alt * 10000;
        hashCode = result;
      }
      return hashCode;
    }

    private int hashCode = 0;
  }

}

