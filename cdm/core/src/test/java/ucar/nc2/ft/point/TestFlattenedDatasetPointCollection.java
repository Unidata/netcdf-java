package ucar.nc2.ft.point;

import static org.mockito.Mockito.mock;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.ft.DsgFeatureCollection;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCC;
import ucar.nc2.ft.PointFeatureCCC;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.EarthLocation;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

public class TestFlattenedDatasetPointCollection {

  // FDP used in all feature methods. Its getPointFeatureCollectionList() method will be stubbed to return
  // different collections per test.
  FeatureDatasetPoint fdPoint = mock(FeatureDatasetPoint.class);

  PointFeature pf1, pf2, pf3, pf4, pf5, pf6, pf7, pf8, pf9;

  @Before
  public void setup() {
    // create point features
    CalendarDateUnit dateUnit = CalendarDateUnit.of(null, "days since 1970-01-01 00:00:00");
    DsgFeatureCollection dummyDsg = new SimplePointFeatureCollection("dummy", dateUnit, "m");

    pf1 = makePointFeat(dummyDsg, -75, -70, 630, 23, dateUnit);
    pf2 = makePointFeat(dummyDsg, -60, -40, 94, 51, dateUnit);
    pf3 = makePointFeat(dummyDsg, -45, -10, 1760, 88, dateUnit);
    pf4 = makePointFeat(dummyDsg, -85, 20, 18940, 120, dateUnit);
    pf5 = makePointFeat(dummyDsg, 0, 50, 26600, 150, dateUnit);
    pf6 = makePointFeat(dummyDsg, 85, 80, 52800, 180, dateUnit);
    pf7 = makePointFeat(dummyDsg, 15, 110, 1894, 200, dateUnit);
    pf8 = makePointFeat(dummyDsg, 30, 140, 266, 300, dateUnit);
    pf9 = makePointFeat(dummyDsg, 45, 170, 5280, 400, dateUnit);
  }

  private static PointFeature makePointFeat(DsgFeatureCollection dsg, double lat, double lon, double alt, double time,
      CalendarDateUnit dateUnit) {
    EarthLocation earthLoc = EarthLocation.create(lat, lon, alt);

    // Pass null StructureData; we only care about the metadata for these tests.
    return new SimplePointFeature(dsg, earthLoc, time, time, dateUnit, null);
  }

  @Test
  public void shouldHandleEmptyFeatureDatasetPoint() {
    when(fdPoint.getPointFeatureCollectionList()).thenReturn(Collections.emptyList());

    FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

    assertThat(flattenedDatasetCol.timeUnit.getUdUnit()).isEqualTo(CalendarDateUnit.unixDateUnit.getUdUnit());
    assertThat(flattenedDatasetCol.altUnits).isNull();

    PointFeatureIterator flattenedDatasetIter = flattenedDatasetCol.getPointFeatureIterator();

    assertThat(flattenedDatasetIter.hasNext()).isFalse();
    assertThat(flattenedDatasetIter.next()).isNull();
  }

  @Test
  public void shouldReturnMetadataFromFirstCollection() {
    CalendarDateUnit calDateUnitAlpha = CalendarDateUnit.of(null, "d since 1970-01-01 00:00:00");
    CalendarDateUnit calDateUnitBeta = CalendarDateUnit.of(null, "day since 1970-01-01 00:00:00");
    CalendarDateUnit dateUnitGamma = CalendarDateUnit.of(null, "days since 1970-01-01 00:00:00");

    PointFeatureCollection pointFeatColAlpha = new SimplePointFeatureCollection("Alpha", calDateUnitAlpha, "yard");
    PointFeatureCollection pointFeatColBeta = new SimplePointFeatureCollection("Beta", calDateUnitBeta, "mm");
    PointFeatureCollection pointFeatColGamma = new SimplePointFeatureCollection("Gamma", dateUnitGamma, "feet");

    when(fdPoint.getPointFeatureCollectionList())
        .thenReturn(Arrays.asList(pointFeatColAlpha, pointFeatColBeta, pointFeatColGamma));

    FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

    assertThat(flattenedDatasetCol.timeUnit).isEqualTo(pointFeatColAlpha.getTimeUnit());
    assertThat(flattenedDatasetCol.altUnits).isEqualTo(pointFeatColAlpha.getAltUnits());
  }

