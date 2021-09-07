package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.unidata.geoloc.Earth;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.LatLonProjection;
import ucar.unidata.geoloc.projection.proj4.TransverseMercatorProjection;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

/** Test {@link GridHorizCoordinateSystem} */
public class TestGridHorizCoordinateSystem {

  @Test
  public void testLatLonIrregular() {
    int n = 7;
    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> builder = GridAxisPoint.builder().setAxisType(AxisType.Lat).setName("lat").setUnits("degN")
        .setDescription("desc").setNcoords(n).setValues(values).setSpacing(GridAxisSpacing.irregularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint latAxis = builder.build();

    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.Lon).setName("lon")
        .setUnits("degE").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint lonAxis = xbuilder.build();

    LatLonProjection project = new LatLonProjection(new Earth());
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(lonAxis, latAxis, project);

    assertThat((Object) hcs.getXHorizAxis()).isEqualTo(lonAxis);
    assertThat((Object) hcs.getYHorizAxis()).isEqualTo(latAxis);
    assertThat(hcs.getProjection()).isEqualTo(project);
    assertThat(hcs.isLatLon()).isTrue();
    assertThat(hcs.isGlobalLon()).isFalse();
    assertThat(hcs.getXHorizAxis().isRegular()).isTrue();
    assertThat(hcs.getYHorizAxis().isRegular()).isFalse();
    assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(7, 9));

    assertThat(hcs.getGeoUnits()).isNull();
    assertThat(hcs.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-5, -2.5, 90, 112.5"));
    assertThat(hcs.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    LatLonPoint llpt = hcs.getLatLon(3, 3);
    assertThat(llpt).isEqualTo(LatLonPoint.create(20, 30));
    GridHorizCoordinateSystem.CoordReturn cr = hcs.findXYindexFromCoord(30, 20).orElseThrow();
    assertThat(cr).isEqualTo(new GridHorizCoordinateSystem.CoordReturn(30, 20, 3, 3));
    assertThat(cr.hashCode()).isEqualTo(new GridHorizCoordinateSystem.CoordReturn(30, 20, 3, 3).hashCode());
    assertThat(hcs.findXYindexFromCoord(-30, 20)).isEmpty();
    assertThat(hcs.findXYindexFromCoord(-30, 20, null)).isEmpty();
    assertThat(cr).isEqualTo(hcs.findXYindexFromCoord(30, 20, new int[] {0, 7}).orElseThrow());
    System.out.printf("%s%n", cr);
    System.out.printf("%s%n", cr.toStringShort());

    int count = 0;
    int nx = lonAxis.getNominalSize();
    for (GridHorizCoordinateSystem.CellBounds cell : hcs.cells()) {
      int yindex = count / nx;
      int xindex = count % nx;
      CoordInterval xintv = lonAxis.getCoordInterval(xindex);
      CoordInterval yintv = latAxis.getCoordInterval(yindex);
      assertThat(cell.ll)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.start(), yintv.start(), xindex, yindex));
      assertThat(cell.ur)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.end(), yintv.end(), xindex, yindex));
      assertThat(cell.lr)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.end(), yintv.start(), xindex, yindex));
      assertThat(cell.ul)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.start(), yintv.end(), xindex, yindex));
      count++;
      if (count == 0) {
        System.out.printf("%s%n", cell);
      }
    }
  }

  @Test
  public void testProjectionRegular() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname")
        .setUnits("km").setDescription("desc").setRegular(7, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    TransverseMercatorProjection project = new TransverseMercatorProjection(new Earth(), 0, 0, 0.9996, 0, 0);
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    assertThat((Object) hcs.getXHorizAxis()).isEqualTo(xaxis);
    assertThat((Object) hcs.getYHorizAxis()).isEqualTo(yaxis);
    assertThat(hcs.getProjection()).isEqualTo(project);
    assertThat(hcs.isLatLon()).isFalse();
    assertThat(hcs.isGlobalLon()).isFalse();
    assertThat(hcs.getXHorizAxis().isRegular()).isTrue();
    assertThat(hcs.getYHorizAxis().isRegular()).isTrue();
    assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(7, 9));

    assertThat(hcs.getGeoUnits()).isEqualTo("km");
    assertThat(hcs.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-5, -5, 90, 70"));
    assertThat(hcs.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    LatLonPoint llpt = hcs.getLatLon(3, 4);
    ProjectionPoint ppt = ProjectionPoint.create(30, 40);
    assertThat(llpt).isEqualTo(hcs.getProjection().projToLatLon(ppt));
    GridHorizCoordinateSystem.CoordReturn cr = hcs.findXYindexFromCoord(ppt.getX(), ppt.getY()).orElseThrow();
    assertThat(cr).isEqualTo(new GridHorizCoordinateSystem.CoordReturn(ppt.getX(), ppt.getY(), 3, 4));
    assertThat(hcs.findXYindexFromCoord(-30, 20)).isEmpty();

    int count = 0;
    int nx = xaxis.getNominalSize();
    for (GridHorizCoordinateSystem.CellBounds cell : hcs.cells()) {
      int yindex = count / nx;
      int xindex = count % nx;
      CoordInterval xintv = xaxis.getCoordInterval(xindex);
      CoordInterval yintv = yaxis.getCoordInterval(yindex);
      assertThat(cell.ll)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.start(), yintv.start(), xindex, yindex));
      assertThat(cell.ur)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.end(), yintv.end(), xindex, yindex));
      assertThat(cell.lr)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.end(), yintv.start(), xindex, yindex));
      assertThat(cell.ul)
          .isEqualTo(new GridHorizCoordinateSystem.CoordReturn(xintv.start(), yintv.end(), xindex, yindex));
      if (count == 0) {
        System.out.printf("%s%n", cell);
      }
      count++;
    }
  }


}
