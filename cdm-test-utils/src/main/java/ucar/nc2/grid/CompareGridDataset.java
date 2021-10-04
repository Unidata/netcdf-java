/*
 * Copyright (c) 1998-2021 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.grid;

import ucar.nc2.Attribute;
import ucar.nc2.AttributeContainer;
import ucar.nc2.util.Misc;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static ucar.nc2.grid.GridAxisDependenceType.independent;
import static ucar.nc2.grid.GridAxisDependenceType.scalar;

public class CompareGridDataset {
  private static final float TOL = 1.0e-5f;

  private final GridDataset roundtrip;
  private final GridDataset expected;
  private final boolean gribIosp;

  public CompareGridDataset(GridDataset roundtrip, GridDataset expected, boolean gribIosp) {
    this.roundtrip = roundtrip;
    this.expected = expected;
    this.gribIosp = gribIosp;
  }

  public void compare() {
    System.out.printf(" GridDataset (%s) = %s%n%n", roundtrip.getClass().getName(), roundtrip.getLocation());
    System.out.printf("    expected (%s) = %s%n%n", expected.getClass().getName(), expected.getLocation());

    assertThat(roundtrip.getName()).startsWith(expected.getName());
    assertThat(roundtrip.getFeatureType()).isEqualTo(expected.getFeatureType());
    compareAttributes(roundtrip.attributes(), expected.attributes());

    assertThat(roundtrip.getGridAxes()).hasSize(expected.getGridAxes().size());
    for (GridAxis<?> axis : roundtrip.getGridAxes()) {
      GridAxis<?> expectedAxis = expected.getGridAxes().stream().filter(a -> a.getName().equals(axis.getName()))
          .findFirst().orElseThrow(() -> new RuntimeException("Cant find axis: " + axis.getName()));
      if (gribIosp && axis.getAxisType().isTime()) {
        continue;
      }
      compareGridAxis(axis, expectedAxis);
    }

    assertThat(roundtrip.getGridCoordinateSystems()).hasSize(expected.getGridCoordinateSystems().size());
    for (GridCoordinateSystem csys : roundtrip.getGridCoordinateSystems()) {
      GridCoordinateSystem expectedCsys =
          expected.getGridCoordinateSystems().stream().filter(cs -> cs.getName().equals(csys.getName())).findFirst()
              .orElseThrow(() -> new RuntimeException("Cant find csys: " + csys.getName()));

      compareGridCoordinateSystem(csys, expectedCsys);
    }

    assertThat(roundtrip.getGrids()).hasSize(expected.getGrids().size());
    for (Grid grid : roundtrip.getGrids()) {
      Grid expectedGrid = expected.getGrids().stream().filter(a -> a.getName().equals(grid.getName())).findFirst()
          .orElseThrow(() -> new RuntimeException("Cant find grid: " + grid.getName()));
      compareGrid(grid, expectedGrid);
    }
  }

  public boolean compareGridCoordinateSystem(GridCoordinateSystem roundtrip, GridCoordinateSystem expected) {
    boolean ok = true;

    assertThat(roundtrip.getName()).isEqualTo(expected.getName());
    assertThat(roundtrip.getFeatureType()).isEqualTo(expected.getFeatureType());
    assertThat(roundtrip.getVerticalTransform() == null).isEqualTo(expected.getVerticalTransform() == null);
    if (roundtrip.getVerticalTransform() != null) {
      assertThat(roundtrip.getVerticalTransform().getName()).isEqualTo(expected.getVerticalTransform().getName());
    }
    assertThat(roundtrip.getNominalShape()).isEqualTo(expected.getNominalShape());
    assertThat(roundtrip.isZPositive()).isEqualTo(expected.isZPositive());

    assertWithMessage(roundtrip.getName())
        .that(
            compareGridHorizCoordinateSystem(roundtrip.getHorizCoordinateSystem(), expected.getHorizCoordinateSystem()))
        .isTrue();
    assertWithMessage(roundtrip.getName())
        .that(compareGridTimeCoordinateSystem(roundtrip.getTimeCoordinateSystem(), expected.getTimeCoordinateSystem()))
        .isTrue();

    assertThat(roundtrip.getGridAxes()).hasSize(expected.getGridAxes().size());
    for (GridAxis<?> axis : roundtrip.getGridAxes()) {
      GridAxis<?> raxis =
          expected.getGridAxes().stream().filter(cs -> cs.getName().equals(axis.getName())).findFirst().orElse(null);
      assertThat((Object) raxis).isNotNull();
    }

    return ok;
  }

  public boolean compareGridHorizCoordinateSystem(GridHorizCoordinateSystem hsys, GridHorizCoordinateSystem expected) {
    compareGridAxis(hsys.getXHorizAxis(), expected.getXHorizAxis());
    compareGridAxis(hsys.getYHorizAxis(), expected.getYHorizAxis());
    assertThat(hsys.getProjection()).isEqualTo(expected.getProjection());
    assertThat(hsys.isLatLon()).isEqualTo(expected.isLatLon());
    assertThat(hsys.isCurvilinear()).isEqualTo(expected.isCurvilinear());
    assertThat(hsys.isGlobalLon()).isEqualTo(expected.isGlobalLon());
    assertThat(hsys.getShape()).isEqualTo(expected.getShape());
    assertThat(hsys.getGeoUnits()).isEqualTo(expected.getGeoUnits());
    assertThat(hsys.getLatLonBoundingBox().nearlyEquals(expected.getLatLonBoundingBox())).isTrue();
    assertThat(hsys.getBoundingBox().nearlyEquals(expected.getBoundingBox())).isTrue();
    return true;
  }

  public boolean compareGridTimeCoordinateSystem(GridTimeCoordinateSystem roundtrip,
      GridTimeCoordinateSystem expected) {
    assertThat(roundtrip == null).isEqualTo(expected == null);
    if (roundtrip == null) {
      return true;
    }
    assertThat(roundtrip.getType()).isEqualTo(expected.getType());
    assertThat(roundtrip.getRuntimeDateUnit()).isEqualTo(expected.getRuntimeDateUnit());
    assertThat(roundtrip.getBaseDate()).isEqualTo(expected.getBaseDate());
    assertWithMessage(roundtrip.toString()).that(roundtrip.getNominalShape()).isEqualTo(expected.getNominalShape());
    if (!gribIosp) {
      assertThat(roundtrip.getMaterializedShape()).isEqualTo(expected.getMaterializedShape());
    }
    compareGridAxis(roundtrip.getRunTimeAxis(), expected.getRunTimeAxis());

    assertThat(roundtrip.getRunTimeAxis() == null).isEqualTo(expected.getRunTimeAxis() == null);
    if (roundtrip.getRunTimeAxis() != null) {
      assertThat(roundtrip.getRunTimeAxis().getName()).isEqualTo(expected.getRunTimeAxis().getName());
      assertThat(roundtrip.getRunTimeAxis().getNominalSize()).isEqualTo(expected.getRunTimeAxis().getNominalSize());
      int nruns = roundtrip.getRunTimeAxis().getNominalSize();
      for (int i = 0; i < nruns; i++) {
        assertThat(roundtrip.getRuntimeDate(i)).isEqualTo(expected.getRuntimeDate(i));
        assertThat(compareGridAxis(roundtrip.getTimeOffsetAxis(i), expected.getTimeOffsetAxis(i))).isTrue();
      }
    } else {
      assertThat(roundtrip.getRuntimeDate(0)).isEqualTo(expected.getRuntimeDate(0));
      assertThat(compareGridAxis(roundtrip.getTimeOffsetAxis(0), expected.getTimeOffsetAxis(0))).isTrue();
    }

    assertThat(compareGridAxis(roundtrip.getTimeOffsetAxis(0), expected.getTimeOffsetAxis(0))).isTrue();
    return true;
  }

  public boolean compareGridAxis(GridAxis<?> roundtrip, GridAxis<?> expected) {
    assertThat(roundtrip == null).isEqualTo(expected == null);
    if (roundtrip == null) {
      return true;
    }

    assertThat(roundtrip.getName()).isEqualTo(expected.getName());
    assertWithMessage(roundtrip.getName() + " has description").that(roundtrip.getDescription())
        .isEqualTo(expected.getDescription());
    if (!this.gribIosp || !roundtrip.getAxisType().isTime()) {
      assertWithMessage(roundtrip.getName() + " has units").that(roundtrip.getUnits()).isEqualTo(expected.getUnits());
      assertThat(roundtrip.getAxisType()).isEqualTo(expected.getAxisType());
    }
    assertThat(roundtrip.getNominalSize()).isEqualTo(expected.getNominalSize());
    assertThat(roundtrip.getDependsOn()).isEqualTo(expected.getDependsOn());
    assertThat(roundtrip.isInterval()).isEqualTo(expected.isInterval());
    assertThat(roundtrip.isRegular()).isEqualTo(expected.isRegular());

    assertWithMessage(roundtrip.getName()).that(roundtrip.getSpacing()).isEqualTo(expected.getSpacing());
    if (roundtrip.getSpacing() != GridAxisSpacing.discontiguousInterval) {
      assertThat(roundtrip.getResolution()).isWithin(TOL).of(expected.getResolution());
    }

    if (roundtrip.getDependenceType() != scalar && expected.getDependenceType() != independent) {
      assertWithMessage(roundtrip.getName()).that(roundtrip.getDependenceType())
          .isEqualTo(expected.getDependenceType());
    }

    if (roundtrip.isInterval()) {
      GridAxisInterval intvLocal = (GridAxisInterval) roundtrip;
      GridAxisInterval intvRemote = (GridAxisInterval) expected;
      for (int i = 0; i < roundtrip.getNominalSize(); i++) {
        assertWithMessage(String.format("%s != %s", intvLocal.getCoordDouble(i), intvRemote.getCoordDouble(i)))
            .that(Misc.nearlyEquals(intvLocal.getCoordDouble(i), intvRemote.getCoordDouble(i), TOL)).isTrue();
        assertWithMessage(String.format("%s != %s", intvLocal.getCoordInterval(i), intvRemote.getCoordInterval(i)))
            .that(intvLocal.getCoordInterval(i).fuzzyEquals(intvRemote.getCoordInterval(i), TOL)).isTrue();
      }
    } else {
      GridAxisPoint intvLocal = (GridAxisPoint) roundtrip;
      GridAxisPoint intvRemote = (GridAxisPoint) expected;
      for (int i = 0; i < roundtrip.getNominalSize(); i++) {
        assertWithMessage(String.format("%s != %s", intvLocal.getCoordDouble(i), intvRemote.getCoordDouble(i)))
            .that(Misc.nearlyEquals(intvLocal.getCoordDouble(i), intvRemote.getCoordDouble(i), TOL)).isTrue();
        assertWithMessage(String.format("%s != %s", intvLocal.getCoordInterval(i), intvRemote.getCoordInterval(i)))
            .that(intvLocal.getCoordInterval(i).fuzzyEquals(intvRemote.getCoordInterval(i), TOL)).isTrue();
      }
    }

    compareAttributes(roundtrip.attributes(), expected.attributes());

    return true;
  }

  public boolean compareGrid(Grid grid, Grid expectedGrid) {
    if (grid.getClass() == expectedGrid.getClass()) {
      assertThat(grid).isEqualTo(expectedGrid);
    }
    assertThat(grid.getCoordinateSystem().getName()).isEqualTo(expectedGrid.getCoordinateSystem().getName());
    assertThat(grid.getName()).isEqualTo(expectedGrid.getName());
    assertThat(grid.getDescription()).isEqualTo(expectedGrid.getDescription());
    assertThat(grid.getUnits()).isEqualTo(expectedGrid.getUnits());
    assertThat(grid.getArrayType()).isEqualTo(expectedGrid.getArrayType());
    compareAttributes(grid.attributes(), expectedGrid.attributes());

    assertWithMessage(grid.getName())
        .that(compareGridHorizCoordinateSystem(grid.getHorizCoordinateSystem(), grid.getHorizCoordinateSystem()))
        .isTrue();
    assertWithMessage(grid.getName())
        .that(compareGridTimeCoordinateSystem(grid.getTimeCoordinateSystem(), grid.getTimeCoordinateSystem())).isTrue();

    return true;
  }

  // Just require that all expected attributes are present and equal
  public void compareAttributes(AttributeContainer atts, AttributeContainer expected) {
    for (Attribute att : expected) {
      Attribute expectedAtt = atts.findAttribute(att.getName());
      assertWithMessage(att.getName()).that(expectedAtt).isNotNull();
      assertThat(att).isEqualTo(expectedAtt);
    }
  }

}
