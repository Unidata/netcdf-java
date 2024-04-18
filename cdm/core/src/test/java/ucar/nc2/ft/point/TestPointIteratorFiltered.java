package ucar.nc2.ft.point;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.Assert;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.ma2.ArrayObject;
import ucar.ma2.StructureData;
import ucar.nc2.ft.FeatureDatasetPoint;
import ucar.nc2.ft.NoFactoryFoundException;
import ucar.nc2.ft.PointFeature;
import ucar.nc2.ft.PointFeatureCollection;
import ucar.nc2.ft.PointFeatureIterator;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarDateRange;
import ucar.nc2.time.CalendarDateUnit;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;

public class TestPointIteratorFiltered {

  @Test
  public void shouldFilterSpaceAndTime() throws IOException, URISyntaxException, NoFactoryFoundException {
    try (FeatureDatasetPoint fdPoint = PointTestUtil.openPointDataset("pointsToFilter.ncml")) {

      double latMin = 10.0;
      double latMax = 50.0;
      double lonMin = -60.0;
      double lonMax = 10.0;
      LatLonRect filterBB = new LatLonRect(LatLonPoint.create(latMin, lonMin), LatLonPoint.create(latMax, lonMax));

      CalendarDateUnit calDateUnit = CalendarDateUnit.of("standard", "days since 1970-01-01 00:00:00");
      CalendarDate start = calDateUnit.makeCalendarDate(20);
      CalendarDate end = calDateUnit.makeCalendarDate(130);
      CalendarDateRange filterDate = CalendarDateRange.of(start, end);

      // filtered point iterator
      PointFeatureCollection flattenedDatasetCol = new FlattenedDatasetPointCollection(fdPoint);
      PointFeatureIterator pointIterOrig = flattenedDatasetCol.getPointFeatureIterator();
      try (PointFeatureIterator pointIterFiltered = new PointIteratorFiltered(pointIterOrig, filterBB, filterDate)) {
        assertThat(getIdsOfPoints(pointIterFiltered)).isEqualTo(Arrays.asList("BBB", "EEE"));

        // we call next() when there are no more elements
        NoSuchElementException e = Assert.assertThrows(NoSuchElementException.class, pointIterFiltered::next);
        assertThat(e.getMessage()).isEqualTo("This iterator has no more elements.");
      }
    }
  }

  private static List<String> getIdsOfPoints(PointFeatureIterator iter) throws IOException {
    List<String> ids = new ArrayList<>();
    while (iter.hasNext()) {
      iter.hasNext(); // Test idempotency. This call should have no effect.
      ids.add(getIdOfPoint(iter.next()));
    }
    return ids;
  }

  private static String getIdOfPoint(PointFeature pointFeat) throws IOException {
    StructureData data = pointFeat.getFeatureData();
    Array memberArray = data.getArray("id");
    assertThat(memberArray).isInstanceOf(ArrayObject.D0.class);

    ArrayObject.D0 memberArrayObject = (ArrayObject.D0) memberArray;
    return (String) memberArrayObject.get();
  }
}
