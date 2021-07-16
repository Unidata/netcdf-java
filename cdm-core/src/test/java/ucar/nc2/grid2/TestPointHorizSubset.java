/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid2;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ucar.array.Range;
import ucar.nc2.Attribute;
import ucar.nc2.constants.AxisType;
import ucar.nc2.grid.GridSubset;
import ucar.unidata.geoloc.Projection;
import ucar.unidata.geoloc.ProjectionRect;
import ucar.unidata.geoloc.projection.FlatEarth;

import java.util.Formatter;

import static com.google.common.truth.Truth.assertThat;

public class TestPointHorizSubset {

  @Test
  public void testRegularWithStride() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname")
        .setUnits("km").setDescription("desc").setRegular(7, 0.0, 11.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(GridSubset.create().setHorizStride(3), errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();
    assertThat(subset.getSubsetRanges()).isEqualTo(ImmutableList.of(Range.make(0, 6, 3), Range.make(0, 8, 3)));
    assertThat(subset.getShape()).isEqualTo(ImmutableList.of(3, 3));

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    assertThat(subset.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-15, -16.5, 90, 99"));
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    int count = 0;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 33);
      count++;
    }
    assertThat(count).isEqualTo(3);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 30);
      count++;
    }
    assertThat(count).isEqualTo(3);
  }

  @Test
  public void testIrregularWithStride() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(values).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(GridSubset.create().setHorizStride(2), errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();
    assertThat(hcs.getXHorizAxis().isRegular()).isTrue();
    assertThat(hcs.getYHorizAxis().isRegular()).isFalse();
    assertThat(subset.getSubsetRanges()).isEqualTo(ImmutableList.of(Range.make(0, 6, 2), Range.make(0, 8, 2)));
    assertThat(subset.getShape()).isEqualTo(ImmutableList.of(4, 5));

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    assertThat(subset.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-10, -5, 100, 135"));
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);

    int count = 0;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(values[count * 2]);
      count++;
    }
    assertThat(count).isEqualTo(4);

    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 20);
      count++;
    }
    assertThat(count).isEqualTo(5);
  }

  @Test
  public void testNominalWithStride() {
    int n = 6;
    double[] xvalues = new double[] {2, 4, 8, 15, 50, 80};
    double[] xedges = new double[] {0, 3, 5, 10, 20, 80, 100};
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("km").setDescription("desc").setNcoords(n).setValues(xvalues).setEdges(xedges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] yvalues = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(yvalues).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(GridSubset.create().setHorizStride(2), errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();
    assertThat(hcs.getXHorizAxis().isRegular()).isFalse();
    assertThat(hcs.getYHorizAxis().isRegular()).isFalse();
    assertThat(subset.getSubsetRanges()).isEqualTo(ImmutableList.of(Range.make(0, 6, 2), Range.make(0, 4, 2)));
    assertThat(subset.getShape()).isEqualTo(ImmutableList.of(4, 3));

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    // assertThat(subset.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-10, -5, 100, 135"));
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);

    int count = 0;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(yvalues[count * 2]);
      count++;
    }
    assertThat(count).isEqualTo(4);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(xvalues[count * 2]);
      count++;
    }
    assertThat(count).isEqualTo(3);

    for (int i = 0; i < xsubset.getNominalSize(); i++) {
      assertThat(xsubset.getCoordMidpoint(i)).isEqualTo(xvalues[2 * i]);
      assertThat(xsubset.getCoordInterval(i).start()).isEqualTo(xedges[2 * i]);
      assertThat(xsubset.getCoordInterval(i).end()).isEqualTo(xedges[2 * (i + 1)]);
    }
  }

  @Test
  public void testRegularWithRange() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(10, 0.0, 100.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname")
        .setUnits("km").setDescription("desc").setRegular(10, 0.0, 110.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(200, 300, 600, 800);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    // assertThat(subset.getBoundingBox()).isEqualTo(ProjectionRect.fromSpec("-15, -16.5, 90, 99"));
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(3, 7));

    int count = 3;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 110);
      count++;
    }
    assertThat(count).isEqualTo(8);


    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(2, 6));

    count = 2;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 100);
      count++;
    }
    assertThat(count).isEqualTo(7);
  }

  @Test
  public void testIrregularWithRange() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(values).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(20, 7, 66, 70);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(1, 5));

    int count = 1;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(values[count]);
      count++;
    }
    assertThat(count).isEqualTo(6);

    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(2, 7));

    count = 2;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(count * 10);
      count++;
    }
    assertThat(count).isEqualTo(8);
  }

  @Test
  public void testNominalWithRange() {
    int n = 6;
    double[] xvalues = new double[] {2, 4, 8, 15, 50, 80};
    double[] xedges = new double[] {0, 3, 5, 10, 20, 79, 100};
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("km").setDescription("desc").setNcoords(n).setValues(xvalues).setEdges(xedges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] yvalues = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(yvalues).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(4.1, 7, 80, 40);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();
    assertThat((Object) subset.getXHorizAxis()).isNotNull();
    assertThat((Object) subset.getYHorizAxis()).isNotNull();
    assertThat(subset.getProjection()).isEqualTo(project);
    assertThat(subset.isLatLon()).isFalse();
    assertThat(subset.isGlobalLon()).isFalse();
    assertThat(subset.getXHorizAxis().isRegular()).isFalse();
    assertThat(subset.getYHorizAxis().isRegular()).isFalse();

    assertThat(subset.getGeoUnits()).isEqualTo("km");
    assertThat(subset.getLatLonBoundingBox()).isNotNull();

    GridHorizCoordinateSystem copy =
        new GridHorizCoordinateSystem(hcs.getXHorizAxis(), hcs.getYHorizAxis(), hcs.getProjection());
    assertThat(copy).isEqualTo(hcs);
    assertThat(copy.hashCode()).isEqualTo(hcs.hashCode());

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(1, 4));

    int count = 1;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(yvalues[count]);
      count++;
    }
    assertThat(count).isEqualTo(5);

    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(1, 5));

    count = 1;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(xvalues[count]);
      count++;
    }
    assertThat(count).isEqualTo(6);

    for (int i = 0; i < xsubset.getNominalSize(); i++) {
      assertThat(xsubset.getCoordMidpoint(i)).isEqualTo(xvalues[i + 1]);
      assertThat(xsubset.getCoordInterval(i).start()).isEqualTo(xedges[i + 1]);
      assertThat(xsubset.getCoordInterval(i).end()).isEqualTo(xedges[i + 2]);
    }
  }

  @Test
  public void testRegularWithRangeAndStride() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(10, 0.0, 100.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    GridAxisPoint.Builder<?> ybuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname")
        .setUnits("km").setDescription("desc").setRegular(10, 0.0, 110.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(200, 300, 600, 800);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect).setHorizStride(3);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(3, 6, 3));
    assertThat(ysubset.getNominalSize()).isEqualTo(2);

    int count = 0;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo((3 + 3 * count) * 110);
      count++;
    }
    assertThat(count).isEqualTo(2);


    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(2, 5, 3));
    assertThat(xsubset.getNominalSize()).isEqualTo(2);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo((2 + 3 * count) * 100);
      count++;
    }
    assertThat(count).isEqualTo(2);
  }

  @Test
  public void testIrregularWithRangeAndStride() {
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("xname")
        .setUnits("km").setDescription("desc").setRegular(9, 0.0, 10.0).setSpacing(GridAxisSpacing.regularPoint)
        .addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] values = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(values).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(20, 7, 66, 70);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect).setHorizStride(2);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(1, 5, 2));
    assertThat(ysubset.getNominalSize()).isEqualTo(3);

    int count = 0;
    for (Number val : ysubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(values[1 + 2 * count]);
      count++;
    }
    assertThat(count).isEqualTo(3);

    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.regularPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(2, 6, 2));
    assertThat(xsubset.getNominalSize()).isEqualTo(3);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo((2 + 2 * count) * 10);
      count++;
    }
    assertThat(count).isEqualTo(3);
  }

  @Test
  public void testNominalWithRangeAndStride() {
    int n = 6;
    double[] xvalues = new double[] {2, 4, 8, 15, 50, 80};
    double[] xedges = new double[] {0, 3, 5, 10, 20, 79, 100};
    GridAxisPoint.Builder<?> xbuilder = GridAxisPoint.builder().setAxisType(AxisType.GeoX).setName("name")
        .setUnits("km").setDescription("desc").setNcoords(n).setValues(xvalues).setEdges(xedges)
        .setSpacing(GridAxisSpacing.nominalPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint xaxis = xbuilder.build();

    double[] yvalues = new double[] {0, 5, 10, 20, 40, 80, 100};
    GridAxisPoint.Builder<?> ybuilder =
        GridAxisPoint.builder().setAxisType(AxisType.GeoY).setName("yname").setUnits("km").setDescription("desc")
            .setValues(yvalues).setSpacing(GridAxisSpacing.irregularPoint).addAttribute(new Attribute("aname", 99.0));
    GridAxisPoint yaxis = ybuilder.build();

    Projection project = new FlatEarth();
    GridHorizCoordinateSystem hcs = new GridHorizCoordinateSystem(xaxis, yaxis, project);

    ProjectionRect rect = new ProjectionRect(4.1, 7, 80, 40);
    GridSubset params = GridSubset.create().setProjectionBoundingBox(rect).setHorizStride(2);
    Formatter errlog = new Formatter();
    GridHorizCoordinateSystem subset = hcs.subset(params, errlog).orElseThrow();

    GridAxisPoint ysubset = subset.getYHorizAxis();
    assertThat((Object) ysubset).isNotNull();
    assertThat(ysubset.getSpacing()).isEqualTo(GridAxisSpacing.irregularPoint);
    assertThat(ysubset.getSubsetRange()).isEqualTo(Range.make(1, 3, 2));
    assertThat(ysubset.getNominalSize()).isEqualTo(2);

    int count = 0;
    for (Number val : ysubset) {
      assertThat(val).isEqualTo(yvalues[1 + 2 * count]);
      count++;
    }
    assertThat(count).isEqualTo(2);

    GridAxisPoint xsubset = subset.getXHorizAxis();
    assertThat((Object) xsubset).isNotNull();
    assertThat(xsubset.getSpacing()).isEqualTo(GridAxisSpacing.nominalPoint);
    assertThat(xsubset.getSubsetRange()).isEqualTo(Range.make(1, 5, 2));
    assertThat(xsubset.getNominalSize()).isEqualTo(3);

    count = 0;
    for (Number val : xsubset) {
      System.out.printf(" %s%n", val);
      assertThat(val).isEqualTo(xvalues[1 + 2 * count]);
      count++;
    }
    assertThat(count).isEqualTo(3);

    for (int i = 0; i < xsubset.getNominalSize(); i++) {
      assertThat(xsubset.getCoordMidpoint(i)).isEqualTo(xvalues[1 + 2 * i]);
      assertThat(xsubset.getCoordInterval(i).start()).isEqualTo(xedges[1 + 2 * i]);
      int maxIdx = Math.min(1 + 2 * (i + 1), xedges.length - 1);
      assertThat(xsubset.getCoordInterval(i).end()).isEqualTo(xedges[maxIdx]);
    }
  }
}
