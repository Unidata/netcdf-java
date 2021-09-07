/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import ucar.array.Array;
import ucar.array.ArrayType;
import ucar.array.Arrays;
import ucar.array.Range;
import ucar.nc2.constants.AxisType;
import ucar.nc2.internal.grid.CurvilinearCoords;
import ucar.nc2.write.NcdumpArray;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.CurvilinearProjection;

import static com.google.common.truth.Truth.assertThat;

/** Unit test {@link GridHorizCurvilinear} */
public class TestGridHorizCurvilinear {
  private static final int nx = 20;
  private static final int ny = 22;

  private GridAxisPoint yaxis, xaxis;
  private Array<Number> lat2d;
  private Array<Number> lon2d;

  @Before
  public void setup() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.Lon).setName("xname")
        .setUnits("nominal").setDescription("desc").setRegular(nx, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.Lat).setName("yname")
        .setUnits("nominal").setDescription("desc").setRegular(ny, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    yaxis = ybuilder.build();
    make(10, 10, (y, x) -> 1.01 * (y + x), (y, x) -> 1.02 * (y + x));
  }

  interface Fn {
    double compute(int y, int x);
  }

  void make(int ny, int nx, Fn lat, Fn lon) {
    int[] shape = new int[] {ny, nx};
    double[] latdata = new double[ny * nx];
    double[] londata = new double[ny * nx];
    for (int y = 0; y < ny; y++) {
      for (int x = 0; x < nx; x++) {
        latdata[y * nx + x] = lat.compute(y, x);
        londata[y * nx + x] = lon.compute(y, x);
      }
    }
    lat2d = Arrays.factory(ArrayType.DOUBLE, shape, latdata);
    lon2d = Arrays.factory(ArrayType.DOUBLE, shape, londata);
  }

  @Test
  public void testEdges() {
    make(10, 10, (y, x) -> y, (y, x) -> x);
    System.out.printf("lat2d = %s%n", NcdumpArray.printArray(lat2d));
    System.out.printf("lon2d = %s%n", NcdumpArray.printArray(lon2d));
    GridHorizCurvilinear hcs = GridHorizCurvilinear.create(xaxis, yaxis, lat2d, lon2d);
    hcs.showEdges();
    assertThat(hcs.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-0.500000, -0.500000, 10.000000, 10.000000"));
  }

  @Test
  public void testEdges3() {
    make(10, 10, (y, x) -> 1.1 * y + 1.02 * x, (y, x) -> 1.2 * x + 1.01 * y);
    System.out.printf("%s%n", NcdumpArray.printArray(lat2d, "test lat2d", null));
    System.out.printf("%s%n", NcdumpArray.printArray(lon2d, "test lon2d", null));
    GridHorizCurvilinear hcs = GridHorizCurvilinear.create(xaxis, yaxis, lat2d, lon2d);
    hcs.showEdges();
    assertThat(hcs.getBoundingBox().nearlyEquals(ProjectionRect.fromSpec("-1.105000, -1.060000, 22.100000, 21.200000")))
        .isTrue();
  }

  @Test
  public void testCreate() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.Lon).setName("xname")
        .setUnits("nominal").setDescription("desc").setRegular(nx, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.Lat).setName("yname")
        .setUnits("nominal").setDescription("desc").setRegular(ny, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint yaxis = ybuilder.build();

    GridHorizCurvilinear hcs = GridHorizCurvilinear.create(xaxis, yaxis, lat2d, lon2d);

    assertThat((Object) hcs.getXHorizAxis()).isEqualTo(xaxis);
    assertThat((Object) hcs.getYHorizAxis()).isEqualTo(yaxis);
    assertThat(hcs.getProjection()).isInstanceOf(CurvilinearProjection.class);
    assertThat(hcs.isLatLon()).isTrue();
    assertThat(hcs.isGlobalLon()).isFalse();
    assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(ny, nx));
    assertThat(hcs.getYHorizAxis().getSubsetRange()).isEqualTo(new Range(ny));
    assertThat(hcs.getXHorizAxis().getSubsetRange()).isEqualTo(new Range(nx));

    assertThat(hcs.getGeoUnits()).isNull();
    assertThat(hcs.getBoundingBox().nearlyEquals(ProjectionRect.fromSpec("-1.020000, -1.010000, 20.400000, 20.200000")))
        .isTrue();
    assertThat(
        hcs.getLatLonBoundingBox().nearlyEquals(LatLonRect.fromSpec("-1.010000, -1.020000, 20.200000, 20.400000")))
            .isTrue();

    assertThat(hcs.findXYindexFromCoord(0, 0)).isEqualTo(hcs.findXYindexFromCoord(0, 0, null));
  }

  @Test
  public void testCreateFromEdges() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.Lon).setName("xname")
        .setUnits("nominal").setDescription("desc").setRegular(nx, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.Lat).setName("yname")
        .setUnits("nominal").setDescription("desc").setRegular(ny, 0.0, 1.0).setSpacing(GridAxisSpacing.regularPoint);
    GridAxisPoint yaxis = ybuilder.build();

    GridHorizCurvilinear hcs = GridHorizCurvilinear.createFromEdges(xaxis, yaxis, CurvilinearCoords.makeEdges(lat2d),
        CurvilinearCoords.makeEdges(lon2d));

    assertThat((Object) hcs.getXHorizAxis()).isEqualTo(xaxis);
    assertThat((Object) hcs.getYHorizAxis()).isEqualTo(yaxis);
    assertThat(hcs.getProjection()).isInstanceOf(CurvilinearProjection.class);
    assertThat(hcs.isLatLon()).isTrue();
    assertThat(hcs.isGlobalLon()).isFalse();
    assertThat(hcs.getShape()).isEqualTo(ImmutableList.of(ny, nx));
    assertThat(hcs.getYHorizAxis().getSubsetRange()).isEqualTo(new Range(ny));
    assertThat(hcs.getXHorizAxis().getSubsetRange()).isEqualTo(new Range(nx));

    assertThat(hcs.getGeoUnits()).isNull();
    assertThat(hcs.getBoundingBox().nearlyEquals(ProjectionRect.fromSpec("-1.020000, -1.010000, 20.400000, 20.200000")))
        .isTrue();
    assertThat(
        hcs.getLatLonBoundingBox().nearlyEquals(LatLonRect.fromSpec("-1.010000, -1.020000, 20.200000, 20.400000")))
            .isTrue();
  }

}
