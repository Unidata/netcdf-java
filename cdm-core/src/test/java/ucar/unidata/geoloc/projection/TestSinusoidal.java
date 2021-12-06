package ucar.unidata.geoloc.projection;

import org.junit.Test;
import ucar.unidata.geoloc.*;

import static com.google.common.truth.Truth.assertThat;

public class TestSinusoidal {
  private static final double TOLERANCE = 0.1;

  // If we want all of the x-coords in the geographic region to be positive, use this.
  // Will be roughly 20015.8.
  private static final double false_easting = new Sinusoidal().latLonToProj(0, 180).getX();

  // If we want all of the y-coords in the geographic region to be positive, use this.
  // Will be roughly 10007.9.
  private static final double false_northing = new Sinusoidal().latLonToProj(90, 0).getY();

  @Test
  public void projToLatLonBB_typical() {
    // All 4 corners of the bounding box are on the map.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Upper left: -2201 1111 -> 10.0N 20.1W
    // Upper right: 2982 111 -> 10.0N 27.23E
    // Lower left: -2201 -4446 -> 40.0S 25.84W
    // Lower right: 2982 -4446 -> 40.0S 35.0E
    ProjectionPoint upperLeft = ProjectionPoint.create(-2201, 1111);
    ProjectionPoint lowerRight = ProjectionPoint.create(2982, -4446);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-25.84);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(35.0);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(-40.0);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(10.0);
  }

  @Test // Reproduces issue from ETO-719860
  public void projToLatLonBB_validBottom() {
    // Bottom 2 corners of bounding box are on the map. Box intersects map edge at 2 places.
    //
    // These values come from the dataset referenced in ETO-719860.
    double minX = 7783.190324950472;
    double minY = 6672.166430716527;
    double maxX = 8895.140844616471;
    double maxY = 7784.116950383528;

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(minX, minY, maxX, maxY);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    // Values come from visual inspection in ToolsUI->Grid Viewer
    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(140.0);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(60);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(67.11);
  }

  @Test
  public void projToLatLonBB_validTop() {
    // Top 2 corners of bounding box are on the map. Box intersects map edge at 2 places.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Upper left: -9070 -2780 -> 25.002S 90.004W
    // Upper right: -3603 -2780 -> 25.002S 35.761W
    // Lower right: -3603 -8854 -> 79.627S 179.99W
    // But let's use false_easting = 20015.8 and false_northing = 10007.9
    ProjectionPoint upperLeft = ProjectionPoint.create(false_easting + -9070, false_northing + -2780);
    ProjectionPoint lowerRight = ProjectionPoint.create(false_easting + -3603, false_northing + -10000);

    Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, Earth.WGS84_EARTH_RADIUS_KM);
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(-35.761);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(-79.627);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(-25.002);
  }

  @Test
  public void projToLatLonBB_validLeft() {
    // Left 2 corners of bounding box are on the map. Box intersects map edge at 2 places.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Upper left: 14480 -2228 -> 20.037S 138.62E
    // Lower left: 14480 -4361 -> 39.224S 168.10E
    // Upper right: 18803 -2228 -> 20.037S 179.99E
    // But let's use false_easting = 20015.8 and false_northing = 10007.9
    ProjectionPoint upperLeft = ProjectionPoint.create(false_easting + 14480, false_northing + -2228);
    ProjectionPoint lowerRight = ProjectionPoint.create(false_easting + 20000, false_northing + -4361);

    Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, Earth.WGS84_EARTH_RADIUS_KM);
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(138.62);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180.0);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(-39.224);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(-20.037);
  }

  @Test
  public void projToLatLonBB_validRight() {
    // Right 2 corners of bounding box are on the map. Box intersects map edge at 2 places.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Lower right: -9370 4446 -> 39.985N 109.99W
    // Upper right: -9370 6278 -> 56.465N 152.54W
    // Lower left: -15334 4446 -> 39.985N 179.99W
    // But let's use false_easting = 20015.8 and false_northing = 10007.9
    ProjectionPoint lowerRight = ProjectionPoint.create(false_easting + -9370, false_northing + 4446);
    ProjectionPoint upperLeft = ProjectionPoint.create(false_easting + -17500, false_northing + 6278);

    Sinusoidal proj = new Sinusoidal(0, false_easting, false_northing, Earth.WGS84_EARTH_RADIUS_KM);
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(-109.99);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(39.985);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(56.465);
  }

  @Test
  public void projToLatLonBB_partiallyValidTop() {
    // The bottom corners are on the map, the top corners are not. However, the line formed by the top corners
    // intersects the map. As a result, the bounding box intersects the map edge at 4 places.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Lower left : -4166 8342 -> 75.0N 145.0W
    // Lower right: 4021 8342 -> 75.0N 140.0E
    // Upper 1st-from-left: -4166 8671 -> 77.985N 180.0W
    // Upper 2nd-from-left: -1744 9451 -> 85.0N 180.0W
    // Upper 3rd-from-left: 1743 9451 -> 85.0N 180.0E
    // Upper 4th-from-left: 4021 8718 -> 78.404N 180.0E
    ProjectionPoint lowerLeft = ProjectionPoint.create(-4166, 8342);
    ProjectionPoint upperRight = ProjectionPoint.create(4021, 9451);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(lowerLeft, upperRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(75);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(85);
  }

  @Test
  public void projToLatLonBB_onlyintersects() {
    // Same bounding box as projToLatLonBB_partiallyValidTop(), but the left and right sides have been
    // extended completely off the map. None of its corners are on the map, but it intersects the edge at 4 places.
    ProjectionPoint lowerLeft = ProjectionPoint.create(-13000, 8342);
    ProjectionPoint upperRight = ProjectionPoint.create(15000, 9451);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(lowerLeft, upperRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(75);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(85);
  }

  @Test
  public void projToLatLonBB_includesNorthPole() {
    // Same bouding box as projToLatLonBB_partiallyValidTop(), but the top was extended past 90°N.
    // It intersects the map edge at 2 places and includes the north pole.
    ProjectionPoint lowerLeft = ProjectionPoint.create(-4166, 8342);
    ProjectionPoint upperRight = ProjectionPoint.create(4021, 11111);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(lowerLeft, upperRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(75);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(90);
  }

  @Test
  public void projToLatLonBB_onlyintersectsAndPole() {
    // Same bounding box as projToLatLonBB_partiallyValidTop(), but the left, right, and top sides have been
    // extended completely off the map. None of its corners are on the map, but it intersects the edge at 2 places
    // and includes the north pole.
    ProjectionPoint lowerLeft = ProjectionPoint.create(-13000, 8342);
    ProjectionPoint upperRight = ProjectionPoint.create(15000, 11111);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(lowerLeft, upperRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(75);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(90);
  }

  @Test
  public void projToLatLonBB_poleAndOneCorner() {
    // The bounding box includes the south pole and only 1 corner is on the map.
    // It intersects the the map edge at 2 places.
    //
    // Values come from visual inspection in ToolsUI->Grid Viewer
    // Upper left: -1388 -6673 -> 60.0S 25.0W
    // Upper right: 10003 -6673 -> 60.0S 180.0E
    // Lower left: -1388 -9565 -> 86.0S 180.0W
    ProjectionPoint upperLeft = ProjectionPoint.create(-1388, -6673);
    ProjectionPoint lowerRight = ProjectionPoint.create(12000, -12000);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(-90);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(-60);
  }

  @Test
  public void projToLatLonBB_completelyOffTheMap() {
    // None of the corners are on the map and none of the sides intersect its edge.
    ProjectionPoint upperLeft = ProjectionPoint.create(10000, -7000);
    ProjectionPoint lowerRight = ProjectionPoint.create(13000, -10000);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(upperLeft, lowerRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB).isEqualTo(LatLonRect.INVALID);
  }

  @Test
  public void projToLatLonBB_everything() {
    // The bounding box includes the entire map.
    ProjectionPoint lowerLeft = ProjectionPoint.create(-30000, -30000);
    ProjectionPoint upperRight = ProjectionPoint.create(30000, 30000);

    Sinusoidal proj = new Sinusoidal();
    ProjectionRect projBB = new ProjectionRect(lowerLeft, upperRight);
    LatLonRect latLonBB = proj.projToLatLonBB(projBB);

    assertThat(latLonBB.getLonMin()).isWithin(TOLERANCE).of(-180);
    assertThat(latLonBB.getLonMax()).isWithin(TOLERANCE).of(180);
    assertThat(latLonBB.getLatMin()).isWithin(TOLERANCE).of(-90);
    assertThat(latLonBB.getLatMax()).isWithin(TOLERANCE).of(90);
  }
}
