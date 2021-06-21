package ucar.nc2.grib.coord;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import thredds.featurecollection.FeatureCollectionConfig;
import ucar.nc2.calendar.CalendarDate;
import ucar.nc2.grib.collection.GribCdmIndex;
import ucar.nc2.grib.collection.GribCollectionImmutable;
import ucar.nc2.util.Indent;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test Coordinate2D is regular in cdmUnitTestDir test files. */
public class TestIsRegular {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final FeatureCollectionConfig config = new FeatureCollectionConfig();

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testHrrrConus3Wrfprs() throws IOException {
    String filename =
        TestDir.cdmUnitTestDir + "tds_index/NOAA_GSD/HRRR/CONUS_3km/wrfprs/GSD_HRRR_CONUS_3km_wrfprs.ncx4";
    boolean sa = testIsRegular(filename, "validtime1");
    assertThat(sa).isFalse();
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testNdfdNws() throws IOException {
    String filename = TestDir.cdmUnitTestDir + "tds_index/NCEP/NDFD/NWS/NDFD_NWS_CONUS_CONDUIT.ncx4";
    String cname = "validtime1";
    boolean sa = testIsRegular(filename, cname);
    assertThat(sa).isTrue();
  }

  private boolean testIsRegular(String filename, String coordName) throws IOException {
    System.out.printf("testIsRegular %s%n", filename);
    try (GribCollectionImmutable gc = GribCdmIndex.openCdmIndex(filename, config, false, logger)) {
      assertThat(gc).isNotNull();
      for (GribCollectionImmutable.Dataset ds : gc.getDatasets()) {
        if (ds.getType() == GribCollectionImmutable.Type.Best) {
          continue;
        }
        GribCollectionImmutable.GroupGC g = Iterables.getOnlyElement(ds.getGroups());
        Coordinate coord =
            g.getCoordinates().stream().filter(c -> c.getName().equals(coordName)).findFirst().orElseThrow();
        assertThat(coord).isInstanceOf(CoordinateTime2D.class);
        return testRegular((CoordinateTime2D) coord);
      }
    }
    fail();
    return false;
  }

  // regular means that all the times for each offset from 0Z can be made into a single time coordinate (FMRC algo)
  private boolean testRegular(CoordinateTime2D time2D) {
    // group time coords by offset
    TreeMap<Integer, List<CoordinateTimeAbstract>> minuteMap = new TreeMap<>(); // <hour, all coords for that hour>
    for (int runIdx = 0; runIdx < time2D.getNruns(); runIdx++) {
      CoordinateTimeAbstract coord = time2D.getTimeCoordinate(runIdx);
      CalendarDate runDate = coord.getRefDate();
      int hour = runDate.getHourOfDay();
      int min = runDate.getMinuteOfHour();
      int minutes = 60 * hour + min;
      List<CoordinateTimeAbstract> hg = minuteMap.computeIfAbsent(minutes, k -> new ArrayList<>());
      hg.add(coord);
    }
    show(minuteMap);

    // see if each offset set is orthogonal
    boolean ok = true;
    for (int hour : minuteMap.keySet()) {
      List<CoordinateTimeAbstract> hg = minuteMap.get(hour);
      boolean isOrthogonal = testOrthogonal(hg, false); // LOOK why orthogonal, why not regular?
      System.out.printf("Hour %d: isOrthogonal=%s%n", hour, isOrthogonal);
      ok &= isOrthogonal;
    }
    System.out.printf("%nAll are orthogonal: %s%n", ok);
    if (ok) {
      return true;
    }

    for (int hour : minuteMap.keySet()) {
      List<CoordinateTimeAbstract> hg = minuteMap.get(hour);
      System.out.printf("Hour %d: %n", hour);
      testOrthogonal(hg, true);
      System.out.printf("%n");
    }
    return false;
  }

  private void show(Map<Integer, List<CoordinateTimeAbstract>> minuteMap) {
    for (Map.Entry<Integer, List<CoordinateTimeAbstract>> entry : minuteMap.entrySet()) {
      int hour = entry.getKey();
      Formatter info = new Formatter();
      info.format("Minute %d%n", hour);
      List<CoordinateTimeAbstract> coords = entry.getValue();
      for (CoordinateTimeAbstract timeCoord : coords) {
        timeCoord.showInfo(info, new Indent(0));
      }
      System.out.printf("%s", info);
    }
  }


  // orthogonal means that all the times can be made into a single time coordinate
  private boolean testIsOrthogonal(CoordinateTime2D time2D) {
    // LOOK bogus this is not testig, just confiring that time2D.isOrthoganal = true
    List<CoordinateTimeAbstract> coords = new ArrayList<>();
    for (int runIdx = 0; runIdx < time2D.getNruns(); runIdx++) {
      coords.add(time2D.getTimeCoordinate(runIdx));
    }
    return testOrthogonal(coords, true);
  }

  private boolean testOrthogonal(List<CoordinateTimeAbstract> times, boolean show) {
    int max = 0;
    Map<Object, Integer> allCoords = new HashMap<>(1000);
    for (CoordinateTimeAbstract coord : times) {
      max = Math.max(max, coord.getSize());
      for (Object val : coord.getValues()) {
        allCoords.merge(val, 1, (a, b) -> a + b);
      }
    }

    // is the set of all values the same as the component times?
    int totalMax = allCoords.size();
    boolean isOrthogonal = (totalMax == max);

    if (show) {
      System.out.printf("isOrthogonal %s : allCoords.size = %d max of coords=%d%nAllCoords=%n", isOrthogonal, totalMax,
          max);
      List allList = new ArrayList<>(allCoords.keySet());
      Collections.sort(allList);
      for (Object coord : allList) {
        Integer count = allCoords.get(coord);
        System.out.printf("  %4d %s%n", count, coord);
      }
    }

    return isOrthogonal;
  }
}
