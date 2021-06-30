package ucar.nc2.internal.grid2;

import org.junit.Test;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;

import static com.google.common.truth.Truth.assertThat;

public class TestRemoteGridEncode {

  @Test
  public void testRuntimeLatest() {
    GridSubset subset = GridSubset.createNew();
    subset.setRunTimeLatest();
    // subset.setVariable("varname");

    String req = RemoteGridEncode.encodeDataRequest(subset, "varname");
    assertThat(req).isEqualTo("var=varname&runtime=latest");

    GridSubset copy = RemoteGridEncode.decodeDataRequest(req);
    assertThat(copy).isEqualTo(subset);
  }

  @Test
  public void testLatLonBoundingBox() {
    GridSubset subset = GridSubset.createNew();
    LatLonRect llbb1 = new LatLonRect(0.0, 170.0, 40.0, 300.0);
    subset.setLatLonBoundingBox(llbb1);

    String req = RemoteGridEncode.encodeDataRequest(subset, "varname");
    assertThat(req).isEqualTo("var=varname&north=40.0&south=0.0&west=170.0&east=300.0");

    GridSubset copy = RemoteGridEncode.decodeDataRequest(req);
    assertThat(copy).isEqualTo(subset);
  }

  @Test
  public void testLatLonPoint() {
    GridSubset subset = GridSubset.createNew();
    LatLonPoint llpt = LatLonPoint.create(99.0, .5);
    subset.setLatLonPoint(llpt);

    String req = RemoteGridEncode.encodeDataRequest(subset, "varname");
    assertThat(req).isEqualTo("var=varname&lat=90.0&lon=0.5");

    GridSubset copy = RemoteGridEncode.decodeDataRequest(req);
    assertThat(copy).isEqualTo(subset);
  }

  @Test
  public void testProjectionBoundingBox() {
    GridSubset subset = GridSubset.createNew();
    ProjectionRect rect1 = new ProjectionRect();
    subset.setProjectionBoundingBox(rect1);

    String req = RemoteGridEncode.encodeDataRequest(subset, "varname");
    assertThat(req).isEqualTo("var=varname&minx=-5000.0&miny=-5000.0&maxx=5000.0&maxy=5000.0");

    GridSubset copy = RemoteGridEncode.decodeDataRequest(req);
    assertThat(copy).isEqualTo(subset);
  }
}
