package ucar.nc2.grid;

import org.junit.Test;
import ucar.nc2.time.CalendarDate;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/** Test {@link GridSubset} */
public class TestGridSubset {

  @Test
  public void testHoriz() {
    GridSubset subset = new GridSubset();
    Integer strideo = subset.getHorizStride();
    assertThat(strideo).isNull();

    try {
      int stride = subset.getHorizStride();
      fail();
    } catch (NullPointerException e) {
      // correct
    }

    subset.setHorizStride(999);
    assertThat(subset.getHorizStride()).isEqualTo(999);
    int stride = subset.getHorizStride();
    assertThat(stride).isEqualTo(999);

    subset.setHorizStride((byte) 999);
    assertThat(subset.getHorizStride()).isEqualTo(-25);
  }

  @Test
  public void testEns() {
    GridSubset subset = new GridSubset();
    Double enso = subset.getEnsCoord();
    assertThat(enso).isNull();

    try {
      double ens = subset.getEnsCoord();
      fail();
    } catch (NullPointerException e) {
      // correct
    }

    subset.setEnsCoord(999.0);
    assertThat(subset.getEnsCoord()).isEqualTo(999);
    double ens = subset.getEnsCoord();
    assertThat(ens).isEqualTo(999);
  }

  @Test
  public void testLatLonBoundingBox() {
    GridSubset subset = new GridSubset();
    LatLonRect llbb = subset.getLatLonBoundingBox();
    assertThat(llbb).isNull();

    LatLonRect llbb1 = new LatLonRect();
    subset.setLatLonBoundingBox(llbb1);
    assertThat(subset.getLatLonBoundingBox()).isEqualTo(llbb1);
    assertThat(subset.getLatLonBoundingBox() == llbb1).isTrue();
  }

  @Test
  public void testLatLonPoint() {
    GridSubset subset = new GridSubset();
    LatLonPoint pt = subset.getLatLonPoint();
    assertThat(pt).isNull();

    LatLonPoint llpt = LatLonPoint.create(99.0, .5);
    subset.setLatLonPoint(llpt);
    assertThat(subset.getLatLonPoint()).isEqualTo(llpt);
    assertThat(subset.getLatLonPoint()).isEqualTo(LatLonPoint.create(99.0, .5));
    assertThat(subset.getLatLonPoint() == llpt).isTrue();
  }

  @Test
  public void testProjectionBoundingBox() {
    GridSubset subset = new GridSubset();
    ProjectionRect rect = subset.getProjectionBoundingBox();
    assertThat(rect).isNull();

    ProjectionRect rect1 = new ProjectionRect();
    subset.setProjectionBoundingBox(rect1);
    assertThat(subset.getProjectionBoundingBox()).isEqualTo(rect1);
    assertThat(subset.getProjectionBoundingBox() == rect1).isTrue();
  }

  @Test
  public void testRuntime() {
    GridSubset subset = new GridSubset();
    CalendarDate cd = subset.getRunTime();
    assertThat(cd).isNull();

    CalendarDate cd1 = CalendarDate.present();
    subset.setRunTime(cd1);
    assertThat(subset.getRunTime()).isEqualTo(cd1);
    assertThat(subset.getRunTime() == cd1).isTrue();
  }

  @Test
  public void testRuntimeLatest() {
    GridSubset subset = new GridSubset();
    Boolean latest = subset.getRunTimeLatest();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getRunTimeLatest();
    assertThat(late).isEqualTo(false);

    subset.setRunTimeLatest();
    assertThat(subset.getRunTimeLatest()).isEqualTo(true);
  }

  @Test
  public void testTime() {
    GridSubset subset = new GridSubset();
    CalendarDate cd = subset.getTime();
    assertThat(cd).isNull();

    CalendarDate cd1 = CalendarDate.present();
    subset.setTime(cd1);
    assertThat(subset.getTime()).isEqualTo(cd1);
    assertThat(subset.getTime() == cd1).isTrue();
  }

  @Test
  public void testTimeOffsetCoord() {
    GridSubset subset = new GridSubset();
    CoordInterval offsetv = CoordInterval.create(34.56, 78.9);
    subset.setTimeOffsetCoord(offsetv);
    assertThat(subset.getTimeOffsetIntv()).isEqualTo(offsetv);
    assertThat(subset.getTimeOffsetIntv() == offsetv).isTrue();

    subset.setTimeOffsetCoord(123.456);
    assertThat(subset.getTimeOffset()).isEqualTo(123.456);

    subset.setTimeOffsetCoord(999);
    assertThat(subset.getTimeOffset()).isEqualTo(999);

    try {
      subset.setTimeOffsetCoord(999);
    } catch (Exception e) {
      // correct
    }

    try {
      subset.setTimeOffsetCoord("999.9");
    } catch (Exception e) {
      // correct
    }
  }

  @Test
  public void testTimeOffsetFirst() {
    GridSubset subset = new GridSubset();
    Boolean latest = subset.getTimeOffsetFirst();
    assertThat(latest).isEqualTo(false);

    boolean late = subset.getTimeOffsetFirst();
    assertThat(late).isEqualTo(false);

    subset.setTimeOffsetFirst();
    assertThat(subset.getTimeOffsetFirst()).isEqualTo(true);
  }

  @Test
  public void testVertCoord() {
    GridSubset subset = new GridSubset();
    CoordInterval offsetv = CoordInterval.create(34.56, 78.9);
    subset.setVertCoord(offsetv);
    assertThat(subset.getVertIntv()).isEqualTo(offsetv);
    assertThat(subset.getVertIntv() == offsetv).isTrue();

    subset.setVertCoord(123.456);
    assertThat(subset.getVertPoint()).isEqualTo(123.456);
  }
}
