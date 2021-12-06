/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.unidata.geoloc;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/** Test {@link ProjectionRect} */
@RunWith(Parameterized.class)
public class TestProjectionRectP {

  private double x1, x2, y1, y2;
  private ProjectionRect projectionRect;

  @Parameterized.Parameters(name = "[{0}, {1}, {2}, {3}]")
  public static Collection projectionRectsInits() {
    Object[][] data = new Object[][] {{-1, -1, 1, 1}, {1, 1, -1, -1}, {-1, 1, 1, -1}, {1, -1, -1, 1}};
    return Arrays.asList(data);
  }

  public TestProjectionRectP(double x1, double y1, double x2, double y2) {
    this.x1 = x1;
    this.x2 = x2;
    this.y1 = y1;
    this.y2 = y2;
  }

  @Before
  public void setUp() {
    this.projectionRect = new ProjectionRect(x1, y1, x2, y2);
  }

  @Test
  public void testGetX() {
    // getX() should give the x value for the upper left corner
    double getX = projectionRect.getMinX();
    double getMinX = projectionRect.getMinX();
    double getMaxX = projectionRect.getMaxX();

    assertThat(getX).isEqualTo(getMinX);
    assertThat(getX).isNotEqualTo(getMaxX);
  }

  @Test
  public void testGetY() {
    // getX() should give the y value for the upper left corner
    double getY = projectionRect.getMinY();
    double getMinY = projectionRect.getMinY();
    double getMaxY = projectionRect.getMaxY();

    assertThat(getY).isNotEqualTo(getMaxY);
    assertThat(getY).isEqualTo(getMinY);
  }

  @Test
  public void testGetWidth1() {
    // getX() should give the y value for the upper left corner
    double minX = projectionRect.getMinX();
    double maxX = projectionRect.getMaxX();
    double testWidth = maxX - minX;
    double width = projectionRect.getWidth();
    assertThat(testWidth).isEqualTo(width);
  }

  @Test
  public void testGetHeight1() {
    // getX() should give the y value for the upper left corner
    double minY = projectionRect.getMinY();
    double maxY = projectionRect.getMaxY();
    double testHeight = maxY - minY;
    double height = projectionRect.getHeight();
    assertThat(testHeight).isEqualTo(height);
  }

  @Test
  public void testGetWidth2() {
    // getX() should give the y value for the upper left corner
    double minX = projectionRect.getMinX();
    double maxX = projectionRect.getMaxX();
    double testWidth = maxX - minX;
    double width = projectionRect.getWidth();
    assertThat(testWidth).isEqualTo(width);

  }

  @Test
  public void testGetHeight2() {
    // getX() should give the y value for the upper left corner
    double minY = projectionRect.getMinY();
    double maxY = projectionRect.getMaxY();
    double testHeight = maxY - minY;
    double height = projectionRect.getHeight();
    assertThat(testHeight).isEqualTo(height);
  }

  @Test
  public void testGetLowerLeftPoint() {
    ProjectionPoint getllp = projectionRect.getLowerLeftPoint();
    double llx = projectionRect.getMinX();
    double lly = projectionRect.getMinY();
    double urx = projectionRect.getMaxX();
    double ury = projectionRect.getMaxY();

    assertThat(llx).isEqualTo(getllp.getX());
    assertThat(lly).isEqualTo(getllp.getY());
    assertThat(urx).isNotEqualTo(getllp.getX());
    assertThat(ury).isNotEqualTo(getllp.getY());
  }

  @Test
  public void testGetUpperRightPoint() {
    ProjectionPoint geturp = projectionRect.getUpperRightPoint();

    double llx = projectionRect.getMinX();
    double lly = projectionRect.getMinY();
    double urx = projectionRect.getMaxX();
    double ury = projectionRect.getMaxY();

    assertThat(urx).isEqualTo(geturp.getX());
    assertThat(ury).isEqualTo(geturp.getY());
    assertThat(llx).isNotEqualTo(geturp.getX());
    assertThat(lly).isNotEqualTo(geturp.getY());
  }

  @Test
  public void testSetX() {
    double x = projectionRect.getMinX();
    double x2 = x * x + 1d;
    ProjectionRect test = projectionRect.toBuilder().setX(x2).build();

    assertThat(x2).isEqualTo(test.getMinX());
    assertThat(x).isNotEqualTo(x2);
  }

  @Test
  public void testSetY() {
    double y = projectionRect.getMinY();
    double y2 = y * y + 1d;
    ProjectionRect test = projectionRect.toBuilder().setY(y2).build();

    assertThat(y2).isEqualTo(test.getMinY());
    assertThat(y).isNotEqualTo(y2);
  }

  @Test
  public void testSetWidth() {
    double width = projectionRect.getWidth();
    double width2 = width + 10d;
    ProjectionRect test = projectionRect.toBuilder().setWidth(width2).build();

    assertThat(width2).isEqualTo(test.getWidth());
    assertThat(width).isNotEqualTo(width2);
  }

  @Test
  public void testSetHeight() {
    double height = projectionRect.getHeight();
    double height2 = height + 10d;
    ProjectionRect test = projectionRect.toBuilder().setHeight(height2).build();

    assertThat(height2).isEqualTo(test.getHeight());
    assertThat(height).isNotEqualTo(height2);
  }

  @Test
  public void testContainsPoint() {
    // contains the center point? -> YES
    assertThat(
        projectionRect.contains(ProjectionPoint.create(projectionRect.getCenterX(), projectionRect.getCenterY())));
    // contains a point outside the rectangle? -> NO
    assertThat(!projectionRect.contains(ProjectionPoint.create((projectionRect.getMinX() - projectionRect.getWidth()),
        (projectionRect.getMinY() - projectionRect.getHeight()))));
    assertThat(!projectionRect.contains(ProjectionPoint.create((projectionRect.getMaxX() + projectionRect.getWidth()),
        (projectionRect.getMaxY() + projectionRect.getHeight()))));
    // contains a point on the rectangle border -> YES
    assertThat(projectionRect.contains(projectionRect.getMinPoint()));
  }

  private ProjectionRect scaleShiftRect(double scaleFactor, double deltaX, double deltaY) {
    // quick and dirty method to scale and shift a rectangle, based on projectionRect
    double centerX = projectionRect.getCenterX();
    double centerY = projectionRect.getCenterY();
    double width = projectionRect.getWidth();
    double height = projectionRect.getHeight();

    double testMinX = (centerX + deltaX) - scaleFactor * (width / 2);
    double testMinY = (centerY + deltaY) - scaleFactor * (height / 2);

    return new ProjectionRect(ProjectionPoint.create(testMinX, testMinY), scaleFactor * width, scaleFactor * height);
  }

  @Test
  public void testContainsRect() {
    // contains a bigger rect? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(2.0, 0, 0)));
    // contains a smaller rect? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(0.5, 0, 0)));
    // contains the same rect? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(1.0, 0, 0)));

    // contains a bigger rect, offset by 0.1? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(2.0, 0.1, 0.1)));
    // contains a smaller rect, offset by 0.1? -> YES
    assertThat(projectionRect.contains(scaleShiftRect(0.5, 0.1, 0.1)));
    // contain the same rect, offset by 0.1? -> NO
    assertThat(!projectionRect.contains(scaleShiftRect(1.0, 0.1, 0.1)));
  }

}
