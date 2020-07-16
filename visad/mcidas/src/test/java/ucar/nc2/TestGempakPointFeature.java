/*
 * Copyright (c) 1998-2020 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.StationTimeSeriesFeatureCollection;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.test.CheckPointFeatureDataset;
import ucar.unidata.util.test.TestDir;
import ucar.unidata.util.test.category.NeedsCdmUnitTest;

public class TestGempakPointFeature {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGempakStationProfile() throws IOException {
    String location = TestDir.cdmUnitTestDir + "ft/sounding/gempak/19580807_upa.ncml";
    CheckPointFeatureDataset checker = new CheckPointFeatureDataset(location, FeatureType.STATION_PROFILE, true);
    Assert.assertEquals("npoints", 8769, checker.check());
  }

  @Test
  @Category(NeedsCdmUnitTest.class)
  public void testGempak() throws Exception {
    String file = TestDir.cdmUnitTestDir + "formats/gempak/surface/09052812.sf"; // Q:/cdmUnitTest/formats/gempak/surface/09052812.sf
    Formatter buf = new Formatter();
    FeatureDatasetPoint pods =
        (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.POINT, file, null, buf);
    if (pods == null) { // try as ANY_POINT
      pods = (FeatureDatasetPoint) FeatureDatasetFactoryManager.open(FeatureType.ANY_POINT, file, null, buf);
    }
    if (pods == null) {
      System.out.printf("can't open file=%s%n error=%s%n", file, buf);
      throw new Exception("can't open file " + file);
    }
    List<DsgFeatureCollection> collectionList = pods.getPointFeatureCollectionList();
    if (collectionList.size() > 1) {
      throw new IllegalArgumentException("Can't handle point data with multiple collections");
    }
    boolean sample;
    for (int time = 0; time < 2; time++) {
      sample = time < 1;
      DsgFeatureCollection fc = collectionList.get(0);
      PointFeatureCollection collection = null;
      LatLonRect llr =
          new LatLonRect.Builder(LatLonPoint.create(33.4, -92.2), LatLonPoint.create(47.9, -75.89)).build();
      System.out.println("llr = " + llr);
      if (fc instanceof PointFeatureCollection) {
        collection = (PointFeatureCollection) fc;
        collection = collection.subset(llr, null);

      } else if (fc instanceof StationTimeSeriesFeatureCollection) {
        StationTimeSeriesFeatureCollection npfc = (StationTimeSeriesFeatureCollection) fc;
        npfc = npfc.subset(llr);
        collection = npfc.flatten(llr, null);

      } else {
        throw new IllegalArgumentException("Can't handle collection of type " + fc.getClass().getName());
      }

      List<PointFeature> pos = new ArrayList<>(100000);
      List<CalendarDate> times = new ArrayList<>(100000);
      for (PointFeature po : collection) {
        pos.add(po);
        times.add(po.getNominalTimeAsCalendarDate());
        if (sample) {
          break;
        }
      }
      int size = pos.size();

      for (PointFeature po : pos) {
        ucar.unidata.geoloc.EarthLocation el = po.getLocation();
        System.out.println("el = " + el);
      }
    }
    pods.close();
  }

}