  @Test
  public void shouldCreateEmptyInstances() {
    // create an empty instance of each of the DsgFeatureCollection types
    PointFeatureCollection emptyC = new SimplePointFeatureCollection("emptyC", null, "m");
    PointFeatureCC emptyCC = new SimplePointFeatureCC("emptyCC", null, "y", FeatureType.POINT);
    PointFeatureCCC emptyCCC = new SimplePointFeatureCCC("emptyCCC", null, "in", FeatureType.POINT);

    // create a non-empty PointFeatureCC that contains an empty PointFeatureCollectio
    SimplePointFeatureCC nonEmptyCC = new SimplePointFeatureCC("nonEmptyCC", null, "y", FeatureType.POINT);
    nonEmptyCC.add(emptyC);

    // create a non-empty PointFeatureCCC that contains both an empty and non-empty PointFeatureCC
    SimplePointFeatureCCC nonEmptyCCC = new SimplePointFeatureCCC("nonEmptyCCC", null, "in", FeatureType.POINT);
    nonEmptyCCC.add(emptyCC);
    nonEmptyCCC.add(nonEmptyCC);

    when(fdPoint.getPointFeatureCollectionList())
        .thenReturn(Arrays.asList(emptyC, emptyCC, emptyCCC, nonEmptyCC, nonEmptyCCC));

    FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

    // collection contains no PointFeature
    PointFeatureIterator flattenedDatasetIter = flattenedDatasetCol.getPointFeatureIterator();
    assertThat(flattenedDatasetIter.hasNext()).isFalse();
  }

  @Test
  public void shouldCreateMultipleTypes() {
    SimplePointFeatureCollection pfc1 = new SimplePointFeatureCollection("pfc1", null, "m");
    pfc1.add(pf1);

    SimplePointFeatureCollection pfc2 = new SimplePointFeatureCollection("pfc2", null, "m");
    pfc2.add(pf2);
    pfc2.add(pf3);

    SimplePointFeatureCollection pfc3 = new SimplePointFeatureCollection("pfc3", null, "m");

    SimplePointFeatureCollection pfc4 = new SimplePointFeatureCollection("pfc4", null, "m");
    pfc4.add(pf4);

    SimplePointFeatureCollection pfc5 = new SimplePointFeatureCollection("pfc5", null, "m");
    pfc5.add(pf5);
    pfc5.add(pf6);
    pfc5.add(pf7);

    SimplePointFeatureCollection pfc6 = new SimplePointFeatureCollection("pfc6", null, "m");
    pfc6.add(pf8);

    SimplePointFeatureCollection pfc7 = new SimplePointFeatureCollection("pfc7", null, "m");
    pfc7.add(pf9);

    SimplePointFeatureCC pfcc1 = new SimplePointFeatureCC("pfcc1", null, "m", FeatureType.POINT);
    pfcc1.add(pfc1);
    pfcc1.add(pfc2);

    SimplePointFeatureCC pfcc2 = new SimplePointFeatureCC("pfcc2", null, "m", FeatureType.POINT);
    pfcc2.add(pfc3);
    pfcc2.add(pfc4);

    SimplePointFeatureCC pfcc3 = new SimplePointFeatureCC("pfcc3", null, "m", FeatureType.POINT);
    pfcc3.add(pfc6);
    pfcc3.add(pfc7);

    CalendarDateUnit dateUnit = CalendarDateUnit.of(null, "d since 1970-01-01 00:00:00");
    SimplePointFeatureCCC pfccc = new SimplePointFeatureCCC("pfccc", dateUnit, "m", FeatureType.POINT);
    pfccc.add(pfcc1);
    pfccc.add(pfcc2);

    // mock FeatureDatasetPoint to return 1 of each DsgFeatureCollection instance, then flatten it
    when(fdPoint.getPointFeatureCollectionList()).thenReturn(Arrays.asList(pfccc, pfc5, pfcc3));
    FlattenedDatasetPointCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);

    // before iterating over the collection, bounds are null
    assertThat(flattenedDatasetCol.getBoundingBox()).isNull();
    assertThat(flattenedDatasetCol.getCalendarDateRange()).isNull();

    // get the iterator and enable bounds calculation
    PointIteratorAbstract flattenedPointIter = (PointIteratorAbstract) flattenedDatasetCol.getPointFeatureIterator();
    flattenedPointIter.setCalculateBounds(flattenedDatasetCol.info);

    // iterate over the collection
    List<PointFeature> actualPointFeats = new ArrayList<>();
    flattenedPointIter.forEachRemaining(actualPointFeats::add);

    // the 9 PointFeatures are returned in order
    assertThat(actualPointFeats).isEqualTo(Arrays.asList(pf1, pf2, pf3, pf4, pf5, pf6, pf7, pf8, pf9));

    // the bounds include all 9 PointFeatures
    assertThat(flattenedDatasetCol.size()).isEqualTo(9);
    assertThat(flattenedDatasetCol.getBoundingBox())
        .isEqualTo(new LatLonRect(LatLonPoint.create(-85, -70), LatLonPoint.create(85, 170)));

    CalendarDateUnit calDateUnit = flattenedDatasetCol.timeUnit;
    assertThat(flattenedDatasetCol.getCalendarDateRange())
        .isEqualTo(CalendarDateRange.of(calDateUnit.makeCalendarDate(23), calDateUnit.makeCalendarDate(400)));
  }
}
